import { useState, useEffect } from 'react';
import {
  Container, Card, Form, Button, Row, Col, Alert, Spinner
} from 'react-bootstrap';
import { FaCog, FaSave, FaBed, FaBuilding, FaMoneyBillWave } from 'react-icons/fa';
import { userApi } from '../../api/userApi';
import { roomApi } from '../../api/roomApi';
import type { UserPreference, Amenity } from '../../types';
import toast from 'react-hot-toast';

const ROOM_TYPES = ['SINGLE', 'DOUBLE', 'TRIPLE', 'SUITE', 'DORMITORY'];

export default function PreferencesPage() {
  const [preferences, setPreferences] = useState<UserPreference>({});
  const [amenities, setAmenities] = useState<Amenity[]>([]);
  const [selectedAmenities, setSelectedAmenities] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [prefRes, amenityRes] = await Promise.all([
        userApi.getPreferences(),
        roomApi.getAllAmenities(),
      ]);
      setPreferences(prefRes.data);
      setAmenities(amenityRes.data);
      setSelectedAmenities(prefRes.data.preferredAmenities || []);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    try {
      await userApi.savePreferences({
        ...preferences,
        preferredAmenities: selectedAmenities
      });
      toast.success('Preferences saved! They will be applied to future searches.');
    } catch {
      setError('Failed to save preferences.');
    } finally {
      setSaving(false);
    }
  };

  const toggleAmenity = (name: string) => {
    setSelectedAmenities(prev =>
      prev.includes(name) ? prev.filter(a => a !== name) : [...prev, name]
    );
  };

  if (loading) {
    return (
      <Container className="py-5 text-center">
        <Spinner animation="border" variant="primary" />
      </Container>
    );
  }

  return (
    <Container className="py-4">
      <h3 className="fw-bold mb-4">
        <FaCog className="me-2 text-primary" />Room Search Preferences
      </h3>
      <p className="text-muted mb-4">
        Save your preferred settings so you don't have to re-enter them when searching for rooms.
      </p>

      <Row className="justify-content-center">
        <Col lg={8}>
          <Card className="border-0 shadow-sm">
            <Card.Body className="p-4">
              {error && <Alert variant="danger">{error}</Alert>}

              <Form onSubmit={handleSave}>
                <Form.Group className="mb-3" controlId="prefRoomType">
                  <Form.Label className="fw-bold"><FaBed className="me-2" />Preferred Room Type</Form.Label>
                  <Form.Select
                    value={preferences.preferredRoomType || ''}
                    onChange={(e) => setPreferences(prev => ({ ...prev, preferredRoomType: e.target.value || undefined }))}
                  >
                    <option value="">No preference</option>
                    {ROOM_TYPES.map(t => (
                      <option key={t} value={t}>{t.charAt(0) + t.slice(1).toLowerCase()}</option>
                    ))}
                  </Form.Select>
                </Form.Group>

                <Form.Group className="mb-3" controlId="prefFloor">
                  <Form.Label className="fw-bold"><FaBuilding className="me-2" />Preferred Floor</Form.Label>
                  <Form.Control
                    type="number"
                    placeholder="No preference"
                    min={1}
                    value={preferences.preferredFloor || ''}
                    onChange={(e) => setPreferences(prev => ({
                      ...prev, preferredFloor: e.target.value ? parseInt(e.target.value) : undefined
                    }))}
                  />
                </Form.Group>

                <Form.Label className="fw-bold"><FaMoneyBillWave className="me-2" />Price Range (₹/night)</Form.Label>
                <Row className="mb-3">
                  <Col>
                    <Form.Control
                      type="number"
                      placeholder="Min price"
                      min={0}
                      value={preferences.preferredMinPrice || ''}
                      onChange={(e) => setPreferences(prev => ({
                        ...prev, preferredMinPrice: e.target.value ? parseFloat(e.target.value) : undefined
                      }))}
                    />
                  </Col>
                  <Col>
                    <Form.Control
                      type="number"
                      placeholder="Max price"
                      min={0}
                      value={preferences.preferredMaxPrice || ''}
                      onChange={(e) => setPreferences(prev => ({
                        ...prev, preferredMaxPrice: e.target.value ? parseFloat(e.target.value) : undefined
                      }))}
                    />
                  </Col>
                </Row>

                <Form.Label className="fw-bold mb-2">Preferred Amenities</Form.Label>
                <div className="mb-4">
                  {amenities.map(a => (
                    <Form.Check
                      key={a.id}
                      type="checkbox"
                      id={`pref-amenity-${a.id}`}
                      label={a.name}
                      checked={selectedAmenities.includes(a.name)}
                      onChange={() => toggleAmenity(a.name)}
                      className="mb-1"
                    />
                  ))}
                </div>

                <Button variant="primary" type="submit" size="lg" disabled={saving}>
                  {saving ? (
                    <><Spinner animation="border" size="sm" className="me-2" />Saving...</>
                  ) : (
                    <><FaSave className="me-2" />Save Preferences</>
                  )}
                </Button>
              </Form>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
