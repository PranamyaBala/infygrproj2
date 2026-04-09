import { Container } from 'react-bootstrap';

export default function Footer() {
  return (
    <footer className="bg-dark text-light py-4 mt-auto">
      <Container>
        <div className="row">
          <div className="col-md-6">
            <h6 className="text-primary fw-bold">HostelHub</h6>
            <p className="text-muted small mb-0">
              Student Hostel & Room Allocation Management System
            </p>
          </div>
          <div className="col-md-6 text-md-end">
            <p className="text-muted small mb-0">
              © {new Date().getFullYear()} HostelHub. All rights reserved.
            </p>
          </div>
        </div>
      </Container>
    </footer>
  );
}
