// ==================== USER TYPES ====================
export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  phone?: string;
  role: 'STUDENT' | 'ADMIN';
  profilePicturePath?: string;
}

export interface AuthResponse {
  token: string;
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
}

export interface UpdateProfileRequest {
  firstName?: string;
  lastName?: string;
  phone?: string;
  email?: string;
}

export interface UserPreference {
  id?: number;
  preferredRoomType?: string;
  preferredFloor?: number;
  preferredAmenities?: string[];
  preferredMinPrice?: number;
  preferredMaxPrice?: number;
}

// ==================== ROOM TYPES ====================
export interface Room {
  id: number;
  roomNumber: string;
  roomType: string;
  floor: number;
  capacity: number;
  pricePerNight: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'MAINTENANCE';
  description?: string;
  imagePath?: string;
  floorPlanPath?: string;
  maintenanceStartDate?: string;
  maintenanceEndDate?: string;
  occupiedStartDate?: string;
  occupiedEndDate?: string;
  amenities?: Amenity[];
  pricingTiers?: PricingTier[];
  currentPrice: number;
  basePriceWithAmenities: number;
  availableBeds?: number;
}

export interface Amenity {
  id: number;
  name: string;
  icon?: string;
  description?: string;
  price?: number;
}

export interface RoomSearchCriteria {
  roomType?: string;
  floor?: number;
  amenities?: string[];
  minPrice?: number;
  maxPrice?: number;
  status?: string;
  minCapacity?: number;
}

export interface CreateRoomRequest {
  roomNumber: string;
  roomType: string;
  floor: number;
  capacity: number;
  pricePerNight: number;
  description?: string;
  imagePath?: string;
  floorPlanPath?: string;
  amenityIds?: number[];
}

export interface UpdateRoomStatusRequest {
  status: string;
  maintenanceStartDate?: string;
  maintenanceEndDate?: string;
  occupiedStartDate?: string;
  occupiedEndDate?: string;
}

export interface PricingTier {
  id?: number;
  seasonName: string;
  startDate: string;
  endDate: string;
  priceMultiplier: number;
}

export interface RoomEvent {
  id?: number;
  eventName: string;
  groupName?: string;
  startDate: string;
  endDate: string;
}

// ==================== BOOKING TYPES ====================
export interface Booking {
  id: number;
  userId: number;
  roomId: number;
  roomNumber: string;
  studentName: string;
  studentEmail: string;
  startDate: string;
  endDate: string;
  occupants: number;
  status: string;
  totalPrice: number;
  bookingReference: string;
  lateCheckoutRequested?: boolean;
  lateCheckoutFee?: number;
  notes?: string;
  createdAt?: string;
}

export interface CreateBookingRequest {
  roomId: number;
  startDate: string;
  endDate: string;
  occupants: number;
  notes?: string;
}

export interface UpdateBookingStatusRequest {
  status: string;
  notes?: string;
}

export interface LateCheckoutRequest {
  lateCheckoutFee?: number;
  notes?: string;
}

export interface OccupancyReport {
  roomId: number;
  roomNumber: string;
  roomType: string;
  totalDays: number;
  occupiedDays: number;
  occupancyRate: number;
  revenue: number;
}

export interface OccupiedDateRange {
  startDate: string;
  endDate: string;
}

