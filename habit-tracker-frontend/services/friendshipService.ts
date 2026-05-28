import { AxiosError } from 'axios';
import { httpClient } from './httpClient';
import { ApiError } from '../types/api';
import type { FriendshipOverview, UserSummary } from '../types/friendship';

type ErrorResponseBody = {
  message?: string;
  error?: string;
};

const getErrorMessage = (status: number, data?: ErrorResponseBody): string => {
  if (status === 401 || status === 403) {
    return 'You are not authorized to perform this action.';
  }

  if (status >= 500) {
    return 'Something went wrong. Please try again later.';
  }

  return data?.message || data?.error || 'Request failed';
};

const authHeaders = (token: string) => ({
  headers: {
    Authorization: `Bearer ${token}`,
  },
});

const toApiError = (error: unknown, fallback: string): ApiError => {
  if (error instanceof AxiosError) {
    const status = error.response?.status ?? 500;
    const data = error.response?.data as ErrorResponseBody | undefined;
    return new ApiError(status, getErrorMessage(status, data));
  }

  return new ApiError(500, fallback);
};

export const friendshipService = {
  async getOverview(token: string): Promise<FriendshipOverview> {
    try {
      const response = await httpClient.get<FriendshipOverview>('/friends/overview', authHeaders(token));
      return response.data;
    } catch (error) {
      throw toApiError(error, 'Unexpected error while loading friends');
    }
  },

  async listFriends(token: string): Promise<UserSummary[]> {
    try {
      const response = await httpClient.get<UserSummary[]>('/friends', authHeaders(token));
      return response.data;
    } catch (error) {
      throw toApiError(error, 'Unexpected error while loading friends');
    }
  },

  async listIncomingRequests(token: string): Promise<UserSummary[]> {
    try {
      const response = await httpClient.get<UserSummary[]>('/friends/requests/incoming', authHeaders(token));
      return response.data;
    } catch (error) {
      throw toApiError(error, 'Unexpected error while loading incoming requests');
    }
  },

  async listOutgoingRequests(token: string): Promise<UserSummary[]> {
    try {
      const response = await httpClient.get<UserSummary[]>('/friends/requests/outgoing', authHeaders(token));
      return response.data;
    } catch (error) {
      throw toApiError(error, 'Unexpected error while loading outgoing requests');
    }
  },

  async sendRequest(targetUsername: string, token: string): Promise<UserSummary> {
    try {
      const response = await httpClient.post<UserSummary>(
        '/friends/requests',
        { targetUsername },
        authHeaders(token)
      );
      return response.data;
    } catch (error) {
      throw toApiError(error, 'Unexpected error while sending friend request');
    }
  },

  async acceptRequest(requesterId: string, token: string): Promise<void> {
    try {
      await httpClient.post(`/friends/requests/${requesterId}/accept`, null, authHeaders(token));
    } catch (error) {
      throw toApiError(error, 'Unexpected error while accepting friend request');
    }
  },

  async declineRequest(requesterId: string, token: string): Promise<void> {
    try {
      await httpClient.post(`/friends/requests/${requesterId}/decline`, null, authHeaders(token));
    } catch (error) {
      throw toApiError(error, 'Unexpected error while declining friend request');
    }
  },

  async cancelOutgoingRequest(targetId: string, token: string): Promise<void> {
    try {
      await httpClient.delete(`/friends/requests/outgoing/${targetId}`, authHeaders(token));
    } catch (error) {
      throw toApiError(error, 'Unexpected error while cancelling friend request');
    }
  },

  async unfriend(userId: string, token: string): Promise<void> {
    try {
      await httpClient.delete(`/friends/${userId}`, authHeaders(token));
    } catch (error) {
      throw toApiError(error, 'Unexpected error while removing friend');
    }
  },
};
