import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Card, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { FaHotel, FaEnvelope, FaLock } from 'react-icons/fa';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!email.trim()) { setError('Please provide a valid email'); return; }
    if (!password.trim()) { setError('Please provide a valid password'); return; }

    setLoading(true);
    try {
      const response = await authApi.login({ email, password });
      login(response.data);
      if (response.data.role === 'ADMIN') {
        navigate('/admin');
      } else {
        navigate('/rooms');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="bg-dark min-vh-100 d-flex align-items-center" style={{
      background: 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)'
    }}>
      <Container>
        <Row className="justify-content-center">
          <Col md={6} lg={5}>
            <div className="text-center mb-4">
              <FaHotel size={48} className="text-primary mb-3" />
              <h2 className="text-white fw-bold">Welcome to HostelHub</h2>

            </div>

            <Card className="shadow-lg border-0" style={{ borderRadius: '1rem' }}>
              <Card.Body className="p-4">
                <h4 className="text-center mb-4 fw-bold">Sign In</h4>

                {error && <Alert variant="danger" dismissible onClose={() => setError('')}>{error}</Alert>}

                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3" controlId="loginEmail">
                    <Form.Label><FaEnvelope className="me-2" />Email Address</Form.Label>
                    <Form.Control
                      type="email"
                      placeholder="Enter your email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      size="lg"
                      required
                    />
                  </Form.Group>

                  <Form.Group className="mb-4" controlId="loginPassword">
                    <Form.Label><FaLock className="me-2" />Password</Form.Label>
                    <Form.Control
                      type="password"
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      size="lg"
                      required
                    />
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
                        Signing In...
                      </>
                    ) : (
                      'Sign In'
                    )}
                  </Button>
                </Form>

                <div className="text-center mt-4">
                  <span className="text-muted">Don't have an account? </span>
                  <Link to="/register" className="text-primary fw-bold text-decoration-none">
                    Register
                  </Link>
                </div>

                <hr className="my-3" />
                <div className="text-center">
                  <small className="text-muted">
                    Demo: <strong>admin@hostel.com</strong> / <strong>Admin@123</strong>
                  </small>
                </div>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </div>
  );
}
