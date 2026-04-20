import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Badge, Button, Spinner, Alert,
  Form, Modal
} from 'react-bootstrap';
import {
  FaArrowLeft, FaBed, FaUsers, FaBuilding,
  FaWifi, FaBath, FaSnowflake, FaDoorOpen, FaCalendarAlt, FaCheckCircle
} from 'react-icons/fa';
import { roomApi } from '../../api/roomApi';
import { bookingApi } from '../../api/bookingApi';
import type { Room } from '../../types';
import toast from 'react-hot-toast';
import { TransformWrapper, TransformComponent } from 'react-zoom-pan-pinch';
import { FaSearchPlus, FaSearchMinus, FaExpand, FaIcons } from 'react-icons/fa';

const ROOM_TYPE_IMAGES: Record<string, string> = {
  'SINGLE': '/images/rooms/single_bed.jpg',
  'DOUBLE': '/images/rooms/double_bed.jpg',
  'TRIPLE': '/images/rooms/triple_bed.jpg',
  'SUITE': '/images/rooms/suite_bed.jpg',
  'DORMITORY': '/images/rooms/dormitory_bed.jpg',
};

const amenityIcons: Record<string, React.ReactNode> = {
  'WiFi': <FaWifi size={20} />,
  'Ensuite Bathroom': <FaBath size={20} />,
  'Air Conditioning': <FaSnowflake size={20} />,
  'Balcony': <FaBuilding size={20} />,
  'Kitchenette': <FaDoorOpen size={20} />,
};

