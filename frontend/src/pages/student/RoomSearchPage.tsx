import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Form, Button, Badge, Spinner, Alert
} from 'react-bootstrap';
import {
  FaSearch, FaFilter, FaBed, FaUsers, FaMoneyBillWave, FaWifi, FaBath,
  FaSnowflake, FaBuilding, FaRedo, FaDoorOpen
} from 'react-icons/fa';
import { roomApi } from '../../api/roomApi';
import type { Room, Amenity, RoomSearchCriteria } from '../../types';

const ROOM_TYPES = ['SINGLE', 'DOUBLE', 'TRIPLE', 'SUITE', 'DORMITORY'];

const ROOM_TYPE_IMAGES: Record<string, string> = {
  'SINGLE': '/images/rooms/single_bed.jpg',
  'DOUBLE': '/images/rooms/double_bed.jpg',
  'TRIPLE': '/images/rooms/triple_bed.jpg',
  'SUITE': '/images/rooms/suite_bed.jpg',
  'DORMITORY': '/images/rooms/dormitory_bed.jpg',
};

const amenityIconMap: Record<string, React.ReactNode> = {
  'WiFi': <FaWifi />,
  'Ensuite Bathroom': <FaBath />,
  'Air Conditioning': <FaSnowflake />,
  'Balcony': <FaBuilding />,
  'Kitchenette': <FaDoorOpen />,
};

