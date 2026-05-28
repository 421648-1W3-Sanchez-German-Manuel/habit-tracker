package com.tp1.habittracker.dto.friendship;

import java.util.List;

public record FriendshipOverviewResponse(
        List<UserSummaryResponse> friends,
        List<UserSummaryResponse> incomingRequests,
        List<UserSummaryResponse> outgoingRequests
) {
}
