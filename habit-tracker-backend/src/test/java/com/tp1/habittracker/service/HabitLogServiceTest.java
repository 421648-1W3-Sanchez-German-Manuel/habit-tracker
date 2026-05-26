package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.tp1.habittracker.domain.enums.HabitType;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.domain.model.HabitLog;
import com.tp1.habittracker.dto.log.CreateHabitLogRequest;
import com.tp1.habittracker.exception.DuplicateResourceException;
import com.tp1.habittracker.exception.ResourceNotFoundException;
import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HabitLogServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private HabitRepository habitRepository;

    private HabitLogService habitLogService;

    @BeforeEach
    void setUp() {
        habitLogService = new HabitLogService(habitLogRepository, habitRepository);
    }

    @Test
    void addHabitLogThrowsWhenLogAlreadyExistsForHabitAndDate() {
        String userId = "user-1";
        String habitId = "habit-1";
        LocalDate date = LocalDate.now();

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .type(HabitType.BOOLEAN)
                .build();

        CreateHabitLogRequest request = new CreateHabitLogRequest(
                habitId,
                date,
                JsonNodeFactory.instance.booleanNode(true)
        );

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(habitLogRepository.existsByHabitIdAndDate(habitId, date)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(
                DuplicateResourceException.class,
                () -> habitLogService.addHabitLog(userId, request)
        );

        assertTrue(ex.getMessage().contains("already completed"));
        verify(habitLogRepository, never()).save(any(HabitLog.class));
    }

    @Test
    void addHabitLogSavesWhenNoDuplicateAndValueMatchesHabitType() {
        String userId = "user-1";
        String habitId = "habit-2";
        LocalDate date = LocalDate.now();
        JsonNode value = JsonNodeFactory.instance.booleanNode(true);

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .type(HabitType.BOOLEAN)
                .build();

        CreateHabitLogRequest request = new CreateHabitLogRequest(habitId, date, value);

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(habitLogRepository.existsByHabitIdAndDate(habitId, date)).thenReturn(false);
        when(habitLogRepository.save(any(HabitLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        HabitLog saved = habitLogService.addHabitLog(userId, request);

        assertEquals(habitId, saved.getHabitId());
        assertEquals(date, saved.getDate());
        assertEquals(Boolean.TRUE, saved.getValue());
        verify(habitLogRepository).save(any(HabitLog.class));
    }

    @Test
    void addHabitLogThrowsWhenValueTypeDoesNotMatchHabitType() {
        String userId = "user-1";
        String habitId = "habit-3";
        LocalDate date = LocalDate.now();

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .type(HabitType.BOOLEAN)
                .build();

        CreateHabitLogRequest request = new CreateHabitLogRequest(
                habitId,
                date,
                JsonNodeFactory.instance.numberNode(1)
        );

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));
        when(habitLogRepository.existsByHabitIdAndDate(habitId, date)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> habitLogService.addHabitLog(userId, request));
        verify(habitLogRepository, never()).save(any(HabitLog.class));
    }

    @Test
    void removeLogsByHabitAndDateRangeDeletesLogsForOwnedHabit() {
        String userId = "user-1";
        String habitId = "habit-4";
        LocalDate from = LocalDate.now().minusDays(3);
        LocalDate to = LocalDate.now();

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .type(HabitType.BOOLEAN)
                .build();

        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        habitLogService.removeLogsByHabitAndDateRange(userId, habitId, from, to);

        verify(habitLogRepository).deleteAllByHabitIdAndDateBetween(habitId, from, to);
    }

    @Test
    void removeLogByIdDeletesWhenLogBelongsToOwnedHabit() {
        String userId = "user-1";
        String habitId = "habit-5";
        String logId = "log-1";

        Habit habit = Habit.builder()
                .id(habitId)
                .userId(userId)
                .type(HabitType.BOOLEAN)
                .build();

        HabitLog log = HabitLog.builder()
                .id(logId)
                .habitId(habitId)
                .date(LocalDate.now())
                .value(true)
                .build();

        when(habitLogRepository.findById(logId)).thenReturn(Optional.of(log));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        habitLogService.removeLogById(userId, logId);

        verify(habitLogRepository).deleteById(logId);
    }

    @Test
    void removeLogByIdThrowsWhenLogNotFound() {
        String userId = "user-1";
        String logId = "missing-log";

        when(habitLogRepository.findById(logId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> habitLogService.removeLogById(userId, logId));
        verify(habitLogRepository, never()).deleteById(any());
    }

    @Test
    void removeLogByIdThrowsWhenHabitOwnershipIsInvalid() {
        String userId = "user-1";
        String habitId = "habit-6";
        String logId = "log-2";

        HabitLog log = HabitLog.builder()
                .id(logId)
                .habitId(habitId)
                .date(LocalDate.now())
                .value(true)
                .build();

        Habit otherUsersHabit = Habit.builder()
                .id(habitId)
                .userId("other-user")
                .type(HabitType.BOOLEAN)
                .build();

        when(habitLogRepository.findById(logId)).thenReturn(Optional.of(log));
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(otherUsersHabit));

        assertThrows(ResourceNotFoundException.class, () -> habitLogService.removeLogById(userId, logId));
        verify(habitLogRepository, never()).deleteById(any());
    }
}
