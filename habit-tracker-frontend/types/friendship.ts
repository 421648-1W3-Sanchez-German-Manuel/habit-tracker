export interface UserSummary {
  id: string;
  username: string;
}

export interface FriendshipOverview {
  friends: UserSummary[];
  incomingRequests: UserSummary[];
  outgoingRequests: UserSummary[];
}
