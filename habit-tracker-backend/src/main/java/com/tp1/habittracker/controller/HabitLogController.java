package com.tp1.habittracker.controller;

import com.tp1.habittracker.domain.model.HabitLog;
import com.tp1.habittracker.dto.log.CreateHabitLogRequest;
import com.tp1.habittracker.dto.log.HabitLogResponse;
import com.tp1.habittracker.service.HabitLogService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class HabitLogController {

    private final HabitLogService habitLogService;

    @PostMapping
    public ResponseEntity<HabitLogResponse> addHabitLog(
            Authentication authentication,
            @Valid @RequestBody CreateHabitLogRequest request
    ) {
        HabitLog created = habitLogService.addHabitLog(extractAuthenticatedUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @GetMapping("/habit/{habitId}")
    public List<HabitLogResponse> getLogsByHabit(
            Authentication authentication,
            @PathVariable String habitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        String authenticatedUserId = extractAuthenticatedUserId(authentication);
        List<HabitLog> logs = (from != null && to != null)
                ? habitLogService.getLogsByHabitAndDateRange(authenticatedUserId, habitId, from, to)
                : habitLogService.getLogsByHabit(authenticatedUserId, habitId);

        return logs.stream().map(this::toResponse).toList();
    }

    @DeleteMapping("/habit/{habitId}")
    public ResponseEntity<Void> deleteLogsByHabitAndDateRange(
            Authentication authentication,
            @PathVariable String habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        habitLogService.removeLogsByHabitAndDateRange(extractAuthenticatedUserId(authentication), habitId, from, to);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{logId}")
    public ResponseEntity<Void> deleteLogById(
            Authentication authentication,
            @PathVariable String logId
    ) {
        habitLogService.removeLogById(extractAuthenticatedUserId(authentication), logId);
        return ResponseEntity.noContent().build();
    }

    private String extractAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalArgumentException("Authenticated user is required");
        }
        return authentication.getName();
    }

    private HabitLogResponse toResponse(HabitLog log) {
        return new HabitLogResponse(
                log.getId(),
                log.getHabitId(),
                log.getDate(),
                log.getValue()
        );
    }
}
