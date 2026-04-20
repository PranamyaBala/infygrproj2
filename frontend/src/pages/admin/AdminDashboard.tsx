import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Table, Badge, Button, Form,
  Spinner, Modal
} from 'react-bootstrap';
import {
  FaTachometerAlt, FaBed, FaUsers, FaCheckCircle,
  FaWrench, FaPlus, FaDoorOpen
} from 'react-icons/fa';
import { roomApi } from '../../api/roomApi';
import type { Room, Amenity, CreateRoomRequest, UpdateRoomStatusRequest } from '../../types';
import toast from 'react-hot-toast';

const ROOM_TYPES = ['SINGLE', 'DOUBLE', 'TRIPLE', 'SUITE', 'DORMITORY'];

const ROOM_TYPE_PRICES: Record<string, number> = {
  'SINGLE': 100,
  'DOUBLE': 200,
  'TRIPLE': 300,
  'SUITE': 1000,
  'DORMITORY': 50
};

const ROOM_TYPE_CAPACITIES: Record<string, number> = {
  'SINGLE': 1,
  'DOUBLE': 2,
  'TRIPLE': 3,
  'SUITE': 4,
  'DORMITORY': 6
};

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [rooms, setRooms] = useState<Room[]>([]);
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('');

  // Add Room Modal
  const [showAddRoom, setShowAddRoom] = useState(false);
  const [newRoom, setNewRoom] = useState<CreateRoomRequest>({
    roomNumber: '', roomType: 'SINGLE', floor: 1, capacity: 1,
    pricePerNight: 100, description: '', imagePath: '', amenityIds: []
  });
  const [addingRoom, setAddingRoom] = useState(false);

  // Edit Room Modal
  const [showEditRoom, setShowEditRoom] = useState(false);
  const [editingRoomId, setEditingRoomId] = useState<number | null>(null);
  const [editRoomData, setEditRoomData] = useState<CreateRoomRequest>({
    roomNumber: '', roomType: 'SINGLE', floor: 1, capacity: 1,
    pricePerNight: 100, description: '', imagePath: '', amenityIds: []
  });
  const [updatingRoom, setUpdatingRoom] = useState(false);

  // Status Update Modal
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedRoom, setSelectedRoom] = useState<Room | null>(null);
  const [statusUpdate, setStatusUpdate] = useState<UpdateRoomStatusRequest>({ status: '' });

  useEffect(() => {
    loadData();
  }, []);

  // Auto-calculate price based on type and amenities
  useEffect(() => {
    const basePrice = ROOM_TYPE_PRICES[newRoom.roomType] || 0;
    setNewRoom(prev => ({ ...prev, pricePerNight: basePrice }));
  }, [newRoom.roomType, amenities]);

  const calculateTotal = (roomData: CreateRoomRequest) => {
    return (roomData.pricePerNight || 0) + (roomData.amenityIds || []).reduce((sum, id) => {
      const amenity = amenities.find(a => a.id === id);
      return sum + (amenity?.price || 0);
    }, 0);
  };

  const calculatedTotal = calculateTotal(newRoom);
  const calculatedEditTotal = calculateTotal(editRoomData);

  // Auto-calculate price based on type (Edit)
  useEffect(() => {
    const basePrice = ROOM_TYPE_PRICES[editRoomData.roomType] || 0;
    setEditRoomData(prev => ({ ...prev, pricePerNight: basePrice }));
  }, [editRoomData.roomType, amenities]);

  // Auto-set capacity based on room type
  useEffect(() => {
    const capacity = ROOM_TYPE_CAPACITIES[newRoom.roomType] || 1;
    setNewRoom(prev => ({ ...prev, capacity }));
  }, [newRoom.roomType]);

  // Auto-set capacity based on room type (Edit)
  useEffect(() => {
    const capacity = ROOM_TYPE_CAPACITIES[editRoomData.roomType] || 1;
    setEditRoomData(prev => ({ ...prev, capacity }));
  }, [editRoomData.roomType]);

  // Auto-infer floor from room number (e.g., 101 -> floor 1)
  useEffect(() => {
    if (newRoom.roomNumber && newRoom.roomNumber.length >= 1) {
      const firstDigit = parseInt(newRoom.roomNumber.charAt(0));
      if (!isNaN(firstDigit) && firstDigit > 0) {
        setNewRoom(prev => ({ ...prev, floor: firstDigit }));
      }
    }
  }, [newRoom.roomNumber]);

  // Auto-infer floor from room number (Edit)
  useEffect(() => {
    if (editRoomData.roomNumber && editRoomData.roomNumber.length >= 1) {
      const firstDigit = parseInt(editRoomData.roomNumber.charAt(0));
      if (!isNaN(firstDigit) && firstDigit > 0) {
        setEditRoomData(prev => ({ ...prev, floor: firstDigit }));
      }
    }
  }, [editRoomData.roomNumber]);

  const loadData = async () => {
    try {
      const [roomRes, amenityRes] = await Promise.all([
        roomApi.getAllRooms(),
        roomApi.getAllAmenities(),
      ]);
      setRooms(roomRes.data);
      setAmenities(amenityRes.data);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleAddRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    setAddingRoom(true);
    if (!newRoom.amenityIds || newRoom.amenityIds.length < 3) {
      toast.error('Please select at least 3 amenities.');
      setAddingRoom(false);
      return;
    }
    try {
      await roomApi.createRoom(newRoom);
      toast.success('Room created successfully!');
      setShowAddRoom(false);
      loadData();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to create room.');
    } finally {
      setAddingRoom(false);
    }
  };
  const handleEditRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editingRoomId === null) return;
    setUpdatingRoom(true);
    if (!editRoomData.amenityIds || editRoomData.amenityIds.length < 3) {
      toast.error('SRS Requirement: Please select at least 3 amenities.');
      setUpdatingRoom(false);
      return;
    }
    try {
      await roomApi.updateRoom(editingRoomId, editRoomData);
      toast.success('Room updated successfully!');
      setShowEditRoom(false);
      loadData();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update room.');
    } finally {
      setUpdatingRoom(false);
    }
  };

  const openEditModal = (room: Room) => {
    setEditingRoomId(room.id);
    setEditRoomData({
      roomNumber: room.roomNumber,
      roomType: room.roomType,
      floor: room.floor,
      capacity: room.capacity,
      pricePerNight: room.pricePerNight,
      description: room.description || '',
      imagePath: room.imagePath || '',
      amenityIds: room.amenities?.map(a => a.id) || []
    });
    setShowEditRoom(true);
  };
  const handleStatusUpdate = async () => {
    if (!selectedRoom) return;
    try {
      await roomApi.updateRoomStatus(selectedRoom.id, statusUpdate);
      toast.success(`Room ${selectedRoom.roomNumber} status updated!`);
      setShowStatusModal(false);
      loadData();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to update status.');
    }
  };

  const openStatusModal = (room: Room) => {
    setSelectedRoom(room);
    setStatusUpdate({ 
      status: room.status,
      maintenanceStartDate: room.maintenanceStartDate,
      maintenanceEndDate: room.maintenanceEndDate,
      occupiedStartDate: room.occupiedStartDate,
      occupiedEndDate: room.occupiedEndDate
    });
    setShowStatusModal(true);
  };

  const filteredRooms = statusFilter
    ? rooms.filter(r => r.status === statusFilter)
    : rooms;

  const stats = {
    total: rooms.length,
    available: rooms.filter(r => r.status === 'AVAILABLE').length,
    occupied: rooms.filter(r => r.status === 'OCCUPIED').length,
    maintenance: rooms.filter(r => r.status === 'MAINTENANCE').length,
  };

  const getStatusBadge = (status: string) => {
    const v: Record<string, string> = { AVAILABLE: 'success', OCCUPIED: 'danger', MAINTENANCE: 'warning' };
    return <Badge bg={v[status] || 'secondary'}>{status}</Badge>;
  };

  if (loading) {
    return <Container className="py-5 text-center"><Spinner animation="border" variant="primary" /></Container>;
  }

  return (
    <Container fluid className="py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h3 className="fw-bold mb-0"><FaTachometerAlt className="me-2 text-primary" />Admin Dashboard</h3>
        <Button variant="primary" onClick={() => setShowAddRoom(true)}>
          <FaPlus className="me-2" />Add New Room
        </Button>
      </div>

      {/* Stats Cards */}
      <Row className="mb-4">
        {[
          { label: 'Total Rooms', value: stats.total, icon: <FaBed />, bg: 'primary' },
          { label: 'Available', value: stats.available, icon: <FaCheckCircle />, bg: 'success' },
          { label: 'Occupied', value: stats.occupied, icon: <FaUsers />, bg: 'danger' },
          { label: 'Maintenance', value: stats.maintenance, icon: <FaWrench />, bg: 'warning' },
        ].map((stat, i) => (
          <Col sm={6} xl={3} key={i} className="mb-3">
            <Card className={`border-0 shadow-sm bg-${stat.bg} bg-opacity-10`}>
              <Card.Body className="d-flex align-items-center gap-3">
                <div className={`rounded-circle bg-${stat.bg} bg-opacity-25 p-3 text-${stat.bg}`}>
                  {stat.icon}
                </div>
                <div>
                  <h3 className="fw-bold mb-0">{stat.value}</h3>
                  <small className="text-muted">{stat.label}</small>
                </div>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>

      {/* Room Table */}
      <Card className="border-0 shadow-sm">
        <Card.Header className="bg-white d-flex justify-content-between align-items-center">
          <h5 className="mb-0 fw-bold"><FaDoorOpen className="me-2" />Room Management</h5>
          <Form.Select
            style={{ width: '200px' }}
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="">All Statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="OCCUPIED">Occupied</option>
            <option value="MAINTENANCE">Maintenance</option>
          </Form.Select>
        </Card.Header>
        <Card.Body className="p-0">
          <Table responsive hover className="mb-0">
            <thead className="table-light">
              <tr>
                <th>Room #</th>
                <th>Type</th>
                <th>Floor</th>
                <th>Capacity</th>
                <th>Price/Night</th>
                <th>Status</th>
                <th>Amenities</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {filteredRooms.map(room => (
                <tr key={room.id}>
                  <td className="fw-bold">{room.roomNumber}</td>
                  <td>{room.roomType}</td>
                  <td>{room.floor}</td>
                  <td>{room.capacity}</td>
                  <td>
                    <div className="fw-bold text-primary">
                      ₹{room.currentPrice}
                      {room.currentPrice !== room.basePriceWithAmenities && (
                        <small className="text-muted text-decoration-line-through ms-2 fw-normal">
                          ₹{room.basePriceWithAmenities}
                        </small>
                      )}
                    </div>
                  </td>
                  <td>{getStatusBadge(room.status)}</td>
                  <td>
                    {room.amenities?.slice(0, 2).map(a => (
                      <Badge key={a.id} bg="light" text="dark" className="me-1 border">
                        {a.name}
                      </Badge>
                    ))}
                    {room.amenities && room.amenities.length > 2 && (
                      <Badge bg="light" text="dark" className="border">+{room.amenities.length - 2}</Badge>
                    )}
                  </td>
                  <td>
                    <div className="d-flex gap-2">
                      <Button variant="outline-warning" size="sm" onClick={() => openEditModal(room)}>
                        Edit
                      </Button>
                      <Button variant="outline-primary" size="sm" onClick={() => openStatusModal(room)}>
                        Status
                      </Button>
                      <Button 
                        variant="outline-info" size="sm" 
                        onClick={() => navigate(`/admin/rooms/${room.id}/pricing`)}
                      >
                        Pricing
                      </Button>
                      <Button 
                        variant="outline-secondary" size="sm" 
                        onClick={() => navigate(`/admin/rooms/${room.id}/events`)}
                      >
                        Events
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        </Card.Body>
      </Card>

      {/* Add Room Modal */}
      <Modal show={showAddRoom} onHide={() => setShowAddRoom(false)} size="lg" centered>
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title><FaPlus className="me-2" />Add New Room</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleAddRoom}>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="newRoomNumber">
                  <Form.Label className="fw-bold">Room Number</Form.Label>
                  <Form.Control
                    type="text"
                    value={newRoom.roomNumber}
                    onChange={(e) => setNewRoom(prev => ({ ...prev, roomNumber: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="newRoomType">
                  <Form.Label className="fw-bold">Room Type</Form.Label>
                  <Form.Select
                    value={newRoom.roomType}
                    onChange={(e) => setNewRoom(prev => ({ ...prev, roomType: e.target.value }))}
                  >
                    {ROOM_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="newRoomFloor">
                  <Form.Label className="fw-bold">Floor</Form.Label>
                  <Form.Control
                    type="number" min={1}
                    value={newRoom.floor}
                    onChange={(e) => setNewRoom(prev => ({ ...prev, floor: parseInt(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="newRoomCapacity">
                  <Form.Label className="fw-bold">Capacity</Form.Label>
                  <Form.Control
                    type="number" min={1}
                    value={newRoom.capacity}
                    onChange={(e) => setNewRoom(prev => ({ ...prev, capacity: parseInt(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="newRoomPrice">
                  <Form.Label className="fw-bold">Price/Night (₹)</Form.Label>
                  <Form.Control
                    type="number" min={0} step="0.01"
                    value={newRoom.pricePerNight}
                    onChange={(e) => setNewRoom(prev => ({ ...prev, pricePerNight: parseFloat(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3" controlId="newRoomDescription">
              <Form.Label className="fw-bold">Description</Form.Label>
              <Form.Control
                as="textarea" rows={2}
                value={newRoom.description}
                onChange={(e) => setNewRoom(prev => ({ ...prev, description: e.target.value }))}
              />
            </Form.Group>
            <Form.Group className="mb-3" controlId="newRoomImage">
              <Form.Label className="fw-bold">Image Path</Form.Label>
              <Form.Control
                type="text"
                placeholder="/images/rooms/your-photo.jpg"
                value={newRoom.imagePath}
                onChange={(e) => setNewRoom(prev => ({ ...prev, imagePath: e.target.value }))}
              />
              <Form.Text className="text-muted">
                Place images in <code>frontend/public/images/rooms/</code>
              </Form.Text>
            </Form.Group>
            <Form.Label className="fw-bold mb-2">Amenities</Form.Label>
            <div className="mb-3">
              {amenities.map(a => (
                <Form.Check
                  key={a.id} type="checkbox" inline
                  id={`new-amenity-${a.id}`} label={a.name}
                  checked={newRoom.amenityIds?.includes(a.id) || false}
                  onChange={(e) => {
                    setNewRoom(prev => ({
                      ...prev,
                      amenityIds: e.target.checked
                        ? [...(prev.amenityIds || []), a.id]
                        : (prev.amenityIds || []).filter(id => id !== a.id)
                    }));
                  }}
                />
              ))}
            </div>
            
            <div className="bg-primary bg-opacity-10 p-3 rounded border border-primary border-opacity-25 mb-4">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h6 className="fw-bold mb-0 text-primary">Estimated Total Price</h6>
                  <small className="text-muted">Room Base + Selected Amenities</small>
                </div>
                <h4 className="fw-bold mb-0 text-primary">₹{calculatedTotal.toFixed(2)}</h4>
              </div>
            </div>
            <Button variant="primary" type="submit" disabled={addingRoom} className="w-100">
              {addingRoom ? <Spinner animation="border" size="sm" /> : 'Create Room'}
            </Button>
          </Form>
        </Modal.Body>
      </Modal>

      {/* Edit Room Modal */}
      <Modal show={showEditRoom} onHide={() => setShowEditRoom(false)} centered size="lg">
        <Modal.Header closeButton className="bg-warning text-dark">
          <Modal.Title><FaWrench className="me-2" />Edit Room Details</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleEditRoom}>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="editRoomNumber">
                  <Form.Label className="fw-bold">Room Number</Form.Label>
                  <Form.Control
                    type="text"
                    value={editRoomData.roomNumber}
                    onChange={(e) => setEditRoomData(prev => ({ ...prev, roomNumber: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="editRoomType">
                  <Form.Label className="fw-bold">Room Type</Form.Label>
                  <Form.Select
                    value={editRoomData.roomType}
                    onChange={(e) => setEditRoomData(prev => ({ ...prev, roomType: e.target.value }))}
                  >
                    {ROOM_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                  </Form.Select>
                </Form.Group>
              </Col>
            </Row>
            <Row>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="editRoomFloor">
                  <Form.Label className="fw-bold">Floor</Form.Label>
                  <Form.Control
                    type="number" min={1}
                    value={editRoomData.floor}
                    onChange={(e) => setEditRoomData(prev => ({ ...prev, floor: parseInt(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="editRoomCapacity">
                  <Form.Label className="fw-bold">Capacity</Form.Label>
                  <Form.Control
                    type="number" min={1}
                    value={editRoomData.capacity}
                    onChange={(e) => setEditRoomData(prev => ({ ...prev, capacity: parseInt(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group className="mb-3" controlId="editRoomPrice">
                  <Form.Label className="fw-bold">Price/Night (₹)</Form.Label>
                  <Form.Control
                    type="number" min={0} step="0.01"
                    value={editRoomData.pricePerNight}
                    onChange={(e) => setEditRoomData(prev => ({ ...prev, pricePerNight: parseFloat(e.target.value) }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3" controlId="editRoomDescription">
              <Form.Label className="fw-bold">Description</Form.Label>
              <Form.Control
                as="textarea" rows={2}
                value={editRoomData.description}
                onChange={(e) => setEditRoomData(prev => ({ ...prev, description: e.target.value }))}
              />
            </Form.Group>
            <Form.Label className="fw-bold mb-2">Amenities</Form.Label>
            <div className="mb-3">
              {amenities.map(a => (
                <Form.Check
                  key={a.id} type="checkbox" inline
                  id={`edit-amenity-${a.id}`} label={a.name}
                  checked={editRoomData.amenityIds?.includes(a.id) || false}
                  onChange={(e) => {
                    setEditRoomData(prev => ({
                      ...prev,
                      amenityIds: e.target.checked
                        ? [...(prev.amenityIds || []), a.id]
                        : (prev.amenityIds || []).filter(id => id !== a.id)
                    }));
                  }}
                />
              ))}
            </div>

            <div className="bg-warning bg-opacity-10 p-3 rounded border border-warning border-opacity-25 mb-4">
              <div className="d-flex justify-content-between align-items-center">
                <div>
                  <h6 className="fw-bold mb-0 text-warning">Updated Total Price</h6>
                  <small className="text-muted">Room Base + Selected Amenities</small>
                </div>
                <h4 className="fw-bold mb-0 text-warning">₹{calculatedEditTotal.toFixed(2)}</h4>
              </div>
            </div>

            <Button variant="warning" type="submit" disabled={updatingRoom} className="w-100 fw-bold">
              {updatingRoom ? <Spinner animation="border" size="sm" /> : 'Update Room Details'}
            </Button>
          </Form>
        </Modal.Body>
      </Modal>

      {/* Status Update Modal */}
      <Modal show={showStatusModal} onHide={() => setShowStatusModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>Update Room {selectedRoom?.roomNumber} Status</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form.Group className="mb-3">
            <Form.Label className="fw-bold">New Status</Form.Label>
            <Form.Select
              value={statusUpdate.status}
              onChange={(e) => setStatusUpdate(prev => ({ ...prev, status: e.target.value }))}
            >
              <option value="AVAILABLE">Available</option>
              <option value="OCCUPIED">Occupied</option>
              <option value="MAINTENANCE">Maintenance</option>
            </Form.Select>
          </Form.Group>
          {statusUpdate.status === 'MAINTENANCE' && (
            <Row>
              <Col>
                <Form.Group className="mb-3">
                  <Form.Label>Start Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={statusUpdate.maintenanceStartDate || ''}
                    onChange={(e) => setStatusUpdate(prev => ({ ...prev, maintenanceStartDate: e.target.value }))}
                  />
                </Form.Group>
              </Col>
              <Col>
                <Form.Group className="mb-3">
                  <Form.Label>End Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={statusUpdate.maintenanceEndDate || ''}
                    onChange={(e) => setStatusUpdate(prev => ({ ...prev, maintenanceEndDate: e.target.value }))}
                  />
                </Form.Group>
              </Col>
            </Row>
          )}
          {statusUpdate.status === 'OCCUPIED' && (
            <Row>
              <Col>
                <Form.Group className="mb-3">
                  <Form.Label>Check-in Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={statusUpdate.occupiedStartDate || ''}
                    onChange={(e) => setStatusUpdate(prev => ({ ...prev, occupiedStartDate: e.target.value }))}
                  />
                </Form.Group>
              </Col>
              <Col>
                <Form.Group className="mb-3">
                  <Form.Label>Check-out Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={statusUpdate.occupiedEndDate || ''}
                    onChange={(e) => setStatusUpdate(prev => ({ ...prev, occupiedEndDate: e.target.value }))}
                  />
                </Form.Group>
              </Col>
            </Row>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setShowStatusModal(false)}>Cancel</Button>
          <Button variant="primary" onClick={handleStatusUpdate}>Update Status</Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
