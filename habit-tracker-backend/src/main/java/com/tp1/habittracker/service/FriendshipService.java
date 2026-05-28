package com.tp1.habittracker.service;

import com.tp1.habittracker.domain.model.GraphEventType;
import com.tp1.habittracker.domain.model.OutboxEvent;
import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.dto.friendship.FriendshipOverviewResponse;
import com.tp1.habittracker.dto.friendship.UserSummaryResponse;
import com.tp1.habittracker.exception.DuplicateResourceException;
import com.tp1.habittracker.exception.ResourceNotFoundException;
import com.tp1.habittracker.repository.OutboxEventRepository;
import com.tp1.habittracker.repository.UserRepository;
import com.tp1.habittracker.repository.graph.UserGraphRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Friendship write-and-read service. Reads are served directly from Neo4j (the source
 * of truth for the social graph). Writes validate against current graph state and
 * then emit an outbox event that {@link com.tp1.habittracker.service.graph.GraphSyncService}
 * projects into Neo4j on its next tick.
 *
 * Eventual-consistency note: after a write returns, the graph reflects the new state
 * within {@code app.graph.sync.poll-ms} (default 2s). Clients should expect a brief
 * window where a just-sent request is not yet visible in list endpoints.
 */
@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final UserRepository userRepository;
    private final UserGraphRepository userGraphRepository;
    private final OutboxEventRepository outboxEventRepository;

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    public FriendshipOverviewResponse getOverview(String authenticatedUserId) {
        String userId = requireUserId(authenticatedUserId);
        requireUserExists(userId);

        List<UserSummaryResponse> friends = hydrate(userGraphRepository.findFriendIds(userId));
        List<UserSummaryResponse> incoming = hydrate(userGraphRepository.findIncomingRequestIds(userId));
        List<UserSummaryResponse> outgoing = hydrate(userGraphRepository.findOutgoingRequestIds(userId));

        return new FriendshipOverviewResponse(friends, incoming, outgoing);
    }

    public List<UserSummaryResponse> listFriends(String authenticatedUserId) {
        String userId = requireUserId(authenticatedUserId);
        requireUserExists(userId);
        return hydrate(userGraphRepository.findFriendIds(userId));
    }

    public List<UserSummaryResponse> listIncomingRequests(String authenticatedUserId) {
        String userId = requireUserId(authenticatedUserId);
        requireUserExists(userId);
        return hydrate(userGraphRepository.findIncomingRequestIds(userId));
    }

    public List<UserSummaryResponse> listOutgoingRequests(String authenticatedUserId) {
        String userId = requireUserId(authenticatedUserId);
        requireUserExists(userId);
        return hydrate(userGraphRepository.findOutgoingRequestIds(userId));
    }

    // ---------------------------------------------------------------------
    // Writes — validate against current graph, then enqueue outbox event
    // ---------------------------------------------------------------------

    @Transactional
    public UserSummaryResponse sendRequest(String authenticatedUserId, String targetUsername) {
        String requesterId = requireUserId(authenticatedUserId);
        String normalizedTargetUsername = Objects.requireNonNull(targetUsername, "targetUsername must not be null").trim();

        if (normalizedTargetUsername.isEmpty()) {
            throw new IllegalArgumentException("Target username is required");
        }

        User target = userRepository.findByUsernameIgnoreCase(normalizedTargetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + normalizedTargetUsername));

        String targetId = target.getId().toString();

        if (targetId.equals(requesterId)) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself");
        }

        Set<String> friendIds = new HashSet<>(userGraphRepository.findFriendIds(requesterId));
        if (friendIds.contains(targetId)) {
            throw new DuplicateResourceException("You are already friends with this user");
        }

        Set<String> outgoing = new HashSet<>(userGraphRepository.findOutgoingRequestIds(requesterId));
        if (outgoing.contains(targetId)) {
            throw new DuplicateResourceException("You already have a pending request to this user");
        }

        Set<String> incoming = new HashSet<>(userGraphRepository.findIncomingRequestIds(requesterId));
        if (incoming.contains(targetId)) {
            throw new DuplicateResourceException(
                    "This user has already sent you a friend request — accept it instead",
                    Map.of("incomingRequesterId", targetId, "incomingRequesterUsername", target.getUsername())
            );
        }

        publish(GraphEventType.FRIENDSHIP_REQUEST_CREATED, requesterId, targetId, null);
        return toSummary(target);
    }

    @Transactional
    public void acceptRequest(String authenticatedUserId, String requesterId) {
        String accepterId = requireUserId(authenticatedUserId);
        String normalizedRequesterId = requireUserId(requesterId);

        if (normalizedRequesterId.equals(accepterId)) {
            throw new IllegalArgumentException("Invalid request");
        }

        requireUserExists(normalizedRequesterId);

        Set<String> incoming = new HashSet<>(userGraphRepository.findIncomingRequestIds(accepterId));
        if (!incoming.contains(normalizedRequesterId)) {
            throw new ResourceNotFoundException("No pending friend request from this user");
        }

        publish(GraphEventType.FRIENDSHIP_REQUEST_ACCEPTED, normalizedRequesterId, accepterId, null);
    }

    @Transactional
    public void declineRequest(String authenticatedUserId, String requesterId) {
        String declinerId = requireUserId(authenticatedUserId);
        String normalizedRequesterId = requireUserId(requesterId);

        Set<String> incoming = new HashSet<>(userGraphRepository.findIncomingRequestIds(declinerId));
        if (!incoming.contains(normalizedRequesterId)) {
            throw new ResourceNotFoundException("No pending friend request from this user");
        }

        publish(GraphEventType.FRIENDSHIP_REQUEST_DECLINED, normalizedRequesterId, declinerId, null);
    }

    @Transactional
    public void cancelOutgoingRequest(String authenticatedUserId, String targetId) {
        String requesterId = requireUserId(authenticatedUserId);
        String normalizedTargetId = requireUserId(targetId);

        Set<String> outgoing = new HashSet<>(userGraphRepository.findOutgoingRequestIds(requesterId));
        if (!outgoing.contains(normalizedTargetId)) {
            throw new ResourceNotFoundException("No outgoing friend request to this user");
        }

        publish(GraphEventType.FRIENDSHIP_REQUEST_CANCELLED, requesterId, normalizedTargetId, null);
    }

    @Transactional
    public void removeFriendship(String authenticatedUserId, String otherUserId) {
        String userId = requireUserId(authenticatedUserId);
        String normalizedOther = requireUserId(otherUserId);

        Set<String> friendIds = new HashSet<>(userGraphRepository.findFriendIds(userId));
        if (!friendIds.contains(normalizedOther)) {
            throw new ResourceNotFoundException("You are not friends with this user");
        }

        publish(GraphEventType.FRIENDSHIP_REMOVED, userId, normalizedOther, null);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void publish(GraphEventType type, String aggregateId, String relatedId, String payload) {
        outboxEventRepository.save(OutboxEvent.builder()
                .eventType(type)
                .aggregateId(aggregateId)
                .relatedId(relatedId)
                .payload(payload)
                .build());
    }

    private List<UserSummaryResponse> hydrate(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<UUID> uuids = ids.stream()
                .map(this::parseUuidOrNull)
                .filter(Objects::nonNull)
                .toList();

        Map<UUID, User> byId = userRepository.findAllById(uuids).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return uuids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(this::toSummary)
                .toList();
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(user.getId().toString(), user.getUsername());
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        return userId.trim().toLowerCase(Locale.ROOT);
    }

    private void requireUserExists(String userId) {
        UUID parsed;
        try {
            parsed = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        if (!userRepository.existsById(parsed)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
