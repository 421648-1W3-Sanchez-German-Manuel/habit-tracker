package com.tp1.habittracker.service.graph;

import com.tp1.habittracker.domain.model.GraphEventType;
import com.tp1.habittracker.domain.model.OutboxEvent;
import com.tp1.habittracker.repository.OutboxEventRepository;
import com.tp1.habittracker.repository.graph.HabitGraphRepository;
import com.tp1.habittracker.repository.graph.UserGraphRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-event transactional boundary for the outbox processor. Lives in its own bean
 * so that cross-bean calls from {@link GraphSyncService} go through the Spring
 * proxy and {@link Transactional} actually applies.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventDispatcher {

    private final OutboxEventRepository outboxRepository;
    private final UserGraphRepository userGraphRepository;
    private final HabitGraphRepository habitGraphRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void applyAndMarkProcessed(OutboxEvent event) {
        dispatch(event);
        event.setProcessedAt(Instant.now());
        event.setLastError(null);
        outboxRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(OutboxEvent event, String errorMessage) {
        event.setAttempts(event.getAttempts() + 1);
        event.setLastError(truncate(errorMessage, 1900));
        outboxRepository.save(event);
    }

    private void dispatch(OutboxEvent event) {
        GraphEventType type = event.getEventType();
        switch (type) {
            case USER_CREATED -> userGraphRepository.ensureUserNode(event.getAggregateId());
            case USER_DELETED -> userGraphRepository.deleteUserNode(event.getAggregateId());

            case HABIT_CREATED -> {
                habitGraphRepository.upsertHabitNode(event.getAggregateId(), event.getPayload());
                userGraphRepository.ensureUserNode(event.getRelatedId());
                userGraphRepository.linkHabit(event.getRelatedId(), event.getAggregateId());
            }
            case HABIT_UPDATED -> habitGraphRepository.upsertHabitNode(event.getAggregateId(), event.getPayload());
            case HABIT_DELETED -> {
                if (event.getRelatedId() != null) {
                    userGraphRepository.unlinkHabit(event.getRelatedId(), event.getAggregateId());
                }
                habitGraphRepository.deleteHabitNode(event.getAggregateId());
            }

            case FRIENDSHIP_REQUEST_CREATED -> {
                userGraphRepository.ensureUserNode(event.getAggregateId());
                userGraphRepository.ensureUserNode(event.getRelatedId());
                userGraphRepository.createFriendRequest(event.getAggregateId(), event.getRelatedId());
            }
            case FRIENDSHIP_REQUEST_ACCEPTED ->
                    userGraphRepository.acceptFriendRequest(event.getAggregateId(), event.getRelatedId());
            case FRIENDSHIP_REQUEST_DECLINED, FRIENDSHIP_REQUEST_CANCELLED ->
                    userGraphRepository.deleteFriendRequest(event.getAggregateId(), event.getRelatedId());
            case FRIENDSHIP_REMOVED ->
                    userGraphRepository.removeFriendship(event.getAggregateId(), event.getRelatedId());
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
