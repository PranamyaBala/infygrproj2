import { useState, useEffect } from 'react';
import {
  Container, Card, Table, Badge, Button, Form, Spinner, Modal
} from 'react-bootstrap';
import {
  FaCalendarAlt, FaCheckCircle, FaTimesCircle, FaFileDownload
} from 'react-icons/fa';
import { bookingApi } from '../../api/bookingApi';
import type { Booking } from '../../types';
import toast from 'react-hot-toast';

export default function BookingManagementPage() {
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');

  // Status Update + Late Checkout Modals
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedBooking, setSelectedBooking] = useState<Booking | null>(null);
  const [newStatus, setNewStatus] = useState('');
  const [statusNotes, setStatusNotes] = useState('');

  const [showLateCheckout, setShowLateCheckout] = useState(false);
  const [lateCheckoutFee, setLateCheckoutFee] = useState('');

  useEffect(() => {
    loadBookings();
  }, [statusFilter]);

  const loadBookings = async () => {
    setLoading(true);
    try {
      const res = statusFilter
        ? await bookingApi.getBookingsByStatus(statusFilter)
        : await bookingApi.getAllBookings();
      setBookings(res.data);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleStatusUpdate = async () => {
    if (!selectedBooking || !newStatus) return;
    try {
      await bookingApi.updateBookingStatus(selectedBooking.id, {
        status: newStatus,
        notes: statusNotes || undefined,
      });
      toast.success(`Booking ${selectedBooking.bookingReference} ${newStatus.toLowerCase()}!`);
      setShowStatusModal(false);
      loadBookings();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update status.');
    }
  };

  const handleLateCheckout = async () => {
    if (!selectedBooking) return;
    try {
      await bookingApi.handleLateCheckout(selectedBooking.id, {
        lateCheckoutFee: lateCheckoutFee ? parseFloat(lateCheckoutFee) : undefined,
      });
      toast.success('Late checkout processed!');
      setShowLateCheckout(false);
      loadBookings();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to process late checkout.');
    }
  };

  const handleExportCsv = async () => {
    try {
      const res = await bookingApi.exportCsv();
      const url = window.URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'bookings_export.csv');
      document.body.appendChild(link);
      link.click();
      link.remove();
      toast.success('CSV exported successfully!');
    } catch {
      toast.error('Failed to export CSV.');
    }
  };

  const openStatusModal = (booking: Booking, action: string) => {
    setSelectedBooking(booking);
    setNewStatus(action);
    setStatusNotes('');
    setShowStatusModal(true);
  };

  const formatDate = (d: string) =>
    new Date(d).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });

  const getStatusBadge = (status: string) => {
    const v: Record<string, string> = {
      PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger',
      CHECKED_IN: 'info', CHECKED_OUT: 'secondary', CANCELLED: 'dark'
    };
    return <Badge bg={v[status] || 'secondary'}>{status}</Badge>;
  };

  return (
    <Container fluid className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h3 className="fw-bold mb-0">
          <FaCalendarAlt className="me-2 text-primary" />Booking Management
        </h3>
        <Button variant="outline-success" onClick={handleExportCsv}>
          <FaFileDownload className="me-2" />Export CSV
        </Button>
      </div>

      <Card className="border-0 shadow-sm">
        <Card.Header className="bg-white d-flex justify-content-between align-items-center">
          <h5 className="mb-0 fw-bold">All Booking Requests</h5>
          <Form.Select
            style={{ width: '200px' }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="CHECKED_IN">Checked In</option>
            <option value="CHECKED_OUT">Checked Out</option>
          </Form.Select>
        </Card.Header>
        <Card.Body className="p-0">
          {loading ? (
            <div className="text-center py-5"><Spinner animation="border" variant="primary" /></div>
          ) : (
            <Table responsive hover className="mb-0">
              <thead className="table-light">
                <tr>
                  <th>Reference</th>
                  <th>Student</th>
                  <th>Room</th>
                  <th>Dates</th>
                  <th>Occupants</th>
                  <th>Total</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {bookings.length === 0 ? (
                  <tr><td colSpan={8} className="text-center py-4 text-muted">No bookings found</td></tr>
                ) : (
                  bookings.map(b => (
                    <tr key={b.id}>
                      <td className="fw-bold">{b.bookingReference}</td>
                      <td>
                        <div>{b.studentName}</div>
                        <small className="text-muted">{b.studentEmail}</small>
                      </td>
                      <td>{b.roomNumber}</td>
                      <td>
                        <small>{formatDate(b.startDate)}<br/>{formatDate(b.endDate)}</small>
                      </td>
                      <td>{b.occupants}</td>
                      <td>
                        <div className="fw-bold text-dark">₹{b.totalPrice + (b.lateCheckoutFee || 0)}</div>
                        {b.lateCheckoutRequested && (
                          <Badge bg="warning" text="dark" style={{ fontSize: '0.65rem' }}>
                            +₹{b.lateCheckoutFee} Late Fee
                          </Badge>
                        )}
                      </td>
                      <td>{getStatusBadge(b.status)}</td>
                      <td>
                        <div className="d-flex gap-1 flex-wrap">
                          {b.status === 'PENDING' && (
                            <>
                              <Button
                                variant="success" size="sm"
                                onClick={() => openStatusModal(b, 'APPROVED')}
                              >
                                <FaCheckCircle className="me-1" />Approve
                              </Button>
                              <Button
                                variant="danger" size="sm"
                                onClick={() => openStatusModal(b, 'REJECTED')}
                              >
                                <FaTimesCircle className="me-1" />Reject
                              </Button>
                            </>
                          )}
                          {b.status === 'APPROVED' && (
                            <Button
                              variant="info" size="sm"
                              onClick={() => openStatusModal(b, 'CHECKED_IN')}
                            >
                              Check In
                            </Button>
                          )}
                          {b.status === 'CHECKED_IN' && (
                            <>
                              <Button
                                variant="secondary" size="sm"
                                onClick={() => openStatusModal(b, 'CHECKED_OUT')}
                              >
                                Check Out
                              </Button>
                              <Button
                                variant="outline-warning" size="sm"
                                onClick={() => {
                                  setSelectedBooking(b);
                                  setLateCheckoutFee('');
                                  setShowLateCheckout(true);
                                }}
                              >
                                Late Checkout
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </Table>
          )}
        </Card.Body>
      </Card>

      {/* Status Update Confirmation */}
      <Modal show={showStatusModal} onHide={() => setShowStatusModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>
            {newStatus === 'APPROVED' ? 'Approve' : newStatus === 'REJECTED' ? 'Reject' : 'Update'} Booking
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <p>
            {newStatus === 'APPROVED'
              ? `Approve booking ${selectedBooking?.bookingReference} for Room ${selectedBooking?.roomNumber}?`
              : newStatus === 'REJECTED'
              ? `Reject booking ${selectedBooking?.bookingReference}?`
              : `Update booking ${selectedBooking?.bookingReference} status to ${newStatus}?`
            }
          </p>
          <Form.Group>
            <Form.Label>Notes (optional)</Form.Label>
            <Form.Control
              as="textarea" rows={2}
              value={statusNotes}
              onChange={(e) => setStatusNotes(e.target.value)}
            />
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowStatusModal(false)}>Cancel</Button>
          <Button
            variant={newStatus === 'REJECTED' ? 'danger' : 'success'}
            onClick={handleStatusUpdate}
          >
            Confirm
          </Button>
        </Modal.Footer>
      </Modal>

      {/* Late Checkout Modal */}
      <Modal show={showLateCheckout} onHide={() => setShowLateCheckout(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Late Checkout — {selectedBooking?.bookingReference}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label className="fw-bold">Late Checkout Fee (₹)</Form.Label>
            <Form.Control
              type="number" min={0} step="0.01"
              placeholder="Leave blank for auto-calculated 15% fee"
              value={lateCheckoutFee}
              onChange={(e) => setLateCheckoutFee(e.target.value)}
            />
            <Form.Text className="text-muted">
              Default: 15% of total price (₹{((selectedBooking?.totalPrice || 0) * 0.15).toFixed(2)})
            </Form.Text>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowLateCheckout(false)}>Cancel</Button>
          <Button variant="warning" onClick={handleLateCheckout}>Process Late Checkout</Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
