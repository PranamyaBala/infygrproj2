import apiClient from './client';
import type { AuthResponse, LoginRequest, RegisterRequest } from '../types';

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient.post<AuthResponse>('/api/users/login', data),

  register: (data: RegisterRequest) =>
    apiClient.post<AuthResponse>('/api/users/register', data),
};
