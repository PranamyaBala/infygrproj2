import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider, useAuth } from './context/AuthContext';

// Common
import Layout from './components/common/Layout';
import ProtectedRoute from './components/common/ProtectedRoute';

// Auth Pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';

// Student Pages
import RoomSearchPage from './pages/student/RoomSearchPage';
import RoomDetailPage from './pages/student/RoomDetailPage';
import MyBookingsPage from './pages/student/MyBookingsPage';
import BookingDetailPage from './pages/student/BookingDetailPage';
import ProfilePage from './pages/student/ProfilePage';
import PreferencesPage from './pages/student/PreferencesPage';

// Admin Pages
import AdminDashboard from './pages/admin/AdminDashboard';
import BookingManagementPage from './pages/admin/BookingManagementPage';
import AdminReportsPage from './pages/admin/AdminReportsPage';

// Styles
import 'bootstrap/dist/css/bootstrap.min.css';
import './index.css';
import './App.css';

function AppRoutes() {
  const { isAuthenticated, isAdmin } = useAuth();

  return (
    <Routes>
      {/* Public Routes */}
      <Route path="/login" element={
        isAuthenticated ? <Navigate to={isAdmin ? '/admin' : '/rooms'} /> : <LoginPage />
      } />
      <Route path="/register" element={
        isAuthenticated ? <Navigate to="/rooms" /> : <RegisterPage />
      } />

      {/* Protected Routes (inside Layout with Navbar) */}
      <Route element={<ProtectedRoute><Layout /></ProtectedRoute>}>
        {/* Student Routes */}
        <Route path="/rooms" element={<RoomSearchPage />} />
        <Route path="/rooms/:id" element={<RoomDetailPage />} />
        <Route path="/bookings" element={<MyBookingsPage />} />
        <Route path="/bookings/:id" element={<BookingDetailPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="/preferences" element={<PreferencesPage />} />

        {/* Admin Routes */}
        <Route path="/admin" element={
          <ProtectedRoute adminOnly><AdminDashboard /></ProtectedRoute>
        } />
        <Route path="/admin/bookings" element={
          <ProtectedRoute adminOnly><BookingManagementPage /></ProtectedRoute>
        } />
        <Route path="/admin/reports" element={
          <ProtectedRoute adminOnly><AdminReportsPage /></ProtectedRoute>
        } />
      </Route>

      {/* Default Redirect */}
      <Route path="/" element={<Navigate to={isAuthenticated ? (isAdmin ? '/admin' : '/rooms') : '/login'} />} />
      <Route path="*" element={<Navigate to="/" />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Toaster
          position="top-right"
          toastOptions={{
            duration: 3000,
            style: { borderRadius: '8px', background: '#333', color: '#fff' },
          }}
        />
        <AppRoutes />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
