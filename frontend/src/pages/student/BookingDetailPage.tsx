import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Badge, Spinner, Alert, Button
} from 'react-bootstrap';
import {
  FaArrowLeft, FaBed, FaClock, FaHashtag, FaMoneyBillWave,
  FaUsers, FaCheckCircle, FaUser, FaEnvelope
} from 'react-icons/fa';
import { bookingApi } from '../../api/bookingApi';
import type { Booking } from '../../types';
import toast from 'react-hot-toast';

export default function BookingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [booking, setBooking] = useState<Booking | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadBooking();
  }, [id]);

  const loadBooking = async () => {
    try {
      const res = await bookingApi.getBookingById(Number(id));
      setBooking(res.data);
    } catch {
      setError('Booking not found.');
    } finally {
      setLoading(false);
    }
  };

  const handleRequestLateCheckout = async () => {
    if (!id || !booking) return;
    
    // US 14: Fee logic is 15% of total price
    const estimatedFee = booking.totalPrice * 0.15;
    
    if (!window.confirm(`Requesting late checkout will add a 15% fee (approx. ₹${estimatedFee.toFixed(2)}) to your total bill. Do you want to proceed?`)) {
      return;
    }

    setSubmitting(true);
    try {
      await bookingApi.handleLateCheckout(Number(id), {
        lateCheckoutFee: estimatedFee,
        notes: booking.notes ? `${booking.notes} | Requested Late Checkout` : 'Requested Late Checkout'
      });
      toast.success('Late checkout requested successfully!');
      loadBooking();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to request late checkout.');
    } finally {
      setSubmitting(false);
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleDateString('en-IN', {
      day: '2-digit', month: 'long', year: 'numeric'
    });
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
      CHECKED_IN: 'info', CHECKED_OUT: 'secondary', CANCELLED: 'dark'
    };
    return <Badge bg={variants[status] || 'secondary'} className="fs-5 px-3 py-2">{status}</Badge>;
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </Container>
    );
  }

  if (error || !booking) {
    return (
      <Container className="py-5">
        <Alert variant="danger">{error}</Alert>
        <Button variant="secondary" onClick={() => navigate('/bookings')}>
          <FaArrowLeft className="me-2" />Back to Bookings
        </Button>
      </Container>
    );
  }

  const isConfirmed = booking.status === 'APPROVED' || booking.status === 'CHECKED_IN';

  return (
    <Container className="py-4">
      <Button variant="outline-secondary" className="mb-3" onClick={() => navigate('/bookings')}>
        <FaArrowLeft className="me-2" />Back to My Bookings
      </Button>

      {isConfirmed && (
        <Alert variant="success" className="d-flex align-items-center gap-2">
          <FaCheckCircle size={24} />
          <div>
            <strong>Your room booking has been confirmed!</strong>
            <p className="mb-0 small">You will receive a confirmation email shortly.</p>
          </div>
        </Alert>
      )}

      <Row>
        <Col lg={8}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white d-flex justify-content-between align-items-center py-3">
              <h4 className="mb-0 fw-bold">
                <FaHashtag className="text-primary me-2" />
                {booking.bookingReference}
              </h4>
              {getStatusBadge(booking.status)}
            </Card.Header>
            <Card.Body>
              <Row className="g-4">
                <Col sm={6}>
                  <div className="p-3 bg-light rounded">
                    <small className="text-muted d-block mb-1">Room</small>
                    <h5 className="fw-bold mb-0"><FaBed className="me-2 text-primary" />Room {booking.roomNumber}</h5>
                  </div>
                </Col>
                <Col sm={6}>
                  <div className="p-3 bg-light rounded">
                    <small className="text-muted d-block mb-1">Occupants</small>
                    <h5 className="fw-bold mb-0"><FaUsers className="me-2 text-primary" />{booking.occupants}</h5>
                  </div>
                </Col>
                <Col sm={6}>
                  <div className="p-3 bg-light rounded">
                    <small className="text-muted d-block mb-1">Check-in</small>
                    <h5 className="fw-bold mb-0"><FaClock className="me-2 text-primary" />{formatDate(booking.startDate)}</h5>
                  </div>
                </Col>
                <Col sm={6}>
                  <div className="p-3 bg-light rounded">
                    <small className="text-muted d-block mb-1">Check-out</small>
                    <h5 className="fw-bold mb-0"><FaClock className="me-2 text-primary" />{formatDate(booking.endDate)}</h5>
                  </div>
                </Col>
              </Row>

              {booking.notes && (
                <div className="mt-3 p-3 bg-light rounded">
                  <small className="text-muted d-block mb-1">Notes</small>
                  <p className="mb-0">{booking.notes}</p>
                </div>
              )}
            </Card.Body>
          </Card>
        </Col>

        <Col lg={4}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body className="text-center">
              <h6 className="text-muted mb-2">Total Price</h6>
              <h2 className="text-primary fw-bold">
                <FaMoneyBillWave className="me-2" />₹{(booking.totalPrice + (booking.lateCheckoutFee || 0)).toFixed(2)}
              </h2>
              {booking.lateCheckoutFee && booking.lateCheckoutFee > 0 && (
                <div className="mt-2 p-2 bg-warning bg-opacity-10 rounded border border-warning">
                  <small className="text-warning fw-bold">
                    Includes ₹{booking.lateCheckoutFee.toFixed(2)} Late Checkout Fee
                  </small>
                </div>
              )}
            </Card.Body>
          </Card>

          {/* US 14: Late Checkout Action */}
          {booking.status === 'CHECKED_IN' && !booking.lateCheckoutRequested && (
            <Card className="border-0 shadow-sm mb-4 bg-primary text-white">
              <Card.Body className="text-center">
                <FaClock size={32} className="mb-3" />
                <h5>Need more time?</h5>
                <p className="small mb-3">Request a late checkout for a small fee (15% of total price).</p>
                <Button 
                  variant="light" 
                  className="w-100 fw-bold text-primary"
                  onClick={handleRequestLateCheckout}
                  disabled={submitting}
                >
                  {submitting ? 'Requesting...' : 'Request Late Checkout'}
                </Button>
              </Card.Body>
            </Card>
          )}

          {booking.lateCheckoutRequested && (
            <Alert variant="info" className="mb-4">
              <FaCheckCircle className="me-2" />
              Late checkout has been applied to this booking.
            </Alert>
          )}

          <Card className="border-0 shadow-sm">
            <Card.Body>
              <h6 className="fw-bold mb-3">Student Details</h6>
              <div className="mb-2">
                <FaUser className="text-primary me-2" />
                {booking.studentName}
              </div>
              <div>
                <FaEnvelope className="text-primary me-2" />
                {booking.studentEmail}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
