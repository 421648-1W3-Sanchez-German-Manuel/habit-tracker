package com.tp1.habittracker.config;

import com.tp1.habittracker.domain.enums.Frequency;
import com.tp1.habittracker.domain.enums.HabitType;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.domain.model.HabitLog;
import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.UserRepository;
import com.tp1.habittracker.service.OllamaClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupDataSeeder implements CommandLineRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(StartupDataSeeder.class);

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final OllamaClient ollamaClient;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
                ensureDefaultHabits();

        if (hasExistingData()) {
            LOGGER.info("Skipping startup seed because data already exists.");
            return;
        }

        User user = userRepository.save(User.builder()
                .username("manu_sanchez")
                .email("manu.sanchez@gmail.com")
                .password(passwordEncoder.encode("seed-password"))
                .build());

                if (user == null || user.getId() == null) {
                        LOGGER.warn("Skipping startup seed because repositories are not returning persisted entities.");
                        return;
                }

        LocalDate today = LocalDate.now();
        List<HabitLog> allHabitLogs = new ArrayList<>();

        // High streak daily habit (45 days consecutive): Drink 2L water
        Habit highStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Drink 2L water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(50, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Drink 2L water"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogs(highStreakDaily.getId(), 45, today));

        // Medium streak daily habit (18 days consecutive): Read pages
        Habit mediumStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Read pages")
                .type(HabitType.NUMBER)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(25, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Read pages"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogsWithValues(mediumStreakDaily.getId(), 18, today, 15.0, 35.0));

        // Low streak daily habit (4 days consecutive): Exercise 30 min
        Habit lowStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Exercise 30 min")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Exercise 30 min"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogs(lowStreakDaily.getId(), 4, today));

        // Zero streak daily habit (created but no logs): Learning session
        Habit zeroStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Learning session")
                .type(HabitType.TEXT)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Learning session"))
                .build());
        // No logs for this habit - will show 0 streak

        // Medium streak weekly habit (6 weeks consecutive): Weekly planning
        Habit mediumStreakWeekly = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Weekly planning")
                .type(HabitType.TEXT)
                .frequency(Frequency.WEEKLY)
                .createdAt(Instant.now().minus(50, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Weekly planning"))
                .build());
        allHabitLogs.addAll(generateWeeklyHabitLogs(mediumStreakWeekly.getId(), 6, today));

        // Low streak monthly habit (3 months consecutive): Pay bills
        Habit lowStreakMonthly = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Pay bills")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.MONTHLY)
                .createdAt(Instant.now().minus(120, ChronoUnit.DAYS))
                .embedding(ollamaClient.generateEmbedding("Pay bills"))
                .build());
        allHabitLogs.addAll(generateMonthlyHabitLogs(lowStreakMonthly.getId(), 3, today));

        if (highStreakDaily == null || mediumStreakDaily == null || lowStreakDaily == null 
            || zeroStreakDaily == null || mediumStreakWeekly == null || lowStreakMonthly == null) {
                LOGGER.warn("Skipping habit log seed because repositories are not returning persisted habits.");
                return;
        }

        habitLogRepository.saveAll(allHabitLogs);

        LOGGER.info("Startup seed inserted: 1 user, 6 habits ({} high, {} medium, {} low, {} zero streak), {} habit logs.",
                1, 2, 2, 1, allHabitLogs.size());
    }

        private void ensureDefaultHabits() {
                seedDefaultHabit("Drink 2L water", HabitType.BOOLEAN, Frequency.DAILY, 7);
                seedDefaultHabit("Read pages", HabitType.NUMBER, Frequency.DAILY, 5);
                seedDefaultHabit("Weekly planning", HabitType.TEXT, Frequency.WEEKLY, 14);
                seedDefaultHabit("Check bank account", HabitType.BOOLEAN, Frequency.WEEKLY, 10);
                seedDefaultHabit("Pay credit card / bills", HabitType.BOOLEAN, Frequency.MONTHLY, 30);
                seedDefaultHabit("Take out the trash", HabitType.BOOLEAN, Frequency.WEEKLY, 9);
                seedDefaultHabit("Clean a room", HabitType.BOOLEAN, Frequency.WEEKLY, 12);
                seedDefaultHabit("Go grocery shopping", HabitType.BOOLEAN, Frequency.WEEKLY, 8);
                seedDefaultHabit("Track expenses", HabitType.TEXT, Frequency.DAILY, 6);
                seedDefaultHabit("Screen time check", HabitType.NUMBER, Frequency.DAILY, 4);
                seedDefaultHabit("Call or message a friend/family member", HabitType.BOOLEAN, Frequency.WEEKLY, 11);
                seedDefaultHabit("Review weekly goals", HabitType.TEXT, Frequency.WEEKLY, 7);
        }

        private void seedDefaultHabit(String name, HabitType type, Frequency frequency, long daysAgo) {
                if (habitRepository.existsByNameAndIsDefaultTrue(name)) {
                        return;
                }

                habitRepository.save(Habit.builder()
                                .userId(null)
                                .isDefault(true)
                                .name(name)
                                .type(type)
                                .frequency(frequency)
                                .createdAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS))
                                .embedding(ollamaClient.generateEmbedding(name))
                                .build());
        }

    private List<HabitLog> generateDailyHabitLogs(String habitId, long consecutiveDays, LocalDate endDate) {
        List<HabitLog> logs = new ArrayList<>();
        for (long i = consecutiveDays - 1; i >= 0; i--) {
            LocalDate logDate = endDate.minusDays(i);
            logs.add(HabitLog.builder()
                    .habitId(habitId)
                    .date(logDate)
                    .value(true)
                    .build());
        }
        return logs;
    }

    private List<HabitLog> generateDailyHabitLogsWithValues(String habitId, long consecutiveDays, LocalDate endDate, 
                                                             double minValue, double maxValue) {
        List<HabitLog> logs = new ArrayList<>();
        for (long i = consecutiveDays - 1; i >= 0; i--) {
            LocalDate logDate = endDate.minusDays(i);
            double value = minValue + (Math.random() * (maxValue - minValue));
            logs.add(HabitLog.builder()
                    .habitId(habitId)
                    .date(logDate)
                    .value(value)
                    .build());
        }
        return logs;
    }

    private List<HabitLog> generateWeeklyHabitLogs(String habitId, long consecutiveWeeks, LocalDate endDate) {
        List<HabitLog> logs = new ArrayList<>();
        LocalDate currentDate = endDate;
        for (long i = 0; i < consecutiveWeeks; i++) {
            logs.add(HabitLog.builder()
                    .habitId(habitId)
                    .date(currentDate)
                    .value("Week " + (consecutiveWeeks - i) + " completed")
                    .build());
            currentDate = currentDate.minusWeeks(1);
        }
        return logs;
    }

    private List<HabitLog> generateMonthlyHabitLogs(String habitId, long consecutiveMonths, LocalDate endDate) {
        List<HabitLog> logs = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(endDate);
        for (long i = 0; i < consecutiveMonths; i++) {
            logs.add(HabitLog.builder()
                    .habitId(habitId)
                    .date(currentMonth.atDay(1))
                    .value(true)
                    .build());
            currentMonth = currentMonth.minusMonths(1);
        }
        return logs;
    }

    private boolean hasExistingData() {
                return userRepository.count() > 0 || habitLogRepository.count() > 0;
    }
}
