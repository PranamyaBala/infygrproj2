import apiClient from './client';
import type { User, UpdateProfileRequest, UserPreference } from '../types';

export const userApi = {
  getProfile: () =>
    apiClient.get<User>('/api/users/profile'),

  updateProfile: (data: UpdateProfileRequest) =>
    apiClient.put<User>('/api/users/profile', data),

  uploadProfilePicture: (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<User>('/api/users/profile/picture', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
  },

  deleteProfilePicture: () =>
    apiClient.delete<User>('/api/users/profile/picture'),

  getPreferences: () =>
    apiClient.get<UserPreference>('/api/users/preferences'),

  savePreferences: (data: UserPreference) =>
    apiClient.put<UserPreference>('/api/users/preferences', data),

  getUserById: (id: number) =>
    apiClient.get<User>(`/api/users/${id}`),

  getAllUsers: () =>
    apiClient.get<User[]>('/api/users'),
};
