import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Badge, Spinner, Alert, Button
} from 'react-bootstrap';
import {
  FaCalendarAlt, FaBed, FaClock, FaHashtag, FaMoneyBillWave, FaUsers, FaDownload
} from 'react-icons/fa';
import { bookingApi } from '../../api/bookingApi';
import type { Booking } from '../../types';

export default function MyBookingsPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  useEffect(() => {
    loadBookings();
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
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
