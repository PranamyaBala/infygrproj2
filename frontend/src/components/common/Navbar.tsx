import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Navbar as BSNavbar, Nav, Container, Button, Badge, Modal } from 'react-bootstrap';
import { FaHotel, FaUser, FaSignOutAlt, FaTachometerAlt, FaSearch, FaCalendarAlt, FaCog } from 'react-icons/fa';
import { useAuth } from '../../context/AuthContext';

export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [showLogoutModal, setShowLogoutModal] = useState(false);

  const handleLogoutClick = () => {
    setShowLogoutModal(true);
  };

  const confirmLogout = () => {
    logout();
    setShowLogoutModal(false);
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname.startsWith(path);

  return (
    <BSNavbar bg="dark" variant="dark" expand="lg" sticky="top" className="shadow-sm">
      <Container fluid>
        <BSNavbar.Brand as={Link} to={isAdmin ? '/admin' : '/rooms'} className="d-flex align-items-center gap-2">
          <FaHotel size={24} className="text-primary" />
          <span className="fw-bold">HostelHub</span>
        </BSNavbar.Brand>

        <BSNavbar.Toggle aria-controls="main-navbar" />
        <BSNavbar.Collapse id="main-navbar">
          <Nav className="me-auto">
            {isAuthenticated && !isAdmin && (
              <>
                <Nav.Link
                  as={Link}
                  to="/rooms"
                  className={isActive('/rooms') ? 'active' : ''}
                >
                  <FaSearch className="me-1" /> Search Rooms
                </Nav.Link>
                <Nav.Link
                  as={Link}
                  to="/bookings"
                  className={isActive('/bookings') ? 'active' : ''}
                >
                  <FaCalendarAlt className="me-1" /> My Bookings
                </Nav.Link>
                <Nav.Link
                  as={Link}
                  to="/preferences"
                  className={isActive('/preferences') ? 'active' : ''}
                >
                  <FaCog className="me-1" /> Preferences
                </Nav.Link>
              </>
            )}
            {isAuthenticated && isAdmin && (
              <>
                <Nav.Link
                  as={Link}
                  to="/admin"
                  className={location.pathname === '/admin' ? 'active' : ''}
                >
                  <FaTachometerAlt className="me-1" /> Dashboard
                </Nav.Link>
                <Nav.Link
                  as={Link}
                  to="/admin/bookings"
                  className={isActive('/admin/bookings') ? 'active' : ''}
                >
                  <FaCalendarAlt className="me-1" /> Bookings
                </Nav.Link>
                <Nav.Link
                  as={Link}
                  to="/admin/reports"
                  className={isActive('/admin/reports') ? 'active' : ''}
                >
                  <FaTachometerAlt className="me-1" /> Reports
                </Nav.Link>
              </>
            )}
          </Nav>

          <Nav className="align-items-center">
            {isAuthenticated ? (
              <>
                <Nav.Link as={Link} to="/profile" className="d-flex align-items-center gap-2">
                  <FaUser />
                  <span>{user?.firstName} {user?.lastName}</span>
                  <Badge bg={isAdmin ? 'danger' : 'primary'} pill>
                    {isAdmin ? 'Admin' : 'Student'}
                  </Badge>
                </Nav.Link>
                <Button
                  variant="outline-light"
                  size="sm"
                  onClick={handleLogoutClick}
                  className="ms-2"
                >
                  <FaSignOutAlt className="me-1" /> Logout
                </Button>
              </>
            ) : (
              <>
                <Nav.Link as={Link} to="/login">Login</Nav.Link>
                <Nav.Link as={Link} to="/register">
                  <Button variant="primary" size="sm">Register</Button>
                </Nav.Link>
              </>
            )}
          </Nav>
        </BSNavbar.Collapse>
      </Container>

      {/* Logout Confirmation Modal */}
      <Modal show={showLogoutModal} onHide={() => setShowLogoutModal(false)} centered size="sm">
        <Modal.Header closeButton className="border-0 pb-0">
          <Modal.Title className="fs-5 fw-bold">Confirm Logout</Modal.Title>
        </Modal.Header>
        <Modal.Body className="py-3">
          Are you sure you want to log out of your session?
        </Modal.Body>
        <Modal.Footer className="border-0 pt-0">
          <Button variant="light" onClick={() => setShowLogoutModal(false)} className="fw-semibold">
            Cancel
          </Button>
          <Button variant="danger" onClick={confirmLogout} className="fw-semibold">
            Logout
          </Button>
        </Modal.Footer>
      </Modal>
    </BSNavbar>
  );
}
