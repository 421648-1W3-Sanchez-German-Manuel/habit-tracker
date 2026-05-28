package com.tp1.habittracker.dto.friendship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendFriendRequestRequest(
        @NotBlank
        @Size(max = 64)
        String targetUsername
) {
}
