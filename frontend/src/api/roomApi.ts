import apiClient from './client';
import type {
  Room, Amenity, RoomSearchCriteria, CreateRoomRequest,
  UpdateRoomStatusRequest, PricingTier, RoomEvent
} from '../types';

export const roomApi = {
  // Student endpoints
  searchRooms: (criteria: RoomSearchCriteria) => {
    const params = new URLSearchParams();
    if (criteria.roomType) params.append('roomType', criteria.roomType);
    if (criteria.floor) params.append('floor', criteria.floor.toString());
    if (criteria.minPrice) params.append('minPrice', criteria.minPrice.toString());
    if (criteria.maxPrice) params.append('maxPrice', criteria.maxPrice.toString());
    if (criteria.status) params.append('status', criteria.status);
    if (criteria.amenities) {
      criteria.amenities.forEach(a => params.append('amenities', a));
    }
    return apiClient.get<Room[]>(`/api/rooms/search?${params.toString()}`);
  },

  getRoomById: (id: number) =>
    apiClient.get<Room>(`/api/rooms/${id}`),

  getRoomByNumber: (roomNumber: string) =>
    apiClient.get<Room>(`/api/rooms/number/${roomNumber}`),

  getAllRooms: () =>
    apiClient.get<Room[]>('/api/rooms'),

  getAllAmenities: () =>
    apiClient.get<Amenity[]>('/api/rooms/amenities'),

  // Admin endpoints
  createRoom: (data: CreateRoomRequest) =>
    apiClient.post<Room>('/api/admin/rooms', data),

  updateRoom: (id: number, data: CreateRoomRequest) =>
    apiClient.put<Room>(`/api/admin/rooms/${id}`, data),

  updateRoomStatus: (id: number, data: UpdateRoomStatusRequest) =>
    apiClient.put<Room>(`/api/admin/rooms/${id}/status`, data),

  getRoomsByStatus: (status: string) =>
    apiClient.get<Room[]>(`/api/admin/rooms/status/${status}`),

  addPricingTier: (roomId: number, data: PricingTier) =>
    apiClient.post<PricingTier>(`/api/admin/rooms/${roomId}/pricing`, data),

  getPricingTiers: (roomId: number) =>
    apiClient.get<PricingTier[]>(`/api/admin/rooms/${roomId}/pricing`),

  assignRoomToEvent: (roomId: number, data: RoomEvent) =>
    apiClient.post<RoomEvent>(`/api/admin/rooms/${roomId}/events`, data),

  getRoomEvents: (roomId: number) =>
    apiClient.get<RoomEvent[]>(`/api/admin/rooms/${roomId}/events`),
};
