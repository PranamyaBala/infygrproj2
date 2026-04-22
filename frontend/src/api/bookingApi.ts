import apiClient from './client';
import type {
  Booking, CreateBookingRequest, UpdateBookingStatusRequest,
  LateCheckoutRequest, OccupancyReport, OccupiedDateRange
} from '../types';

export const bookingApi = {
  // Student endpoints
  createBooking: (data: CreateBookingRequest) =>
    apiClient.post<Booking>('/api/bookings', data),

  getMyBookings: () =>
    apiClient.get<Booking[]>('/api/bookings/my'),

  getBookingById: (id: number) =>
    apiClient.get<Booking>(`/api/bookings/${id}`),

  // Admin endpoints
  getAllBookings: () =>
    apiClient.get<Booking[]>('/api/admin/bookings'),

  getBookingsByStatus: (status: string) =>
    apiClient.get<Booking[]>(`/api/admin/bookings/status/${status}`),

  updateBookingStatus: (id: number, data: UpdateBookingStatusRequest) =>
    apiClient.put<Booking>(`/api/admin/bookings/${id}/status`, data),

  handleLateCheckout: (id: number, data: LateCheckoutRequest) =>
    apiClient.put<Booking>(`/api/bookings/${id}/late-checkout`, data),

  getOccupancyReport: (startDate: string, endDate: string) =>
    apiClient.get<OccupancyReport[]>(
      `/api/admin/bookings/reports/occupancy?startDate=${startDate}&endDate=${endDate}`
    ),

  exportCsv: () =>
    apiClient.get('/api/admin/bookings/export/csv', { responseType: 'blob' }),

  getOccupiedDates: (roomId: number) =>
    apiClient.get<OccupiedDateRange[]>(`/api/bookings/room/${roomId}/occupied-dates`),

  getAvailableBeds: (roomId: number, startDate: string, endDate: string) =>
    apiClient.get<number>(`/api/bookings/room/${roomId}/available-beds?startDate=${startDate}&endDate=${endDate}`),

  downloadReceipt: (id: number) =>
    apiClient.get(`/api/bookings/${id}/receipt`, { responseType: 'blob' }),
};
