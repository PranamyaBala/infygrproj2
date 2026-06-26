import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Badge, Spinner, Alert, Button, Modal
} from 'react-bootstrap';
import {
  FaCalendarAlt, FaBed, FaClock, FaHashtag, FaMoneyBillWave, FaUsers, FaDownload
} from 'react-icons/fa';
import { bookingApi } from '../../api/bookingApi';
import type { Booking } from '../../types';
import { useBookingNotifications } from '../../hooks/useBookingNotifications';

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { markAllAsSeen } = useBookingNotifications();

  // Cancel State
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [bookingToCancel, setBookingToCancel] = useState<Booking | null>(null);
  const [cancelPenaltyWarning, setCancelPenaltyWarning] = useState(false);
  const [cancelLoading, setCancelLoading] = useState(false);

  useEffect(() => {
    loadBookings();
    markAllAsSeen();
  }, []);

  const loadBookings = async () => {
    try {
      const res = await bookingApi.getMyBookings();
      setBookings(res.data);
    } catch {
      setError('Failed to load bookings.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancelClick = (e: React.MouseEvent, booking: Booking) => {
    e.stopPropagation();
    setBookingToCancel(booking);
    
    // Check if within 48 hours for APPROVED bookings
    if (booking.status === 'APPROVED') {
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      const start = new Date(booking.startDate);
      start.setHours(0, 0, 0, 0);
      const diffTime = start.getTime() - today.getTime();
      const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
      
      if (diffDays < 2) {
        setCancelPenaltyWarning(true);
      } else {
        setCancelPenaltyWarning(false);
      }
    } else {
      setCancelPenaltyWarning(false);
    }
    
    setShowCancelModal(true);
  };

  const confirmCancel = async () => {
    if (!bookingToCancel) return;
    setCancelLoading(true);
    try {
      await bookingApi.cancelBooking(bookingToCancel.id);
      setShowCancelModal(false);
      loadBookings(); // Refresh list
    } catch (err) {
      setError('Failed to cancel booking.');
    } finally {
      setCancelLoading(false);
      setBookingToCancel(null);
    }
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
      CHECKED_IN: 'info', CHECKED_OUT: 'secondary', CANCELLED: 'dark'
    };
    return <Badge bg={variants[status] || 'secondary'} className="fs-6">{status}</Badge>;
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'short', year: 'numeric'
    });
  };

  const handleDownloadReceipt = async (e: React.MouseEvent, bookingId: number, roomNumber: string) => {
    e.stopPropagation(); // Avoid card click navigation
    try {
      const response = await bookingApi.downloadReceipt(bookingId);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `Receipt_Room_${roomNumber}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download failed', err);
      // Fallback or toast if needed
    }
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Loading your bookings...</p>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <h3 className="fw-bold mb-4">
        <FaCalendarAlt className="me-2 text-primary" />My Bookings
      </h3>

      {error && <Alert variant="danger">{error}</Alert>}

      {bookings.length === 0 ? (
        <Card className="text-center py-5 border-0 shadow-sm">
          <Card.Body>
            <FaBed size={48} className="text-muted mb-3" />
            <h5 className="text-muted">No bookings yet</h5>
            <p className="text-muted">Start by searching for available rooms</p>
            <Button variant="primary" onClick={() => navigate('/rooms')}>
              Search Rooms
            </Button>
          </Card.Body>
        </Card>
      ) : (
        <Row>
          {bookings.map(booking => (
            <Col md={6} lg={4} key={booking.id} className="mb-4">
              <Card
                className="h-100 shadow-sm border-0"
                style={{ cursor: 'pointer', transition: 'transform 0.2s' }}
                onClick={() => navigate(`/bookings/${booking.id}`)}
                onMouseEnter={(e) => {
                  (e.currentTarget as HTMLElement).style.transform = 'translateY(-2px)';
                }}
                onMouseLeave={(e) => {
                  (e.currentTarget as HTMLElement).style.transform = 'translateY(0)';
                }}
              >
                <Card.Header className="bg-white d-flex justify-content-between align-items-center">
                  <span className="fw-bold">
                    <FaHashtag className="me-1 text-primary" />
                    {booking.bookingReference}
                  </span>
                  {getStatusBadge(booking.status)}
                </Card.Header>
                <Card.Body>
                  <div className="mb-2">
                    <FaBed className="text-primary me-2" />
                    <strong>Room {booking.roomNumber}</strong>
                  </div>
                  <div className="mb-2">
                    <FaClock className="text-muted me-2" />
                    <span>{formatDate(booking.startDate)} — {formatDate(booking.endDate)}</span>
                  </div>
                  <div className="mb-2">
                    <FaUsers className="text-muted me-2" />
                    <span>{booking.occupants} occupant{booking.occupants > 1 ? 's' : ''}</span>
                  </div>
                  <div className="d-flex justify-content-between align-items-center">
                    <span className="text-muted">Total Price</span>
                    <h5 className="text-primary fw-bold mb-0">
                      <FaMoneyBillWave className="me-1" />₹{booking.totalPrice + (booking.lateCheckoutFee || 0)}
                    </h5>
                  </div>
                  {['APPROVED', 'CHECKED_IN', 'CHECKED_OUT'].includes(booking.status) && (
                    <div className="d-grid mt-3">
                      <Button 
                        variant="outline-primary" 
                        size="sm"
                        onClick={(e) => handleDownloadReceipt(e, booking.id, booking.roomNumber)}
                      >
                        <FaDownload className="me-2" />Download Receipt
                      </Button>
                    </div>
                  )}
                  {['PENDING', 'APPROVED'].includes(booking.status) && new Date(booking.startDate) >= new Date(new Date().setHours(0,0,0,0)) && (
                    <div className="d-grid mt-2">
                      <Button 
                        variant="outline-danger" 
                        size="sm"
                        onClick={(e) => handleCancelClick(e, booking)}
                      >
                        Cancel Request
                      </Button>
                    </div>
                  )}
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}

      {/* Cancel Modal */}
      <Modal show={showCancelModal} onHide={() => setShowCancelModal(false)} centered>
        <Modal.Header closeButton className="bg-danger text-white">
          <Modal.Title>Cancel Booking</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {cancelPenaltyWarning ? (
            <Alert variant="warning">
              <Alert.Heading>Late Cancellation Penalty</Alert.Heading>
              <p className="mb-0">
                You are canceling less than 48 hours before your arrival. A <strong>15% cancellation penalty</strong> will be applied. Are you sure you want to proceed?
              </p>
            </Alert>
          ) : (
            <p>Are you sure you want to cancel this booking? This action cannot be undone.</p>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowCancelModal(false)}>
            Close
          </Button>
          <Button variant="danger" onClick={confirmCancel} disabled={cancelLoading}>
            {cancelLoading ? <Spinner size="sm" /> : 'Yes, Cancel'}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
