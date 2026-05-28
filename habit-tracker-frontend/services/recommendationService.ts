import { AxiosError } from 'axios';
import { httpClient } from './httpClient';
import { ApiError } from '../types/api';
import type { HabitRecommendation } from '../types/recommendation';

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

export const recommendationService = {
  async getHabitRecommendations(token: string): Promise<HabitRecommendation[]> {
    try {
      const response = await httpClient.get<HabitRecommendation[]>('/recommendations/habits', authHeaders(token));
      return response.data;
    } catch (error) {
      if (error instanceof AxiosError) {
        const status = error.response?.status ?? 500;
        const data = error.response?.data as ErrorResponseBody | undefined;
        throw new ApiError(status, getErrorMessage(status, data));
      }

      throw new ApiError(500, 'Unexpected error while loading recommendations');
    }
  },
};
