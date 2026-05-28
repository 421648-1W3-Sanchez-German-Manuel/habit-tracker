package com.tp1.habittracker.controller;

import com.tp1.habittracker.dto.friendship.FriendshipOverviewResponse;
import com.tp1.habittracker.dto.friendship.SendFriendRequestRequest;
import com.tp1.habittracker.dto.friendship.UserSummaryResponse;
import com.tp1.habittracker.service.FriendshipService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @GetMapping
    public List<UserSummaryResponse> listFriends(Authentication authentication) {
        return friendshipService.listFriends(extractUserId(authentication));
    }

    @GetMapping("/overview")
    public FriendshipOverviewResponse overview(Authentication authentication) {
        return friendshipService.getOverview(extractUserId(authentication));
    }

    @GetMapping("/requests/incoming")
    public List<UserSummaryResponse> incoming(Authentication authentication) {
        return friendshipService.listIncomingRequests(extractUserId(authentication));
    }

    @GetMapping("/requests/outgoing")
    public List<UserSummaryResponse> outgoing(Authentication authentication) {
        return friendshipService.listOutgoingRequests(extractUserId(authentication));
    }

    @PostMapping("/requests")
    public ResponseEntity<UserSummaryResponse> sendRequest(
            Authentication authentication,
            @Valid @RequestBody SendFriendRequestRequest request
    ) {
        UserSummaryResponse target = friendshipService.sendRequest(extractUserId(authentication), request.targetUsername());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(target);
    }

    @PostMapping("/requests/{requesterId}/accept")
    public ResponseEntity<Void> acceptRequest(
            Authentication authentication,
            @PathVariable("requesterId") String requesterId
    ) {
        friendshipService.acceptRequest(extractUserId(authentication), requesterId);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/requests/{requesterId}/decline")
    public ResponseEntity<Void> declineRequest(
            Authentication authentication,
            @PathVariable("requesterId") String requesterId
    ) {
        friendshipService.declineRequest(extractUserId(authentication), requesterId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/requests/outgoing/{targetId}")
    public ResponseEntity<Void> cancelOutgoing(
            Authentication authentication,
            @PathVariable("targetId") String targetId
    ) {
        friendshipService.cancelOutgoingRequest(extractUserId(authentication), targetId);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> unfriend(
            Authentication authentication,
            @PathVariable("userId") String userId
    ) {
        friendshipService.removeFriendship(extractUserId(authentication), userId);
        return ResponseEntity.accepted().build();
    }

    private String extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }
}
