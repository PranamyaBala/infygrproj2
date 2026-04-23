import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { FaHotel, FaEnvelope, FaLock, FaUser, FaPhone } from 'react-icons/fa';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';
import Footer from '../../components/common/Footer';

export default function RegisterPage() {
  const [formData, setFormData] = useState({
    firstName: '', lastName: '', email: '', password: '', phone: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleChange = (field: string, value: string) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!formData.firstName.trim()) { setError('Please provide a valid first name'); return; }
    if (!formData.lastName.trim()) { setError('Please provide a valid last name'); return; }
    if (!formData.email.trim()) { setError('Please provide a valid email'); return; }
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    if (!formData.password || !passwordRegex.test(formData.password)) {
      setError('Password must be at least 8 characters and include uppercase, lowercase, number, and special character (@$!%*?&).');
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.register(formData);
      login(response.data);
      navigate('/rooms');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="d-flex flex-column min-vh-100" style={{
      background: 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)'
    }}>
      <div className="flex-grow-1 d-flex align-items-center py-5">
        <Container>
          <Row className="justify-content-center">
            <Col md={7} lg={6}>
              <div className="text-center mb-4">
                <FaHotel size={48} className="text-primary mb-3" />
                <h2 className="text-white fw-bold">Join HostelHub</h2>
                <p className="text-muted">Create your account to start booking rooms</p>
              </div>

              <Card className="shadow-lg border-0" style={{ borderRadius: '1rem' }}>
                <Card.Body className="p-4">
                  <h4 className="text-center mb-4 fw-bold">Create Account</h4>

                  {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

                  <Form onSubmit={handleSubmit}>
                    <Row>
                      <Col md={6}>
                        <Form.Group className="mb-3" controlId="registerFirstName">
                          <Form.Label><FaUser className="me-2" />First Name</Form.Label>
                          <Form.Control
                            type="text"
                            placeholder="First name"
                            value={formData.firstName}
                            onChange={(e) => handleChange('firstName', e.target.value)}
                            required
                          />
                        </Form.Group>
                      </Col>
                      <Col md={6}>
                        <Form.Group className="mb-3" controlId="registerLastName">
                          <Form.Label><FaUser className="me-2" />Last Name</Form.Label>
                          <Form.Control
                            type="text"
                            placeholder="Last name"
                            value={formData.lastName}
                            onChange={(e) => handleChange('lastName', e.target.value)}
                            required
                          />
                        </Form.Group>
                      </Col>
                    </Row>

                    <Form.Group className="mb-3" controlId="registerEmail">
                      <Form.Label><FaEnvelope className="me-2" />Email Address</Form.Label>
                      <Form.Control
                        type="email"
                        placeholder="your@email.com"
                        value={formData.email}
                        onChange={(e) => handleChange('email', e.target.value)}
                        required
                      />
                    </Form.Group>

                    <Form.Group className="mb-3" controlId="registerPhone">
                      <Form.Label><FaPhone className="me-2" />Phone Number</Form.Label>
                      <Form.Control
                        type="tel"
                        placeholder="Phone number (optional)"
                        value={formData.phone}
                        onChange={(e) => handleChange('phone', e.target.value)}
                      />
                    </Form.Group>

                      <Form.Group className="mb-4" controlId="registerPassword">
                      <Form.Label><FaLock className="me-2" />Password</Form.Label>
                      <Form.Control
                        type="password"
                        placeholder="8+ chars (incl. Uppercase, Number & Symbol)"
                        value={formData.password}
                        onChange={(e) => handleChange('password', e.target.value)}
                        required
                        minLength={8}
                      />
                      <Form.Text className="text-muted small">
                        At least 8 characters with Uppercase, Lowercase, Number & Special Character (@$!%*?&).
                      </Form.Text>
                    </Form.Group>

                    <Button
                      variant="primary"
                      type="submit"
                      size="lg"
                      className="w-100 fw-bold"
                      disabled={loading}
                    >
                      {loading ? (
                        <>
                          <Spinner animation="border" size="sm" className="me-2" />
                          Creating Account...
                        </>
                      ) : (
                        'Create Account'
                      )}
                    </Button>
                  </Form>

                  <div className="text-center mt-4">
                    <span className="text-muted">Already have an account? </span>
                    <Link to="/login" className="text-primary fw-bold text-decoration-none">
                      Sign In
                    </Link>
                  </div>
                </Card.Body>
              </Card>
            </Col>
          </Row>
        </Container>
      </div>
      <Footer />
    </div>
  );
}
