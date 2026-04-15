import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Container, Row, Col, Card, Table, Badge, Button, Form,
  Spinner, Alert, Modal
} from 'react-bootstrap';
import {
  FaArrowLeft, FaMoneyBillWave, FaCalendarPlus, FaPercent,
  FaCogs, FaHistory
} from 'react-icons/fa';
import { roomApi } from '../../api/roomApi';
import type { Room, PricingTier } from '../../types';
import toast from 'react-hot-toast';

export default function PricingTierManagement() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const [room, setRoom] = useState<Room | null>(null);
  const [tiers, setTiers] = useState<PricingTier[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Add Tier Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newTier, setNewTier] = useState<PricingTier>({
    seasonName: '', 
    startDate: '', 
    endDate: '', 
    priceMultiplier: 1.0
  });
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadData();
  }, [roomId]);

  const loadData = async () => {
    if (!roomId) return;
    setLoading(true);
    try {
      const [roomRes, tiersRes] = await Promise.all([
        roomApi.getRoomById(Number(roomId)),
        roomApi.getPricingTiers(Number(roomId))
      ]);
      setRoom(roomRes.data);
      setTiers(tiersRes.data);
    } catch (err: any) {
      setError('Failed to load pricing data.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddTier = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!roomId) return;
    setSubmitting(true);
    try {
      await roomApi.addPricingTier(Number(roomId), newTier);
      toast.success('Pricing tier added successfully!');
      setShowAddModal(false);
      loadData();
      setNewTier({ seasonName: '', startDate: '', endDate: '', priceMultiplier: 1.0 });
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add pricing tier.');
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
            <FaMoneyBillWave className="me-2 text-primary" />
            Pricing Tiers: Room {room.roomNumber}
          </h3>
          <p className="text-muted mb-0">Base Price: ₹{room.pricePerNight} per night</p>
        </div>
        <Button variant="primary" onClick={() => setShowAddModal(true)}>
          <FaCalendarPlus className="me-2" />Add Seasonal Rate
        </Button>
      </div>

      <Row>
        <Col lg={4}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body>
              <h5 className="fw-bold mb-3">Dynamic Pricing 101</h5>
              <p className="small text-muted">
                Seasonal rates allow you to adjust room prices for holidays, exam periods, or peak seasons without changing the base price.
              </p>
              <ul className="small text-muted ps-3">
                <li><strong>Multiplier 1.5:</strong> Increases price by 50%</li>
                <li><strong>Multiplier 2.0:</strong> Doubles the price</li>
                <li><strong>Multiplier 0.8:</strong> Gives a 20% discount</li>
              </ul>
              <div className="bg-light p-3 rounded text-center mt-3">
                <FaPercent className="text-primary mb-2" size={24} />
                <h6 className="fw-bold mb-0">Auto-Calculation</h6>
                <small className="text-muted">Applied specifically during booking based on dates.</small>
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={8}>
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white">
              <h5 className="mb-0 fw-bold"><FaHistory className="me-2" />Pricing History & Tiers</h5>
            </Card.Header>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Season Name</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Multiplier</th>
                    <th>Effective Price</th>
                  </tr>
                </thead>
                <tbody>
                  {tiers.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="text-center py-4 text-muted">
                        No seasonal rates defined for this room.
                      </td>
                    </tr>
                  ) : (
                    tiers.map(tier => (
                      <tr key={tier.id}>
                        <td className="fw-bold">{tier.seasonName}</td>
                        <td>{tier.startDate}</td>
                        <td>{tier.endDate}</td>
                        <td>
                          <Badge bg="info" className="text-white">{tier.priceMultiplier}x</Badge>
                        </td>
                        <td className="fw-bold text-primary">
                          ₹{(room.pricePerNight * tier.priceMultiplier).toFixed(2)}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </Col>
      </Row>

      {/* Add Tier Modal (US 13) */}
      <Modal show={showAddModal} onHide={() => setShowAddModal(false)} centered>
        <Modal.Header closeButton className="bg-primary text-white">
          <Modal.Title><FaCogs className="me-2" />Set Seasonal Rate</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleAddTier}>
            <Form.Group className="mb-3" controlId="seasonName">
              <Form.Label className="fw-bold">Season/Event Name</Form.Label>
              <Form.Control
                type="text"
                placeholder="e.g., Summer Break, Exam Week"
                value={newTier.seasonName}
                onChange={(e) => setNewTier(prev => ({ ...prev, seasonName: e.target.value }))}
                required
              />
            </Form.Group>
            <Row>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="tierStart">
                  <Form.Label className="fw-bold">Start Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={newTier.startDate}
                    onChange={(e) => setNewTier(prev => ({ ...prev, startDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={6}>
                <Form.Group className="mb-3" controlId="tierEnd">
                  <Form.Label className="fw-bold">End Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={newTier.endDate}
                    min={newTier.startDate}
                    onChange={(e) => setNewTier(prev => ({ ...prev, endDate: e.target.value }))}
                    required
                  />
                </Form.Group>
              </Col>
            </Row>
            <Form.Group className="mb-3" controlId="tierMultiplier">
              <Form.Label className="fw-bold">Price Multiplier</Form.Label>
              <Form.Control
                type="number"
                step="0.01"
                min="0.1"
                value={newTier.priceMultiplier}
                onChange={(e) => setNewTier(prev => ({ ...prev, priceMultiplier: parseFloat(e.target.value) }))}
                required
              />
              <Form.Text className="text-muted">
                Currently: ₹{(room.pricePerNight * (newTier.priceMultiplier || 1)).toFixed(2)} / night
              </Form.Text>
            </Form.Group>
            <Button variant="primary" type="submit" className="w-100 fw-bold" disabled={submitting}>
              {submitting ? 'Adding...' : 'Save Seasonal Rate'}
            </Button>
          </Form>
        </Modal.Body>
      </Modal>
    </Container>
  );
}