export default function RoomSearchPage() {
  const [rooms, setRooms] = useState<Room[]>([]);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [criteria, setCriteria] = useState<RoomSearchCriteria>({ status: 'AVAILABLE' });
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    loadAmenities();
    searchRooms();
  }, []);

  const loadAmenities = async () => {
    try {
      const res = await roomApi.getAllAmenities();
      setAmenities(res.data);
    } catch { /* ignore */ }
  };

  const searchRooms = async (searchCriteria?: RoomSearchCriteria) => {
    setLoading(true);
    setError('');
    try {
      const c = searchCriteria || { ...criteria, amenities: selectedAmenities.length > 0 ? selectedAmenities : undefined };
      const res = await roomApi.searchRooms(c);
      setRooms(res.data);
    } catch (err: any) {
      setError('Failed to load rooms. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    searchRooms({ ...criteria, amenities: selectedAmenities.length > 0 ? selectedAmenities : undefined });
  };

  const handleReset = () => {
    setCriteria({ status: 'AVAILABLE' });
    setSelectedAmenities([]);
    searchRooms({ status: 'AVAILABLE' });
  };

  const toggleAmenity = (name: string) => {
    setSelectedAmenities(prev =>
      prev.includes(name) ? prev.filter(a => a !== name) : [...prev, name]
    );
  };

  const getStatusBadge = (status: string) => {
    const variants: Record<string, string> = {
      AVAILABLE: 'success', OCCUPIED: 'danger', MAINTENANCE: 'warning'
    };
    return <Badge bg={variants[status] || 'secondary'}>{status}</Badge>;
  };

  const getRoomTypeIcon = (type: string) => {
    const icons: Record<string, string> = {
      SINGLE: '🛏️', DOUBLE: '🛏️🛏️', TRIPLE: '🛏️🛏️🛏️', SUITE: '🏨', DORMITORY: '🏢'
    };
    return icons[type] || '🏠';
  };

  return (
    <Container fluid className="py-4">
      <Row>
        {/* Filter Sidebar */}
        <Col lg={3}>
          <Card className="shadow-sm border-0 mb-4">
            <Card.Header className="bg-primary text-white d-flex align-items-center">
              <FaFilter className="me-2" />
              <strong>Filters</strong>
            </Card.Header>
            <Card.Body>
              <Form onSubmit={handleSearch}>
                <Form.Group className="mb-3" controlId="filterRoomType">
                  <Form.Label className="fw-bold"><FaBed className="me-2" />Room Type</Form.Label>
                  <Form.Select
                    value={criteria.roomType || ''}
                    onChange={(e) => setCriteria(prev => ({ ...prev, roomType: e.target.value || undefined }))}
                  >
                    <option value="">All Types</option>
                    {ROOM_TYPES.map(t => (
                      <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>
                    ))}
                  </Form.Select>
                </Form.Group>

                <Form.Group className="mb-3" controlId="filterFloor">
                  <Form.Label className="fw-bold"><FaBuilding className="me-2" />Floor</Form.Label>
                  <Form.Control
                    type="number"
                    placeholder="Any floor"
                    min={1}
                    value={criteria.floor || ''}
                    onChange={(e) => setCriteria(prev => ({
                      ...prev, floor: e.target.value ? parseInt(e.target.value) : undefined
                    }))}
                  />
                </Form.Group>

                <Form.Label className="fw-bold"><FaMoneyBillWave className="me-2" />Price Range (₹/night)</Form.Label>
                <Row className="mb-3">
                  <Col>
                    <Form.Control
                      type="number"
                      placeholder="Min"
                      min={0}
                      value={criteria.minPrice || ''}
                      onChange={(e) => setCriteria(prev => ({
                        ...prev, minPrice: e.target.value ? parseFloat(e.target.value) : undefined
                      }))}
                    />
                  </Col>
                  <Col>
                    <Form.Control
                      type="number"
                      placeholder="Max"
                      min={0}
                      value={criteria.maxPrice || ''}
                      onChange={(e) => setCriteria(prev => ({
                        ...prev, maxPrice: e.target.value ? parseFloat(e.target.value) : undefined
                      }))}
                    />
                  </Col>
                </Row>

                <Form.Label className="fw-bold mb-2">Amenities</Form.Label>
                <div className="mb-3">
                  {amenities.map(a => (
                    <Form.Check
                      key={a.id}
                      type="checkbox"
                      id={`amenity-${a.id}`}
                      label={
                        <span className="d-flex align-items-center gap-1">
                          {amenityIconMap[a.name] || '✓'} {a.name}
                        </span>
                      }
                      checked={selectedAmenities.includes(a.name)}
                      onChange={() => toggleAmenity(a.name)}
                      className="mb-1"
                    />
                  ))}
                </div>

                <div className="d-grid gap-2">
                  <Button variant="primary" type="submit" disabled={loading}>
                    <FaSearch className="me-2" />Search
                  </Button>
                  <Button variant="outline-secondary" onClick={handleReset}>
                    <FaRedo className="me-2" />Reset Filters
                  </Button>
                </div>
              </Form>
            </Card.Body>
          </Card>
        </Col>

        {/* Room Results */}
        <Col lg={9}>
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h4 className="fw-bold mb-0">
              <FaSearch className="me-2 text-primary" />Available Rooms
            </h4>
            <Badge bg="secondary" className="fs-6">{rooms.length} rooms found</Badge>
          </div>

          {error && <Alert variant="danger">{error}</Alert>}

          {loading ? (
            <div className="text-center py-5">
              <Spinner animation="border" variant="primary" />
              <p className="mt-2 text-muted">Searching rooms...</p>
            </div>
          ) : rooms.length === 0 ? (
            <Card className="text-center py-5 border-0 shadow-sm">
              <Card.Body>
                <FaBed size={48} className="text-muted mb-3" />
                <h5 className="text-muted">No rooms match your criteria</h5>
                <p className="text-muted">Try adjusting your filters</p>
              </Card.Body>
            </Card>
          ) : (
            <Row>
              {rooms.map(room => (
                <Col md={6} xl={4} key={room.id} className="mb-4">
                  <Card
                    className="h-100 shadow-sm border-0 room-card"
                    style={{ cursor: 'pointer', transition: 'transform 0.2s, box-shadow 0.2s' }}
                    onClick={() => navigate(`/rooms/${room.id}`)}
                    onMouseEnter={(e) => {
                      (e.currentTarget as HTMLElement).style.transform = 'translateY(-4px)';
                      (e.currentTarget as HTMLElement).style.boxShadow = '0 8px 25px rgba(0,0,0,0.15)';
                    }}
                    onMouseLeave={(e) => {
                      (e.currentTarget as HTMLElement).style.transform = 'translateY(0)';
                      (e.currentTarget as HTMLElement).style.boxShadow = '';
                    }}
                  >
                    <div
                      className="card-img-top position-relative overflow-hidden"
                      style={{ height: '200px' }}
                    >
                      <img
                        src={room.imagePath || ROOM_TYPE_IMAGES[room.roomType]}
                        alt={`Room ${room.roomNumber}`}
                        className="w-100 h-100 object-fit-cover transition-transform"
                        onError={(e) => {
                          const target = e.currentTarget as HTMLImageElement;
                          // If current src is imagePath, try ROOM_TYPE_IMAGES fallback
                          if (room.imagePath && target.src.includes(room.imagePath)) {
                            target.src = ROOM_TYPE_IMAGES[room.roomType];
                          } else {
                            // Ultimate fallback to icons/gradients
                            target.style.display = 'none';
                            const parent = target.parentElement as HTMLElement;
                            parent.style.background = `linear-gradient(135deg, ${
                              room.roomType === 'SUITE' ? '#667eea, #764ba2' :
                              room.roomType === 'DOUBLE' ? '#f093fb, #f5576c' :
                              room.roomType === 'TRIPLE' ? '#4facfe, #00f2fe' :
                              room.roomType === 'DORMITORY' ? '#43e97b, #38f9d7' :
                              '#a18cd1, #fbc2eb'
                            })`;
                            const span = document.createElement('span');
                            span.style.fontSize = '3rem';
                            span.innerHTML = getRoomTypeIcon(room.roomType);
                            parent.appendChild(span);
                          }
                        }}
                      />
                    </div>
                    <Card.Body>
                      <div className="d-flex justify-content-between align-items-start mb-2">
                        <div>
                          <h5 className="fw-bold mb-0">Room {room.roomNumber}</h5>
                          <small className="text-muted">{room.roomType} • Floor {room.floor}</small>
                        </div>
                        {getStatusBadge(room.status)}
                      </div>

                      <div className="d-flex gap-3 mb-2 text-muted small">
                        <span><FaUsers className="me-1" />{room.capacity} {room.capacity > 1 ? 'persons' : 'person'}</span>
                      </div>

                      {room.amenities && room.amenities.length > 0 && (
                        <div className="mb-2">
                          {room.amenities.slice(0, 3).map(a => (
                            <Badge key={a.id} bg="light" text="dark" className="me-1 mb-1 border">
                              {amenityIconMap[a.name] || '✓'} {a.name}
                            </Badge>
                          ))}
                          {room.amenities.length > 3 && (
                            <Badge bg="light" text="dark" className="mb-1 border">
                              +{room.amenities.length - 3} more
                            </Badge>
                          )}
                        </div>
                      )}

                      <div className="d-flex justify-content-between align-items-center mt-auto">
                        <div>
                          <h5 className="text-primary fw-bold mb-0">₹{room.currentPrice}</h5>
                          <small className="text-muted">/night</small>
                        </div>
                        {room.currentPrice !== room.pricePerNight && (
                          <Badge bg="warning" text="dark" style={{fontSize: '0.65rem'}}>Seasonal Rate</Badge>
                        )}
                      </div>
                    </Card.Body>
                  </Card>
                </Col>
              ))}
            </Row>
          )}
        </Col>
      </Row>
    </Container>
  );
}
