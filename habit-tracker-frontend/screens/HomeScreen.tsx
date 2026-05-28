import { useCallback, useMemo, useState } from 'react';
import { useFocusEffect, useNavigation } from '@react-navigation/native';
import type { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import {
  ActivityIndicator,
  Alert,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { HabitActionMenu } from '../components/HabitActionMenu';
import { HabitListItem } from '../components/HabitListItem';
import { habitService } from '../services/habitService';
import { recommendationService } from '../services/recommendationService';
import {
  buildLogsByHabitId,
  getCurrentPeriodLog,
  getTodayLocalDate,
  isHabitCompleted,
} from '../services/completionService';
import { useAuthStore } from '../store/authStore';
import { isApiError } from '../types/api';
import type { Habit, HabitLog, HabitLogsByHabitId, HabitStreakResponse } from '../types/habit';
import type { HabitRecommendation } from '../types/recommendation';
import type { MainTabParamList } from '../types/navigation';

const getDefaultCompletionValue = (habitType: Habit['type']): unknown => {
  switch (habitType) {
    case 'BOOLEAN':
      return true;
    case 'NUMBER':
      return 1;
    case 'TEXT':
      return 'completed';
  }
};

const mergeHabitLog = (logs: HabitLog[], log: HabitLog): HabitLog[] => {
  const withoutDuplicatedLog = logs.filter((entry) => entry.id !== log.id);
  return [log, ...withoutDuplicatedLog];
};

const isDuplicateCompletionError = (status: number | undefined, message: string) => {
  if (status === 409) {
    return true;
  }

  const normalized = message.toLowerCase();
  return (
    normalized.includes('duplicate') ||
    normalized.includes('already') ||
    normalized.includes('e11000') ||
    normalized.includes('conflict')
  );
};

const applyStreaks = (habits: Habit[], streaks: HabitStreakResponse[]): Habit[] => {
  const streakByHabitId = new Map(streaks.map((entry) => [entry.habitId, entry]));

  return habits.map((habit) => {
    const streak = streakByHabitId.get(habit.id);
    return {
      ...habit,
      currentStreak: streak?.currentStreak ?? 0,
      lastCompletedAt: streak?.lastCompletedAt ?? null,
    };
  });
};

const getDateNumeric = (value: string) => Number.parseInt(value.slice(0, 10).replace(/-/g, ''), 10);

const getWeekStartDateString = () => {
  const date = new Date();
  const day = date.getDay();
  const shift = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + shift);
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const dayOfMonth = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${dayOfMonth}`;
};

const getGreetingByHour = () => {
  const hour = new Date().getHours();

  if (hour < 12) {
    return 'Morning';
  }

  if (hour < 18) {
    return 'Afternoon';
  }

  return 'Evening';
};

const distanceLabel = (distance: number): string => {
  if (distance <= 1) {
    return 'Friend';
  }
  if (distance === 2) {
    return 'Friend of friend';
  }
  return `${distance} hops away`;
};

export const HomeScreen = () => {
  const insets = useSafeAreaInsets();
  const navigation = useNavigation<BottomTabNavigationProp<MainTabParamList>>();
  const token = useAuthStore((state) => state.token);
  const user = useAuthStore((state) => state.user);

  const [habits, setHabits] = useState<Habit[]>([]);
  const [logsByHabitId, setLogsByHabitId] = useState<HabitLogsByHabitId>({});
  const [recommendations, setRecommendations] = useState<HabitRecommendation[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [completingHabitIds, setCompletingHabitIds] = useState<string[]>([]);

  const loadHomeData = useCallback(
    async (showRefresh = false) => {
      if (!token || !user) {
        setHabits([]);
        setLogsByHabitId({});
        setRecommendations([]);
        setLoading(false);
        return;
      }

      if (showRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      try {
        const [userHabits, streaks, recs] = await Promise.all([
          habitService.getUserHabits(user.id, token),
          habitService.getHabitsWithStreaks(token),
          recommendationService.getHabitRecommendations(token).catch(() => [] as HabitRecommendation[]),
        ]);

        const habitsWithStreaks = applyStreaks(userHabits, streaks);
        const logs = await buildLogsByHabitId(habitsWithStreaks, token, habitService.getHabitLogs);

        setHabits(habitsWithStreaks);
        setLogsByHabitId(logs);
        setRecommendations(recs);
      } catch (loadError) {
        const message = isApiError(loadError)
          ? loadError.message
          : 'Could not load your home activity. Please try again.';
        setError(message);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    [token, user]
  );

  const handleCreateFromRecommendation = useCallback(
    (recommendation: HabitRecommendation) => {
      navigation.navigate('Habits', {
        screen: 'CreateHabit',
        params: { mode: 'create', prefillName: recommendation.habitName },
      });
    },
    [navigation]
  );

  useFocusEffect(
    useCallback(() => {
      void loadHomeData();
    }, [loadHomeData])
  );

  const isHabitCompletedForHabit = useCallback(
    (habit: Habit) => isHabitCompleted(habit, logsByHabitId[habit.id] ?? []),
    [logsByHabitId]
  );

  const handleCompleteHabit = useCallback(
    async (habit: Habit) => {
      if (!token) {
        return;
      }

      const habitId = habit.id;
      const habitLogs = logsByHabitId[habitId] ?? [];
      const currentlyCompleted = isHabitCompleted(habit, habitLogs);
      const nextCompleted = !currentlyCompleted;

      if (completingHabitIds.includes(habitId)) {
        return;
      }

      const today = getTodayLocalDate();
      setCompletingHabitIds((prev) => [...prev, habitId]);

      try {
        if (nextCompleted) {
          const createdLog = await habitService.createHabitLog(
            {
              habitId,
              date: today,
              value: getDefaultCompletionValue(habit.type),
            },
            token
          );

          setLogsByHabitId((prev) => ({
            ...prev,
            [habitId]: mergeHabitLog(prev[habitId] ?? [], createdLog),
          }));
        } else {
          const currentPeriodLog = getCurrentPeriodLog(habit, habitLogs);

          if (!currentPeriodLog) {
            const refreshedLogs = await habitService.getHabitLogs(habitId, token);
            setLogsByHabitId((prev) => ({
              ...prev,
              [habitId]: refreshedLogs,
            }));
            return;
          }

          await habitService.deleteHabitLogById(currentPeriodLog.id, token);
          setLogsByHabitId((prev) => ({
            ...prev,
            [habitId]: (prev[habitId] ?? []).filter((entry) => entry.id !== currentPeriodLog.id),
          }));
        }
      } catch (completeError) {
        const isDuplicateOnCheck =
          nextCompleted &&
          isDuplicateCompletionError(
            isApiError(completeError) ? completeError.status : undefined,
            isApiError(completeError) ? completeError.message : ''
          );

        if (isDuplicateOnCheck) {
          const refreshedLogs = await habitService.getHabitLogs(habitId, token);
          setLogsByHabitId((prev) => ({
            ...prev,
            [habitId]: refreshedLogs,
          }));
        } else {
          const message = isApiError(completeError)
            ? completeError.message
            : nextCompleted
              ? 'Could not mark habit as completed. Please try again.'
              : 'Could not unmark habit. Please try again.';
          Alert.alert('Could not update habit', message);
        }
      } finally {
        setCompletingHabitIds((prev) => prev.filter((id) => id !== habitId));
      }
    },
    [completingHabitIds, logsByHabitId, token]
  );

  const greeting = useMemo(() => `Good ${getGreetingByHour()}, ${user?.username ?? 'there'}`, [user?.username]);

  const completedTodayCount = useMemo(
    () => habits.filter((habit) => isHabitCompletedForHabit(habit)).length,
    [habits, isHabitCompletedForHabit]
  );

  const totalTodayCount = habits.length;
  const habitsToCompleteToday = useMemo(
    () => habits.filter((habit) => !isHabitCompletedForHabit(habit)),
    [habits, isHabitCompletedForHabit]
  );

  const bestStreak = useMemo(
    () => habits.reduce((max, habit) => Math.max(max, habit.currentStreak ?? 0), 0),
    [habits]
  );

  const completedThisWeek = useMemo(() => {
    const weekStart = getDateNumeric(getWeekStartDateString());
    const today = getDateNumeric(getTodayLocalDate());

    return Object.values(logsByHabitId).reduce((sum, logs) => {
      const weeklyLogs = logs.filter((log) => {
        const logDate = getDateNumeric(log.date);
        return logDate >= weekStart && logDate <= today;
      });

      return sum + weeklyLogs.length;
    }, 0);
  }, [logsByHabitId]);

  const progressRatio = totalTodayCount === 0 ? 0 : completedTodayCount / totalTodayCount;

  if (loading) {
    return (
      <View style={styles.centeredContainer}>
        <ActivityIndicator size="large" color="#0f766e" />
        <Text style={styles.subtitle}>Loading your home activity...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View style={styles.centeredContainer}>
        <Text style={styles.errorTitle}>Could not load Home</Text>
        <Text style={styles.errorSubtitle}>{error}</Text>
        <Pressable onPress={() => void loadHomeData()} style={({ pressed }) => [styles.retryButton, pressed && styles.retryPressed]}>
          <Text style={styles.retryText}>Try again</Text>
        </Pressable>
      </View>
    );
  }

  return (
    <View style={[styles.screen, { paddingTop: Math.max(insets.top, 8) }]}>
      <ScrollView
        contentContainerStyle={[styles.scrollContent, { paddingBottom: insets.bottom + 100 }]}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => void loadHomeData(true)} tintColor="#0f766e" />}
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.headerCard}>
          <Text style={styles.greeting}>{greeting}</Text>
          <Text style={styles.summaryText}>
            {habitsToCompleteToday.length} habits to complete today
          </Text>
          <View style={styles.progressRow}>
            <Text style={styles.progressLabel}>{completedTodayCount}/{totalTodayCount} completed</Text>
          </View>
          <View style={styles.progressTrack}>
            <View style={[styles.progressFill, { width: `${progressRatio * 100}%` }]} />
          </View>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionTitle}>Today</Text>
            <Text style={styles.sectionSubtitle}>Based on your schedule</Text>
          </View>

          {habitsToCompleteToday.length === 0 ? (
            <View style={styles.emptyCard}>
              <Text style={styles.emptyTitle}>All set for today</Text>
              <Text style={styles.emptySubtitle}>You completed all your scheduled habits.</Text>
            </View>
          ) : (
            <View style={styles.listStack}>
              {habitsToCompleteToday.map((habit) => {
                const isCompleting = completingHabitIds.includes(habit.id);
                return (
                  <HabitListItem
                    key={habit.id}
                    habit={habit}
                    completed={isHabitCompletedForHabit(habit)}
                    completionDisabled={isCompleting}
                    onToggleComplete={() => {
                      void handleCompleteHabit(habit);
                    }}
                    trailing={
                      <HabitActionMenu
                        habit={habit}
                        canEdit={!habit.sourceDefaultHabitId}
                        disabled={isCompleting}
                        onEdit={() => {
                          Alert.alert('Tip', 'Open My Habits to edit this habit.');
                        }}
                        onViewDetails={() => {
                          Alert.alert('Tip', 'Open My Habits to view details.');
                        }}
                        onDelete={() => {
                          Alert.alert('Tip', 'Open My Habits to remove this habit.');
                        }}
                      />
                    }
                  />
                );
              })}
            </View>
          )}
        </View>

        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Stats</Text>
          <View style={styles.statsGrid}>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>🔥 {bestStreak}</Text>
              <Text style={styles.statLabel}>Best streak</Text>
            </View>
            <View style={styles.statCard}>
              <Text style={styles.statValue}>{completedThisWeek}</Text>
              <Text style={styles.statLabel}>Completed this week</Text>
            </View>
          </View>
        </View>

        <View style={styles.section}>
          <View style={styles.sectionHeaderRow}>
            <Text style={styles.sectionTitle}>From your friends</Text>
            <Text style={styles.sectionSubtitle}>Habits we think you'll like</Text>
          </View>

          {recommendations.length === 0 ? (
            <Pressable
              accessibilityRole="button"
              onPress={() => navigation.navigate('Friends')}
              style={({ pressed }) => [styles.emptyCard, pressed && styles.emptyCardPressed]}
            >
              <Text style={styles.emptyTitle}>No recommendations yet</Text>
              <Text style={styles.emptySubtitle}>
                Add a friend to see habits they're doing that you might like.
              </Text>
            </Pressable>
          ) : (
            <View style={styles.listStack}>
              {recommendations.map((rec) => (
                <View key={rec.habitId} style={styles.socialCard}>
                  <View style={styles.socialMain}>
                    <Text style={styles.socialText}>
                      <Text style={styles.socialFriend}>{rec.ownerUsername ?? 'A friend'}</Text>
                      {` does "${rec.habitName}"`}
                    </Text>
                    <Text style={styles.socialHint} numberOfLines={1}>
                      Similar to your “{rec.closestOwnHabit.name}”
                    </Text>
                    <View style={styles.socialMetaRow}>
                      <View style={styles.distanceBadge}>
                        <MaterialCommunityIcons
                          name={rec.graphDistance === 1 ? 'account' : 'account-multiple'}
                          size={12}
                          color="#0f766e"
                        />
                        <Text style={styles.distanceBadgeText}>{distanceLabel(rec.graphDistance)}</Text>
                      </View>
                      <Text style={styles.matchScore}>{Math.round(rec.similarityScore * 100)}% match</Text>
                    </View>
                  </View>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Create habit ${rec.habitName}`}
                    onPress={() => handleCreateFromRecommendation(rec)}
                    style={({ pressed }) => [styles.recAddButton, pressed && styles.pressed]}
                  >
                    <MaterialCommunityIcons name="plus" size={20} color="#ffffff" />
                  </Pressable>
                </View>
              ))}
            </View>
          )}
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: '#f8fafc',
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 12,
    gap: 18,
  },
  centeredContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    backgroundColor: '#f8fafc',
  },
  subtitle: {
    marginTop: 8,
    fontSize: 14,
    color: '#475569',
    textAlign: 'center',
  },
  errorTitle: {
    fontSize: 20,
    fontWeight: '700',
    color: '#0f172a',
    textAlign: 'center',
  },
  errorSubtitle: {
    marginTop: 8,
    fontSize: 14,
    color: '#475569',
    textAlign: 'center',
  },
  retryButton: {
    marginTop: 16,
    backgroundColor: '#0f766e',
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 10,
  },
  retryPressed: {
    opacity: 0.85,
  },
  retryText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: '700',
  },
  headerCard: {
    backgroundColor: '#ffffff',
    borderRadius: 16,
    borderWidth: 1,
    borderColor: '#dbe3ee',
    padding: 16,
  },
  greeting: {
    fontSize: 24,
    fontWeight: '800',
    color: '#0f172a',
  },
  summaryText: {
    marginTop: 8,
    fontSize: 14,
    color: '#475569',
  },
  progressRow: {
    marginTop: 12,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  progressLabel: {
    fontSize: 13,
    color: '#334155',
    fontWeight: '700',
  },
  progressTrack: {
    marginTop: 8,
    height: 10,
    borderRadius: 999,
    backgroundColor: '#e2e8f0',
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#0f766e',
  },
  section: {
    gap: 10,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '800',
    color: '#0f172a',
  },
  sectionSubtitle: {
    fontSize: 12,
    color: '#64748b',
    fontWeight: '600',
  },
  listStack: {
    gap: 10,
  },
  emptyCard: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#dbe3ee',
    padding: 14,
  },
  emptyTitle: {
    fontSize: 15,
    fontWeight: '700',
    color: '#0f172a',
  },
  emptySubtitle: {
    marginTop: 4,
    fontSize: 13,
    color: '#64748b',
  },
  statsGrid: {
    flexDirection: 'row',
    gap: 10,
  },
  statCard: {
    flex: 1,
    backgroundColor: '#ffffff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#dbe3ee',
    paddingVertical: 14,
    paddingHorizontal: 12,
  },
  statValue: {
    fontSize: 20,
    fontWeight: '800',
    color: '#0f172a',
  },
  statLabel: {
    marginTop: 4,
    fontSize: 12,
    color: '#64748b',
    fontWeight: '600',
  },
  emptyCardPressed: {
    opacity: 0.85,
  },
  socialCard: {
    backgroundColor: '#ffffff',
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#dbe3ee',
    paddingHorizontal: 12,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  socialMain: {
    flex: 1,
    marginRight: 10,
  },
  socialText: {
    fontSize: 14,
    color: '#1e293b',
    lineHeight: 20,
  },
  socialFriend: {
    fontWeight: '800',
    color: '#0f172a',
  },
  socialHint: {
    marginTop: 2,
    fontSize: 12,
    color: '#64748b',
    fontStyle: 'italic',
  },
  socialMetaRow: {
    marginTop: 8,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  distanceBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 999,
    backgroundColor: '#ccfbf1',
  },
  distanceBadgeText: {
    fontSize: 11,
    fontWeight: '700',
    color: '#0f766e',
  },
  matchScore: {
    fontSize: 11,
    fontWeight: '700',
    color: '#64748b',
  },
  recAddButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    backgroundColor: '#0f766e',
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.85,
  },
});
