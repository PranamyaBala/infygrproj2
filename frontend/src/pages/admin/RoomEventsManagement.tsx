import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Table, Badge, Button, Form,
  Spinner, Alert, Modal
} from 'react-bootstrap';
import {
  FaArrowLeft, FaCalendarCheck, FaUsers, FaPlus,
  FaDoorOpen, FaShieldAlt
} from 'react-icons/fa';
import { roomApi } from '../../api/roomApi';
import type { Room, RoomEvent } from '../../types';
import toast from 'react-hot-toast';

export default function RoomEventsManagement() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [room, setRoom] = useState<Room | null>(null);
  const [events, setEvents] = useState<RoomEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Add Event Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newEvent, setNewEvent] = useState<RoomEvent>({
    eventName: '', 
    groupName: '', 
    startDate: '', 
    endDate: ''
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, [roomId]);

  const loadData = async () => {
    if (!roomId) return;
    setLoading(true);
    try {
      const [roomRes, eventsRes] = await Promise.all([
        roomApi.getRoomById(Number(roomId)),
        roomApi.getRoomEvents(Number(roomId))
      ]);
      setRoom(roomRes.data);
      setEvents(eventsRes.data);
    } catch (err: any) {
      setError('Failed to load event data.');
    } finally {
      setLoading(false);
    }
  };

  const handleAssignEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!roomId) return;
    setSubmitting(true);
    try {
      await roomApi.assignRoomToEvent(Number(roomId), newEvent);
      toast.success('Room assigned to event successfully!');
      setShowAddModal(false);
      loadData();
      setNewEvent({ eventName: '', groupName: '', startDate: '', endDate: '' });
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to assign event. Dates might overlap with existing assignments.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <Container className="py-5 text-center"><Spinner animation="border" variant="primary" /></Container>;
  }

  if (error || !room) {
    return (
      <Container className="py-5">
        <Alert variant="danger">{error || 'Room not found'}</Alert>
        <Button variant="secondary" onClick={() => navigate('/admin')}>
          <FaArrowLeft className="me-2" />Back to Dashboard
        </Button>
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <Button variant="outline-secondary" size="sm" className="mb-2" onClick={() => navigate('/admin')}>
            <FaArrowLeft className="me-2" />Back
          </Button>
          <h3 className="fw-bold mb-0">
            <FaCalendarCheck className="me-2 text-primary" />
            Event Assignments: Room {room.roomNumber}
          </h3>
          <p className="text-muted mb-0">{room.roomType} Room | Floor {room.floor}</p>
        </div>
        <Button variant="primary" onClick={() => setShowAddModal(true)}>
          <FaPlus className="me-2" />Assign New Event
        </Button>
      </div>

      <Row>
        <Col lg={4}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body>
              <h5 className="fw-bold mb-3"><FaShieldAlt className="me-2 text-warning" />Room Reservation</h5>
              <p className="small text-muted">
                Assigning a room to an event or specific group "blocks" it from regular student availability during the selected dates.
              </p>
              <div className="bg-light p-3 rounded mb-3">
                <h6 className="fw-bold small mb-2 text-uppercase">Validation Rules</h6>
                <ul className="small text-muted ps-3 mb-0">
                  <li>No overlapping events allowed.</li>
                  <li>Regular students will see this room as "Unavailable" during these dates.</li>
                  <li>Group Name is optional (useful for clubs or sports teams).</li>
                </ul>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={8}>
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white">
              <h5 className="mb-0 fw-bold"><FaUsers className="me-2" />Group & Event History</h5>
            </Card.Header>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Event Name</th>
                    <th>Group / Team</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {events.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-center py-4 text-muted">
                        No events assigned to this room.
                      </td>
                    </tr>
                  ) : (
                    events.map(event => {
                      const today = new Date().toISOString().split('T')[0];
                      const isActive = today >= event.startDate && today <= event.endDate;
                      const isPast = today > event.endDate;

                      return (
                        <tr key={event.id}>
                          <td className="fw-bold">{event.eventName}</td>
                          <td>{event.groupName || <em className="text-muted">N/A</em>}</td>
                          <td>{event.startDate}</td>
                          <td>{event.endDate}</td>
                          <td>
                            <Badge bg={isActive ? 'success' : isPast ? 'secondary' : 'info'}>
                              {isActive ? 'Active' : isPast ? 'Past' : 'Upcoming'}
                            </Badge>
                          </td>
                        </tr>
                      );
                    })
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Assign Event Modal (US 09) */}
      <Modal show={showAddModal} onHide={() => setShowAddModal(false)} centered>
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title><FaDoorOpen className="me-2" />Assign Room to Group/Event</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleAssignEvent}>
            <Form.Group className="mb-3" controlId="eventName">
              <Form.Label className="fw-bold">Event Name</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g., Annual Sports Meet, Freshman Orientation"
                value={newEvent.eventName}
                onChange={(e) => setNewEvent(prev => ({ ...prev, eventName: e.target.value }))}
                required
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="groupName">
              <Form.Label className="fw-bold">Group / Organization</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g., Football Team, Drama Club"
                value={newEvent.groupName}
                onChange={(e) => setNewEvent(prev => ({ ...prev, groupName: e.target.value }))}
              />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="eventStart">
                  <Form.Label className="fw-bold">Start Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.startDate}
                    onChange={(e) => setNewEvent(prev => ({ ...prev, startDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="eventEnd">
                  <Form.Label className="fw-bold">End Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={newEvent.endDate}
                    min={newEvent.startDate}
                    onChange={(e) => setNewEvent(prev => ({ ...prev, endDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
            <Button variant="primary" type="submit" className="w-100 fw-bold" disabled={submitting}>
              {submitting ? 'Assigning...' : 'Assign Room'}
            </Button>
          </Form>
        </Modal.Body>
      </Modal>
    </Container>
  );
}
