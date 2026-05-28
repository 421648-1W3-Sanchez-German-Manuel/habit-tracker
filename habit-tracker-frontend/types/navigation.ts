import type { NavigatorScreenParams } from '@react-navigation/native';
import type { Habit } from './habit';

export type AuthStackParamList = {
  Login: undefined;
  Register: undefined;
};

export type MainTabParamList = {
  Habits: NavigatorScreenParams<HabitsStackParamList> | undefined;
  Home: undefined;
  Friends: NavigatorScreenParams<FriendsStackParamList> | undefined;
  Profile: undefined;
};

export type HabitsTopTabParamList = {
  DefaultHabits: undefined;
  MyHabits: undefined;
};

export type HabitsStackParamList = {
  HabitsHome: undefined;
  CreateHabit:
    | {
        mode?: 'create' | 'edit' | 'view';
        habit?: Habit;
        prefillName?: string;
      }
    | undefined;
};

export type FriendsTopTabParamList = {
  FriendsList: undefined;
  IncomingRequests: undefined;
  OutgoingRequests: undefined;
};

export type FriendsStackParamList = {
  FriendsHome: undefined;
};
