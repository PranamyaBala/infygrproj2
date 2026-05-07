import { useState, useEffect, useCallback } from 'react';
import { bookingApi } from '../api/bookingApi';
import { useAuth } from '../context/AuthContext';

interface SeenBookingState {
  [bookingId: string]: string; // bookingId -> last seen status
}

const STORAGE_KEY = 'hostelhub_seen_booking_statuses';

/**
 * Hook that tracks booking status changes (APPROVED / REJECTED)
 * and returns a count of unseen notifications.
 *
 * - Polls every 30 seconds for new status changes
 * - Stores "seen" statuses in localStorage
 * - Call markAllAsSeen() when the user visits My Bookings page
 */
export function useBookingNotifications() {
  const [unseenCount, setUnseenCount] = useState(0);
  const { isAuthenticated, isAdmin } = useAuth();

  const getSeenState = (): SeenBookingState => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored ? JSON.parse(stored) : {};
    } catch {
      return {};
    }
  };

  const checkForUpdates = useCallback(async () => {
    if (!isAuthenticated) {
      setUnseenCount(0);
      return;
    }

    try {
      if (isAdmin) {
        // Admin logic: Count all PENDING bookings
        const res = await bookingApi.getBookingsByStatus('PENDING');
        setUnseenCount(res.data.length);
      } else {
        // Student logic: Track status changes (APPROVED/REJECTED)
        const res = await bookingApi.getMyBookings();
        const bookings = res.data;
        const seenState = getSeenState();

        let count = 0;
        for (const booking of bookings) {
          if (['APPROVED', 'REJECTED'].includes(booking.status)) {
            const lastSeenStatus = seenState[String(booking.id)];
            if (lastSeenStatus !== booking.status) {
              count++;
            }
          }
        }
        setUnseenCount(count);
      }
    } catch {
      // Silently fail
    }
  }, [isAuthenticated, isAdmin]);

  const markAllAsSeen = useCallback(async () => {
    if (!isAuthenticated) return;

    try {
      if (isAdmin) {
        // For admin, "marking as seen" just refreshes the count
        // Real marking as seen happens when they process the bookings
        checkForUpdates();
      } else {
        const res = await bookingApi.getMyBookings();
        const bookings = res.data;
        const seenState = getSeenState();

        for (const booking of bookings) {
          seenState[String(booking.id)] = booking.status;
        }

        localStorage.setItem(STORAGE_KEY, JSON.stringify(seenState));
        setUnseenCount(0);
      }
    } catch {
      // Silently fail
    }
  }, [isAuthenticated, isAdmin, checkForUpdates]);

  useEffect(() => {
    // Initial check
    checkForUpdates();

    // Poll every 30 seconds
    const interval = setInterval(checkForUpdates, 15000);
    return () => clearInterval(interval);
  }, [checkForUpdates]);

  return { unseenCount, markAllAsSeen, checkForUpdates };
}
