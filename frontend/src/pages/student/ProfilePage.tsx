import { useState, useEffect } from 'react';
import {
  Container, Row, Col, Card, Form, Button, Spinner, Alert
} from 'react-bootstrap';
import {
  FaUser, FaEnvelope, FaPhone, FaSave, FaCamera, FaTrash
} from 'react-icons/fa';
import { userApi } from '../../api/userApi';
import type { User } from '../../types';
import toast from 'react-hot-toast';

export default function ProfilePage() {
  const [profile, setProfile] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const [formData, setFormData] = useState({
    firstName: '', lastName: '', phone: '', email: ''
  });

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      const res = await userApi.getProfile();
      setProfile(res.data);
      setFormData({
        firstName: res.data.firstName,
        lastName: res.data.lastName,
        phone: res.data.phone || '',
        email: res.data.email,
      });
    } catch {
      setError('Failed to load profile.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.firstName.trim()) { setError('Please provide a valid first name'); return; }
    if (!formData.lastName.trim()) { setError('Please provide a valid last name'); return; }

    setSaving(true);
    setError('');
    try {
      const res = await userApi.updateProfile(formData);
      setProfile(res.data);
      toast.success('Profile updated successfully!');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const handleUploadPicture = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    try {
      const res = await userApi.uploadProfilePicture(file);
      setProfile(res.data);
      toast.success('Profile picture uploaded!');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to upload picture.');
    }
  };

  const handleDeletePicture = async () => {
    try {
      const res = await userApi.deleteProfilePicture();
      setProfile(res.data);
      toast.success('Profile picture removed.');
    } catch {
      toast.error('Failed to remove picture.');
    }
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
        <FaUser className="me-2 text-primary" />My Profile
      </h3>

      <Row>
        <Col lg={4} className="mb-4">
          <Card className="border-0 shadow-sm text-center">
            <Card.Body className="p-4">
              <div
                className="rounded-circle bg-primary d-flex align-items-center justify-content-center mx-auto mb-3"
                style={{ width: '120px', height: '120px', fontSize: '3rem', color: 'white' }}
              >
                {profile?.firstName?.charAt(0)}{profile?.lastName?.charAt(0)}
              </div>
              <h5 className="fw-bold">{profile?.firstName} {profile?.lastName}</h5>
              <p className="text-muted">{profile?.email}</p>

              <div className="d-grid gap-2">
                <Button variant="outline-primary" as="label" className="mb-0">
                  <FaCamera className="me-2" />Upload Photo
                  <input
                    type="file"
                    hidden
                    accept="image/jpeg,image/png,image/gif,image/webp"
                    onChange={handleUploadPicture}
                  />
                </Button>
                {profile?.profilePicturePath && (
                  <Button variant="outline-danger" size="sm" onClick={handleDeletePicture}>
                    <FaTrash className="me-2" />Remove Photo
                  </Button>
                )}
              </div>
            </Card.Body>
          </Card>
        </Col>

        <Col lg={8}>
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white">
              <h5 className="mb-0 fw-bold">Edit Profile</h5>
            </Card.Header>
            <Card.Body>
              {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

              <Form onSubmit={handleSave}>
                <Row>
                  <Col md={6}>
                    <Form.Group className="mb-3" controlId="profileFirstName">
                      <Form.Label><FaUser className="me-2" />First Name</Form.Label>
                      <Form.Control
                        type="text"
                        value={formData.firstName}
                        onChange={(e) => setFormData(prev => ({ ...prev, firstName: e.target.value }))}
                        required
                      />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group className="mb-3" controlId="profileLastName">
                      <Form.Label><FaUser className="me-2" />Last Name</Form.Label>
                      <Form.Control
                        type="text"
                        value={formData.lastName}
                        onChange={(e) => setFormData(prev => ({ ...prev, lastName: e.target.value }))}
                        required
                      />
                    </Form.Group>
                  </Col>
                </Row>

                <Form.Group className="mb-3" controlId="profileEmail">
                  <Form.Label><FaEnvelope className="me-2" />Email Address</Form.Label>
                  <Form.Control
                    type="email"
                    value={formData.email}
                    onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                    required
                  />
                </Form.Group>

                <Form.Group className="mb-4" controlId="profilePhone">
                  <Form.Label><FaPhone className="me-2" />Phone Number</Form.Label>
                  <Form.Control
                    type="tel"
                    value={formData.phone}
                    onChange={(e) => setFormData(prev => ({ ...prev, phone: e.target.value }))}
                  />
                </Form.Group>

                <Button variant="primary" type="submit" disabled={saving}>
                  {saving ? (
                    <><Spinner animation="border" size="sm" className="me-2" />Saving...</>
                  ) : (
                    <><FaSave className="me-2" />Save Changes</>
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
