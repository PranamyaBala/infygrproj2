import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Container, Row, Col, Form, Button, Alert, Spinner } from 'react-bootstrap';
import { FaHotel, FaEnvelope, FaLock, FaArrowRight } from 'react-icons/fa';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
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
      login(response.data, rememberMe);
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
    <div className="min-vh-100 d-flex flex-column bg-light">
      <div className="flex-grow-1 d-flex">
        <Container fluid className="p-0">
          <Row className="g-0 min-vh-100">
            {/* Left Side: Image & Branding (Hidden on mobile) */}
            <Col lg={7} xl={8} className="d-none d-lg-block position-relative overflow-hidden">
              <div
                className="position-absolute inset-0 w-100 h-100"
                style={{
                  backgroundImage: 'url("/images/auth/login-bg.png")',
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                  filter: 'brightness(0.7)'
                }}
              />
              <div
                className="position-absolute inset-0 w-100 h-100"
                style={{
                  background: 'linear-gradient(45deg, rgba(15, 12, 41, 0.8) 0%, rgba(48, 43, 99, 0.4) 100%)'
                }}
              />
              <div className="position-relative h-100 d-flex flex-column justify-content-center px-5 text-white">
                <div className="mb-4">
                  <h1 className="display-3 fw-800 mb-3" style={{ letterSpacing: '-0.02em' }}>
                    <b>Elevate Your <span className="text-primary text-gradient">Hostel Experience</span></b>
                  </h1>
                  <p className="fs-4 fw-light opacity-90 max-w-lg">
                    Manage your stay, connect with peers, and access premium amenities all in one place.
                  </p>
                </div>

                <div className="d-flex gap-5 mt-5 pt-4">
                  <div>
                    <h3 className="fw-bold mb-1">1000+</h3>
                    <p className="text-light opacity-75">Student Capacity</p>
                  </div>
                  {/* <div>
                    <h3 className="fw-bold mb-1">CCTV</h3>
                    <p className="text-light opacity-75">Surveillance</p>
                  </div> */}
                  <div>
                    <h3 className="fw-bold mb-1">24/7 CCTV</h3>
                    <p className="text-light opacity-75">Security and Peace of mind</p>
                  </div>
                  <div>
                    <h3 className="fw-bold mb-1">Events</h3>
                    <p className="text-light opacity-75">Meet people</p>
                  </div>
                  <div>
                    <h3 className="fw-bold mb-1">Services</h3>
                    <p className="text-light opacity-75">AC,Wifi,Laundry,Food,Lounge</p>
                  </div>

                </div>
              </div>
            </Col>

            {/* Right Side: Login Form */}
            <Col lg={5} xl={4} className="d-flex align-items-center justify-content-center bg-white p-4 p-md-5">
              <div className="w-100" style={{ maxWidth: '420px' }}>
                <div className="text-center d-lg-none mb-4">
                  <FaHotel size={48} className="text-primary mb-3" />
                  <h2 className="fw-bold">HostelHub</h2>
                </div>

                <div className="mb-5">
                  <div className="d-lg-flex d-none align-items-center justify-content-center bg-primary rounded-circle mb-3 shadow-sm" style={{ width: '56px', height: '56px' }}>
                    <FaHotel size={24} className="text-white" />
                  </div>
                  <h2 className="fw-bold mb-2">Welcome Back</h2>
                  <p className="text-muted">Enter your credentials to access your account</p>
                </div>

                {error && (
                  <Alert variant="danger" className="border-0 shadow-sm mb-4 py-3" dismissible onClose={() => setError('')}>
                    {error}
                  </Alert>
                )}

                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-4" controlId="loginEmail">
                    <Form.Label className="fw-semibold small text-uppercase text-muted">Email Address</Form.Label>
                    <div className="input-group input-group-lg border rounded-3 overflow-hidden shadow-sm transition-all focus-within-shadow">
                      <span className="input-group-text bg-white border-0 ps-3">
                        <FaEnvelope className="text-muted" />
                      </span>
                      <Form.Control
                        type="email"
                        placeholder="name@hostel.com"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        className="border-0 ps-2"
                        required
                        style={{ fontSize: '1rem' }}
                      />
                    </div>
                  </Form.Group>

                  <Form.Group className="mb-4" controlId="loginPassword">
                    <div className="d-flex justify-content-between align-items-center mb-1">
                      <Form.Label className="fw-semibold small text-uppercase text-muted mb-0">Password</Form.Label>
                    </div>
                    <div className="input-group input-group-lg border rounded-3 overflow-hidden shadow-sm transition-all focus-within-shadow">
                      <span className="input-group-text bg-white border-0 ps-3">
                        <FaLock className="text-muted" />
                      </span>
                      <Form.Control
                        type="password"
                        placeholder="••••••••"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        className="border-0 ps-2"
                        required
                        style={{ fontSize: '1rem' }}
                      />
                    </div>
                  </Form.Group>

                  <div className="mb-4 form-check">
                    <input 
                      type="checkbox" 
                      className="form-check-input" 
                      id="rememberMe" 
                      checked={rememberMe}
                      onChange={(e) => setRememberMe(e.target.checked)}
                    />
                    <label className="form-check-label small text-muted" htmlFor="rememberMe">Remember me for 30 days</label>
                  </div>

                  <Button
                    variant="primary"
                    type="submit"
                    size="lg"
                    className="w-100 py-3 fw-bold shadow-sm d-flex align-items-center justify-content-center gap-2"
                    disabled={loading}
                    style={{ borderRadius: '0.75rem' }}
                  >
                    {loading ? (
                      <><Spinner animation="border" size="sm" /> Authenticating...</>
                    ) : (
                      <><FaArrowRight className="ms-2 order-last" /> Sign In</>
                    )}
                  </Button>
                </Form>

                <div className="text-center mt-5">
                  <p className="text-muted mb-0">
                    New to HostelHub?{' '}
                    <Link to="/register" className="text-primary fw-bold text-decoration-none hover-underline">
                      Create an account
                    </Link>
                  </p>
                </div>

                <div className="mt-5 pt-4 border-top">
                  <div className="p-3 bg-light rounded-3 text-center small text-muted">
                    <span className="fw-bold d-block mb-1">Demo Access</span>
                    admin@hostel.com / Admin@123
                  </div>
                </div>
              </div>
            </Col>
          </Row>
        </Container>
      </div>
    </div>
  );
}
