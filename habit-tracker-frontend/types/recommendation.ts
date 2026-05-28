import type { Frequency, HabitType } from './habit';

export interface ClosestOwnHabit {
  id: string;
  name: string;
}

export interface HabitRecommendation {
  habitId: string;
  habitName: string;
  habitType: HabitType;
  habitFrequency: Frequency;
  ownerUserId: string;
  ownerUsername: string | null;
  graphDistance: number;
  similarityScore: number;
  weightedScore: number;
  closestOwnHabit: ClosestOwnHabit;
}
