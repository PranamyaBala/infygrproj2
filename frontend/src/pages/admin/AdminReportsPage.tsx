import { useState } from 'react';
import {
  Container, Row, Col, Card, Form, Button, Table, Spinner, Alert
} from 'react-bootstrap';
import {
  FaChartBar, FaCalendarAlt, FaSearch
} from 'react-icons/fa';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, Cell
} from 'recharts';
import { bookingApi } from '../../api/bookingApi';
import type { OccupancyReport } from '../../types';

const COLORS = ['#0d6efd', '#6610f2', '#6f42c1', '#d63384', '#dc3545', '#fd7e14', '#ffc107', '#198754', '#20c997', '#0dcaf0', '#6c757d', '#adb5bd'];

export default function AdminReportsPage() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [report, setReport] = useState<OccupancyReport[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [generated, setGenerated] = useState(false);

  const handleGenerateReport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!startDate || !endDate) { setError('Please select both dates'); return; }
    if (startDate > endDate) { setError('Start date must be before end date'); return; }

    setLoading(true);
    setError('');
    try {
      const res = await bookingApi.getOccupancyReport(startDate, endDate);
      setReport(res.data);
      setGenerated(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to generate report.');
    } finally {
      setLoading(false);
    }
  };

  const avgOccupancy = report.length > 0
    ? (report.reduce((sum, r) => sum + r.occupancyRate, 0) / report.length).toFixed(1)
    : '0';

  const totalRevenue = report.reduce((sum, r) => sum + r.revenue, 0);

  return (
    <Container fluid className="py-4">
      <h3 className="fw-bold mb-4">
        <FaChartBar className="me-2 text-primary" />Occupancy Reports
      </h3>

      {/* Date Range Selector */}
      <Card className="border-0 shadow-sm mb-4">
        <Card.Body>
          <Form onSubmit={handleGenerateReport}>
            <Row className="align-items-end">
              <Col md={4}>
                <Form.Group controlId="reportStartDate">
                  <Form.Label className="fw-bold"><FaCalendarAlt className="me-2" />Start Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={startDate}
                    onChange={(e) => setStartDate(e.target.value)}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Form.Group controlId="reportEndDate">
                  <Form.Label className="fw-bold"><FaCalendarAlt className="me-2" />End Date</Form.Label>
                  <Form.Control
                    type="date"
                    value={endDate}
                    onChange={(e) => setEndDate(e.target.value)}
                    required
                  />
                </Form.Group>
              </Col>
              <Col md={4}>
                <Button variant="primary" type="submit" size="lg" className="w-100" disabled={loading}>
                  {loading ? <Spinner animation="border" size="sm" /> : <><FaSearch className="me-2" />Generate Report</>}
                </Button>
              </Col>
            </Row>
          </Form>
        </Card.Body>
      </Card>

      {error && <Alert variant="danger">{error}</Alert>}

      {generated && report.length > 0 && (
        <>
          {/* Summary Stats */}
          <Row className="mb-4">
            <Col md={4} className="mb-3">
              <Card className="border-0 shadow-sm bg-primary bg-opacity-10">
                <Card.Body className="text-center">
                  <h6 className="text-muted">Rooms Analyzed</h6>
                  <h2 className="fw-bold text-primary">{report.length}</h2>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4} className="mb-3">
              <Card className="border-0 shadow-sm bg-success bg-opacity-10">
                <Card.Body className="text-center">
                  <h6 className="text-muted">Avg Occupancy Rate</h6>
                  <h2 className="fw-bold text-success">{avgOccupancy}%</h2>
                </Card.Body>
              </Card>
            </Col>
            <Col md={4} className="mb-3">
              <Card className="border-0 shadow-sm bg-warning bg-opacity-10">
                <Card.Body className="text-center">
                  <h6 className="text-muted">Total Revenue</h6>
                  <h2 className="fw-bold text-warning">₹{totalRevenue.toFixed(2)}</h2>
                </Card.Body>
              </Card>
            </Col>
          </Row>

          {/* Bar Chart */}
          <Card className="border-0 shadow-sm mb-4">
            <Card.Header className="bg-white">
              <h5 className="mb-0 fw-bold">Occupancy Rate by Room (%)</h5>
            </Card.Header>
            <Card.Body>
              <ResponsiveContainer width="100%" height={400}>
                <BarChart data={report} margin={{ top: 20, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="roomNumber" label={{ value: 'Room Number', position: 'insideBottom', offset: -5 }} />
                  <YAxis label={{ value: 'Occupancy %', angle: -90, position: 'insideLeft' }} domain={[0, 100]} />
                  <Tooltip
                    formatter={(value) => [`${value}%`, 'Occupancy Rate']}
                    labelFormatter={(label) => `Room ${label}`}
                  />
                  <Legend />
                  <Bar dataKey="occupancyRate" name="Occupancy Rate" radius={[4, 4, 0, 0]}>
                    {report.map((_, index) => (
                      <Cell key={index} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </Card.Body>
          </Card>

          {/* Data Table */}
          <Card className="border-0 shadow-sm">
            <Card.Header className="bg-white">
              <h5 className="mb-0 fw-bold">Detailed Data</h5>
            </Card.Header>
            <Card.Body className="p-0">
              <Table responsive hover className="mb-0">
                <thead className="table-light">
                  <tr>
                    <th>Room #</th>
                    <th>Room Type</th>
                    <th>Total Days</th>
                    <th>Occupied Days</th>
                    <th>Occupancy Rate</th>
                    <th>Revenue</th>
                  </tr>
                </thead>
                <tbody>
                  {report.map(r => (
                    <tr key={r.roomId}>
                      <td className="fw-bold">{r.roomNumber}</td>
                      <td>{r.roomType}</td>
                      <td>{r.totalDays}</td>
                      <td>{r.occupiedDays}</td>
                      <td>
                        <div className="d-flex align-items-center gap-2">
                          <div className="progress flex-grow-1" style={{ height: '8px' }}>
                            <div
                              className={`progress-bar ${
                                r.occupancyRate >= 75 ? 'bg-success' :
                                r.occupancyRate >= 50 ? 'bg-warning' : 'bg-danger'
                              }`}
                              style={{ width: `${r.occupancyRate}%` }}
                            />
                          </div>
                          <span className="fw-bold">{r.occupancyRate}%</span>
                        </div>
                      </td>
                      <td className="fw-bold">₹{r.revenue.toFixed(2)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            </Card.Body>
          </Card>
        </>
      )}

      {generated && report.length === 0 && !loading && (
        <Alert variant="info">No data found for the selected date range.</Alert>
      )}
    </Container>
  );
}
