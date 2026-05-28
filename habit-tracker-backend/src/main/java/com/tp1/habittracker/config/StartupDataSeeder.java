package com.tp1.habittracker.config;

import com.tp1.habittracker.domain.enums.Frequency;
import com.tp1.habittracker.domain.enums.HabitType;
import com.tp1.habittracker.domain.model.Habit;
import com.tp1.habittracker.domain.model.HabitLog;
import com.tp1.habittracker.domain.model.User;
import com.tp1.habittracker.exception.UpstreamBadResponseException;
import com.tp1.habittracker.exception.UpstreamServiceUnavailableException;
import com.tp1.habittracker.repository.HabitLogRepository;
import com.tp1.habittracker.repository.HabitRepository;
import com.tp1.habittracker.repository.UserRepository;
import com.tp1.habittracker.repository.graph.HabitGraphRepository;
import com.tp1.habittracker.repository.graph.UserGraphRepository;
import com.tp1.habittracker.service.OllamaClient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private static final String MAIN_USERNAME = "manu_sanchez";
    private static final String FRIEND_LUCIA = "lucia_ramos";
    private static final String FRIEND_PEDRO = "pedro_garcia";
    private static final String FOF_VALENTINA = "valentina_lopez";
    private static final String PENDING_TOMAS = "tomas_diaz";

    private final UserRepository userRepository;
    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final UserGraphRepository userGraphRepository;
    private final HabitGraphRepository habitGraphRepository;
    private final OllamaClient ollamaClient;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureDefaultHabits();

        boolean bootstrap = !hasExistingData();
        if (bootstrap) {
            User mainUser = seedMainUser();
            if (mainUser == null) {
                LOGGER.warn("Skipping startup seed because repositories are not returning persisted entities.");
                return;
            }
            seedMainUserHabitsAndLogs(mainUser);
            seedDemoFriends();
        } else {
            LOGGER.info("Primary stores already populated; skipping bootstrap insert.");
        }

        mirrorUsersAndHabitsToGraph();

        if (bootstrap) {
            seedFriendshipTopology();
        }
    }

    // ---------------------------------------------------------------------
    // Default habits (templates) — idempotent per-name
    // ---------------------------------------------------------------------

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
                .embedding(tryGenerateEmbedding(name))
                .build());
    }

    // ---------------------------------------------------------------------
    // Main user + habits + logs (original demo data)
    // ---------------------------------------------------------------------

    private User seedMainUser() {
        return userRepository.save(User.builder()
                .username(MAIN_USERNAME)
                .email("manu.sanchez@gmail.com")
                .password(passwordEncoder.encode("seed-password"))
                .build());
    }

    private void seedMainUserHabitsAndLogs(User user) {
        LocalDate today = LocalDate.now();
        List<HabitLog> allHabitLogs = new ArrayList<>();

        Habit highStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Drink 2L water")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(50, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Drink 2L water"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogs(highStreakDaily.getId(), 45, today));

        Habit mediumStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Read pages")
                .type(HabitType.NUMBER)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(25, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Read pages"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogsWithValues(mediumStreakDaily.getId(), 18, today, 15.0, 35.0));

        Habit lowStreakDaily = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Exercise 30 min")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(10, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Exercise 30 min"))
                .build());
        allHabitLogs.addAll(generateDailyHabitLogs(lowStreakDaily.getId(), 4, today));

        habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Learning session")
                .type(HabitType.TEXT)
                .frequency(Frequency.DAILY)
                .createdAt(Instant.now().minus(5, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Learning session"))
                .build());

        Habit mediumStreakWeekly = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Weekly planning")
                .type(HabitType.TEXT)
                .frequency(Frequency.WEEKLY)
                .createdAt(Instant.now().minus(50, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Weekly planning"))
                .build());
        allHabitLogs.addAll(generateWeeklyHabitLogs(mediumStreakWeekly.getId(), 6, today));

        Habit lowStreakMonthly = habitRepository.save(Habit.builder()
                .userId(user.getId().toString())
                .isDefault(false)
                .name("Pay bills")
                .type(HabitType.BOOLEAN)
                .frequency(Frequency.MONTHLY)
                .createdAt(Instant.now().minus(120, ChronoUnit.DAYS))
                .embedding(tryGenerateEmbedding("Pay bills"))
                .build());
        allHabitLogs.addAll(generateMonthlyHabitLogs(lowStreakMonthly.getId(), 3, today));

        habitLogRepository.saveAll(allHabitLogs);

        LOGGER.info("Startup seed inserted main user with 6 habits and {} logs.", allHabitLogs.size());
    }

    // ---------------------------------------------------------------------
    // Demo friend users — owned habits only, no logs (logs aren't needed for the recommendation demo)
    // ---------------------------------------------------------------------

    private void seedDemoFriends() {
        seedFriendUserWithHabits(
                FRIEND_LUCIA,
                "lucia.ramos@example.com",
                List.of(
                        ownedHabitSpec("Meditate 10 minutes", HabitType.BOOLEAN, Frequency.DAILY),
                        ownedHabitSpec("Run 5 kilometers", HabitType.BOOLEAN, Frequency.DAILY)
                )
        );

        seedFriendUserWithHabits(
                FRIEND_PEDRO,
                "pedro.garcia@example.com",
                List.of(
                        ownedHabitSpec("Practice guitar", HabitType.BOOLEAN, Frequency.DAILY),
                        ownedHabitSpec("Cook dinner at home", HabitType.BOOLEAN, Frequency.DAILY)
                )
        );

        seedFriendUserWithHabits(
                FOF_VALENTINA,
                "valentina.lopez@example.com",
                List.of(
                        // Intentionally semantically close to manu's "Drink 2L water" — drives the FoF recommendation demo.
                        ownedHabitSpec("Hydrate frequently", HabitType.BOOLEAN, Frequency.DAILY),
                        ownedHabitSpec("Study Spanish", HabitType.TEXT, Frequency.DAILY)
                )
        );

        seedFriendUserWithHabits(
                PENDING_TOMAS,
                "tomas.diaz@example.com",
                List.of(
                        ownedHabitSpec("Journal entries", HabitType.TEXT, Frequency.DAILY)
                )
        );
    }

    private void seedFriendUserWithHabits(String username, String email, List<OwnedHabitSpec> habits) {
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        User saved = userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("seed-password"))
                .build());

        for (OwnedHabitSpec spec : habits) {
            habitRepository.save(Habit.builder()
                    .userId(saved.getId().toString())
                    .isDefault(false)
                    .name(spec.name())
                    .type(spec.type())
                    .frequency(spec.frequency())
                    .createdAt(Instant.now().minus(7, ChronoUnit.DAYS))
                    .embedding(tryGenerateEmbedding(spec.name()))
                    .build());
        }
    }

    // ---------------------------------------------------------------------
    // Graph mirror — always runs, idempotent via MERGE. Reflects current
    // state of Postgres (users) + Mongo (non-default habits) into Neo4j.
    // ---------------------------------------------------------------------

    private void mirrorUsersAndHabitsToGraph() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            userGraphRepository.ensureUserNode(user.getId().toString());
        }

        List<Habit> ownedHabits = habitRepository.findAll().stream()
                .filter(habit -> !habit.isDefault())
                .filter(habit -> habit.getUserId() != null)
                .toList();

        for (Habit habit : ownedHabits) {
            habitGraphRepository.upsertHabitNode(habit.getId(), habit.getName());
            userGraphRepository.linkHabit(habit.getUserId(), habit.getId());
        }

        LOGGER.info("Graph mirror: {} users, {} owned habits.", users.size(), ownedHabits.size());
    }

    // ---------------------------------------------------------------------
    // Friendship topology — first-boot only. After this, friendships are
    // user-driven via the API and must not be re-asserted from the seeder.
    // ---------------------------------------------------------------------

    private void seedFriendshipTopology() {
        Optional<String> manu = userIdByUsername(MAIN_USERNAME);
        Optional<String> lucia = userIdByUsername(FRIEND_LUCIA);
        Optional<String> pedro = userIdByUsername(FRIEND_PEDRO);
        Optional<String> valentina = userIdByUsername(FOF_VALENTINA);
        Optional<String> tomas = userIdByUsername(PENDING_TOMAS);

        if (manu.isEmpty()) {
            LOGGER.warn("Skipping friendship topology seed: main user not resolvable.");
            return;
        }

        lucia.ifPresent(id -> userGraphRepository.createFriendship(manu.get(), id));
        pedro.ifPresent(id -> userGraphRepository.createFriendship(manu.get(), id));
        if (pedro.isPresent() && valentina.isPresent()) {
            userGraphRepository.createFriendship(pedro.get(), valentina.get());
        }
        tomas.ifPresent(id -> userGraphRepository.createFriendRequest(id, manu.get()));

        LOGGER.info("Friendship topology seeded: manu↔lucia, manu↔pedro, pedro↔valentina, tomas→manu (pending).");
    }

    private Optional<String> userIdByUsername(String username) {
        return userRepository.findByUsernameIgnoreCase(username).map(u -> u.getId().toString());
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private List<Double> tryGenerateEmbedding(String text) {
        try {
            return ollamaClient.generateEmbedding(text);
        } catch (UpstreamServiceUnavailableException | UpstreamBadResponseException ex) {
            LOGGER.warn("Ollama unavailable while seeding '{}'; embedding will be empty. ({})", text, ex.getMessage());
            return List.of();
        }
    }

    private record OwnedHabitSpec(String name, HabitType type, Frequency frequency) {
    }

    private OwnedHabitSpec ownedHabitSpec(String name, HabitType type, Frequency frequency) {
        return new OwnedHabitSpec(name, type, frequency);
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