export default function RoomDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [room, setRoom] = useState<Room | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Booking form state
  const [showBookingModal, setShowBookingModal] = useState(false);
  const [bookingForm, setBookingForm] = useState({
    startDate: '', endDate: '', occupants: 1, notes: ''
  });
  const [bookingLoading, setBookingLoading] = useState(false);
  const [bookingError, setBookingError] = useState('');

  useEffect(() => {
    loadRoom();
  }, [id]);

  const loadRoom = async () => {
    setLoading(true);
    try {
      const res = await roomApi.getRoomById(Number(id));
      setRoom(res.data);
    } catch {
      setError('Failed to load room details.');
    } finally {
      setLoading(false);
    }
  };

  const handleBookingSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBookingError('');

    if (!bookingForm.startDate) { setBookingError('Please provide a valid start date'); return; }
    if (!bookingForm.endDate) { setBookingError('Please provide a valid end date'); return; }
    if (bookingForm.startDate >= bookingForm.endDate) {
      setBookingError('End date must be after start date'); return;
    }
    if (bookingForm.occupants < 1) { setBookingError('Please provide a valid number of occupants'); return; }
    if (room && bookingForm.occupants > room.capacity) {
      setBookingError(`Occupants cannot exceed room capacity (${room.capacity})`); return;
    }

    setBookingLoading(true);
    try {
      const response = await bookingApi.createBooking({
        roomId: Number(id),
        startDate: bookingForm.startDate,
        endDate: bookingForm.endDate,
        occupants: bookingForm.occupants,
        notes: bookingForm.notes || undefined,
      });
      toast.success('Booking request submitted successfully!');
      setShowBookingModal(false);
      navigate(`/bookings/${response.data.id}`);
    } catch (err: any) {
      setBookingError(err.response?.data?.message || 'Failed to submit booking request.');
    } finally {
      setBookingLoading(false);
    }
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
        <p className="mt-2 text-muted">Loading room details...</p>
      </Container>
    );
  }

  if (error || !room) {
    return (
      <Container className="py-5">
        <Alert variant="danger">{error || 'Room not found'}</Alert>
        <Button variant="secondary" onClick={() => navigate('/rooms')}>
          <FaArrowLeft className="me-2" />Back to Search
        </Button>
      </Container>
    );
  }

  const statusVariant: Record<string, string> = {
    AVAILABLE: 'success', OCCUPIED: 'danger', MAINTENANCE: 'warning'
  };

  // Calculate nights for price display
  const nights = bookingForm.startDate && bookingForm.endDate
    ? Math.ceil((new Date(bookingForm.endDate).getTime() - new Date(bookingForm.startDate).getTime()) / (1000 * 60 * 60 * 24))
    : 0;

  // US 13: Calculate Dynamic Total Price
  const calculateTotalPrice = () => {
    if (!room || !bookingForm.startDate || !bookingForm.endDate || nights <= 0) {
      return 0;
    }

    let total = 0;
    const start = new Date(bookingForm.startDate);
    const end = new Date(bookingForm.endDate);
    
    // Iterate day by day to match backend logic
    for (let d = new Date(start); d < end; d.setDate(d.getDate() + 1)) {
      const dateStr = d.toISOString().split('T')[0];
      let dayMultiplier = 1.0;
      
      if (room.pricingTiers) {
        const activeTier = room.pricingTiers.find(t => 
          dateStr >= t.startDate && dateStr <= t.endDate
        );
        if (activeTier) {
          dayMultiplier = activeTier.priceMultiplier;
        }
      }
      
      
      total += room.basePriceWithAmenities * dayMultiplier;
    }
    
    return total;
  };

  const dynamicTotal = calculateTotalPrice();
  const isSeasonal = nights > 0 && dynamicTotal !== (room.basePriceWithAmenities * nights);

  return (
    <Container className="py-4">
      <Button variant="outline-secondary" className="mb-3" onClick={() => navigate('/rooms')}>
        <FaArrowLeft className="me-2" />Back to Search
      </Button>

      <Row>
        <Col lg={7}>
          {/* Room Image */}
          <Card className="border-0 shadow-sm mb-4 overflow-hidden position-relative">
            <TransformWrapper
              initialScale={1}
              minScale={1}
              maxScale={3}
              centerOnInit={true}
            >
              {({ zoomIn, zoomOut, resetTransform }) => (
                <>
                  <div className="position-absolute top-0 end-0 m-3 z-3 d-flex gap-1">
                    <Button variant="light" size="sm" className="shadow-sm border rounded-circle p-2" onClick={() => zoomIn()}>
                      <FaSearchPlus />
                    </Button>
                    <Button variant="light" size="sm" className="shadow-sm border rounded-circle p-2" onClick={() => zoomOut()}>
                      <FaSearchMinus />
                    </Button>
                    <Button variant="light" size="sm" className="shadow-sm border rounded-circle p-2" onClick={() => resetTransform()}>
                      <FaExpand />
                    </Button>
                  </div>
                  <TransformComponent wrapperStyle={{ width: '100%', height: '450px' }}>
                    <img
                      src={room.imagePath || ROOM_TYPE_IMAGES[room.roomType]}
                      alt={`Room ${room.roomNumber}`}
                      style={{
                        height: '450px',
                        width: '100%',
                        maxWidth: 'none',
                        objectFit: 'cover',
                        cursor: 'zoom-in'
                      }}
                      onError={(e) => {
                        const target = e.currentTarget as HTMLImageElement;
                        if (room.imagePath && target.src.includes(room.imagePath)) {
                          target.src = ROOM_TYPE_IMAGES[room.roomType];
                        } else {
                          target.style.display = 'none';
                          const parent = target.parentElement as HTMLElement;
                          parent.style.height = '400px';
                          parent.style.display = 'flex';
                          parent.style.alignItems = 'center';
                          parent.style.justifyContent = 'center';
                          parent.style.background = `linear-gradient(135deg, ${
                            room.roomType === 'SUITE' ? '#667eea, #764ba2' :
                            room.roomType === 'DOUBLE' ? '#f093fb, #f5576c' :
                            room.roomType === 'TRIPLE' ? '#4facfe, #00f2fe' :
                            room.roomType === 'DORMITORY' ? '#43e97b, #38f9d7' :
                            '#a18cd1, #fbc2eb'
                          })`;
                          const inner = document.createElement('div');
                          inner.className = 'text-center text-white';
                          inner.innerHTML = `
                            <span style="font-size: 5rem;">${
                              room.roomType === 'SUITE' ? '🏨' : room.roomType === 'DORMITORY' ? '🏢' : '🛏️'
                            }</span>
                            <h3 class="mt-2 fw-bold">Room ${room.roomNumber}</h3>
                          `;
                          parent.appendChild(inner);
                        }
                      }}
                    />
                  </TransformComponent>
                </>
              )}
            </TransformWrapper>
          </Card>

          {/* Description */}
          {room.description && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Body>
                <h5 className="fw-bold">About This Room</h5>
                <p className="text-muted mb-0">{room.description}</p>
              </Card.Body>
            </Card>
          )}

          {/* Amenities */}
          {room.amenities && room.amenities.length > 0 && (
            <Card className="border-0 shadow-sm mb-4">
              <Card.Body>
                <h5 className="fw-bold mb-3">Amenities</h5>
                <Row>
                  {room.amenities.map(amenity => (
                    <Col xs={6} md={4} key={amenity.id} className="mb-3">
                      <div className="d-flex align-items-center gap-2 p-2 rounded bg-light">
                        <span className="text-primary">
                          {amenityIcons[amenity.name] || <FaCheckCircle size={20} />}
                        </span>
                        <div>
                          <strong className="small">{amenity.name}</strong>
                          {amenity.description && (
                            <small className="d-block text-muted">{amenity.description}</small>
                          )}
                        </div>
                      </div>
                    </Col>
                  ))}
                </Row>
              </Card.Body>
            </Card>
          )}

          {/* Floor Plan (US 16) */}
          <Card className="border-0 shadow-sm mb-4 overflow-hidden position-relative">
            <Card.Body>
              <h5 className="fw-bold mb-3">Floor Plan</h5>
              <TransformWrapper
                initialScale={1}
                minScale={1}
                maxScale={4}
                centerOnInit={true}
              >
                {({ zoomIn, zoomOut, resetTransform }) => (
                  <div className="bg-light rounded p-0 text-center position-relative overflow-hidden" style={{ minHeight: '300px' }}>
                    <div className="position-absolute top-0 end-0 m-2 z-3 d-flex flex-column gap-1">
                      <Button variant="dark" size="sm" className="bg-opacity-50 border-0 p-2" onClick={() => zoomIn()}>
                        <FaSearchPlus />
                      </Button>
                      <Button variant="dark" size="sm" className="bg-opacity-50 border-0 p-2" onClick={() => zoomOut()}>
                        <FaSearchMinus />
                      </Button>
                      <Button variant="dark" size="sm" className="bg-opacity-50 border-0 p-2" onClick={() => resetTransform()}>
                        <FaExpand />
                      </Button>
                    </div>
                    <TransformComponent wrapperStyle={{ width: '100%', height: '100%', display: 'block' }}>
                      <img 
                        src={room.floorPlanPath || `/images/floors/${room.floor}${room.floor === 1 ? 'st' : room.floor === 2 ? 'nd' : room.floor === 3 ? 'rd' : 'th'}_floor.png`}
                        alt={`Floor ${room.floor} Plan`}
                        className="img-fluid"
                        style={{ cursor: 'move', width: '100%', objectFit: 'contain', maxHeight: '500px' }}
                        onError={(e) => {
                          const target = e.currentTarget as HTMLImageElement;
                          if (!target.src.includes('placeholder_floor.png')) {
                            target.src = '/images/floors/placeholder_floor.png';
                          } else {
                            target.style.display = 'none';
                          }
                        }}
                      />
                    </TransformComponent>
                  </div>
                )}
              </TransformWrapper>
              <p className="mt-2 text-muted small text-center mb-0">Drag to pan • Pinch or use icons to zoom</p>
            </Card.Body>
          </Card>
        </Col>

        {/* Room Info Sidebar */}
        <Col lg={5}>
          <Card className="border-0 shadow-sm sticky-top" style={{ top: '80px' }}>
            <Card.Body>
              <div className="d-flex justify-content-between align-items-start mb-3">
                <div>
                  <h3 className="fw-bold mb-1">Room {room.roomNumber}</h3>
                  <Badge bg={statusVariant[room.status] || 'secondary'} className="fs-6">
                    {room.status}
                  </Badge>
                </div>
                <div className="text-end">
                  <h3 className="text-primary fw-bold mb-0">₹{room.currentPrice}</h3>
                  <div className="d-flex flex-column align-items-end">
                    <small className="text-muted">per night</small>
                    {room.currentPrice !== room.basePriceWithAmenities && (
                      <div className="text-muted text-decoration-line-through small">₹{room.basePriceWithAmenities}</div>
                    )}
                    {/* <Badge bg="light" text="dark" className="mt-1 border fw-normal" style={{ fontSize: '0.7rem' }}> */}
                      {/* Base + Amenities: ₹{room.basePriceWithAmenities} */}
                    {/* </Badge> */}
                  </div>
                </div>
              </div>

              <hr />

              <div className="mb-3">
                <div className="d-flex align-items-center mb-2">
                  <FaBed className="text-primary me-3" size={20} />
                  <div>
                    <small className="text-muted d-block">Room Type</small>
                    <strong>{room.roomType.charAt(0) + room.roomType.slice(1).toLowerCase()}</strong>
                  </div>
                </div>
                <div className="d-flex align-items-center mb-2">
                  <FaUsers className="text-primary me-3" size={20} />
                  <div>
                    <small className="text-muted d-block">Capacity</small>
                    <strong>{room.capacity} {room.capacity > 1 ? 'persons' : 'person'}</strong>
                  </div>
                </div>
                <div className="d-flex align-items-center mb-2">
                  <FaBuilding className="text-primary me-3" size={20} />
                  <div>
                    <small className="text-muted d-block">Floor</small>
                    <strong>Floor {room.floor}</strong>
                  </div>
                </div>
              </div>

              <hr />

              {room.status === 'AVAILABLE' ? (
                <Button
                  variant="primary"
                  size="lg"
                  className="w-100 fw-bold"
                  onClick={() => setShowBookingModal(true)}
                >
                  <FaCalendarAlt className="me-2" />Request This Room
                </Button>
              ) : (
                <Button variant="secondary" size="lg" className="w-100" disabled>
                  Not Available
                </Button>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Booking Request Modal (US 03) */}
      <Modal show={showBookingModal} onHide={() => setShowBookingModal(false)} centered size="lg">
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title><FaCalendarAlt className="me-2" />Room Booking Request</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {bookingError && <Alert variant="danger">{bookingError}</Alert>}

          <div className="bg-light rounded p-3 mb-3">
            <div className="d-flex justify-content-between align-items-center">
              <div>
                <strong>Room {room.roomNumber}</strong>
                <span className="text-muted ms-2">({room.roomType})</span>
              </div>
              <div className="text-end">
                <strong className="text-primary d-block">₹{room.currentPrice}/night</strong>
              </div>
            </div>
          </div>

          <Form onSubmit={handleBookingSubmit}>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="bookingStartDate">
                  <Form.Label className="fw-bold">Check-in Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={bookingForm.startDate}
                    min={new Date().toISOString().split('T')[0]}
                    onChange={(e) => setBookingForm(prev => ({ ...prev, startDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="bookingEndDate">
                  <Form.Label className="fw-bold">Check-out Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={bookingForm.endDate}
                    min={bookingForm.startDate || new Date().toISOString().split('T')[0]}
                    onChange={(e) => setBookingForm(prev => ({ ...prev, endDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>

            <Form.Group className="mb-3" controlId="bookingOccupants">
              <Form.Label className="fw-bold">Number of Occupants</Form.Label>
              <Form.Control
                type="number"
                min={1}
                max={room.capacity}
                value={bookingForm.occupants}
                onChange={(e) => setBookingForm(prev => ({ ...prev, occupants: parseInt(e.target.value) || 1 }))}
                required
              />
              <Form.Text className="text-muted">Max capacity: {room.capacity}</Form.Text>
            </Form.Group>

            <Form.Group className="mb-3" controlId="bookingNotes">
              <Form.Label className="fw-bold">Notes (Optional)</Form.Label>
              <Form.Control
                as="textarea"
                rows={2}
                placeholder="Any special requests..."
                value={bookingForm.notes}
                onChange={(e) => setBookingForm(prev => ({ ...prev, notes: e.target.value }))}
              />
            </Form.Group>

            {nights > 0 && (
              <div className="bg-light rounded p-3 mb-3 border border-primary border-opacity-25">
                <div className="d-flex justify-content-between align-items-center">
                  <div>
                    <span>₹{room.basePriceWithAmenities} × {nights} night{nights > 1 ? 's' : ''}</span>
                    {isSeasonal && (
                      <Badge bg="warning" text="dark" className="d-block mt-1">
                        Seasonal Rate Applied
                      </Badge>
                    )}
                  </div>
                  <div className="text-end">
                    <strong className="text-primary fs-4">₹{dynamicTotal.toFixed(2)}</strong>
                  </div>
                </div>
              </div>
            )}

            <Button
              variant="primary"
              type="submit"
              size="lg"
              className="w-100 fw-bold"
              disabled={bookingLoading}
            >
              {bookingLoading ? (
                <><Spinner animation="border" size="sm" className="me-2" />Submitting...</>
              ) : (
                'Submit Booking Request'
              )}
            </Button>
          </Form>
        </Modal.Body>
      </Modal>
    </Container>
  );
}
