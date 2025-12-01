import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import ForgotPassword from './pages/ForgotPassword';
import CustomerHome from './pages/CustomerHome';
import VoiceOrderPage from './pages/VoiceOrderPage';
import StaffHome from './pages/StaffHome';
import AdminHome from './pages/AdminHome';
import PendingApproval from './pages/PendingApproval';
import Order from './pages/Order';
import Orders from './pages/Orders';
import Profile from './pages/Profile';
import DeliveryStatus from './pages/DeliveryStatus';
import AdminOrderManagement from './pages/AdminOrderManagement';
import AdminInventoryManagement from './pages/AdminInventoryManagement';
import AdminAccountManagement from './pages/AdminAccountManagement';
import AdminApprovalManagement from './pages/AdminApprovalManagement';
import AdminScheduleManagement from './pages/AdminScheduleManagement';
import AdminReservationChangeRequests from './pages/AdminReservationChangeRequests';
import EmployeeOrderManagement from './pages/EmployeeOrderManagement';
import EmployeeInventoryManagement from './pages/EmployeeInventoryManagement';
import ScheduleCalendar from './pages/ScheduleCalendar';
import { useAuth } from './contexts/AuthContext';
import './App.css';

function HomeRouter() {
  const { user } = useAuth();
  
  // 승인 대기 상태의 직원/관리자는 승인 대기 화면으로
  if (user?.approvalStatus === 'pending') {
    return <PendingApproval />;
  }
  
  if (user?.role === 'admin') {
    return <AdminHome />;
  }
  
  if (user?.role === 'employee') {
    return <StaffHome />;
  }
  
  return <CustomerHome />;
}

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route
            path="/"
            element={
              <PrivateRoute>
                <HomeRouter />
              </PrivateRoute>
            }
          />
          <Route
            path="/order"
            element={
              <PrivateRoute>
                <Order />
              </PrivateRoute>
            }
          />
          <Route
            path="/voice-order"
            element={
              <PrivateRoute>
                <VoiceOrderPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <PrivateRoute>
                <Orders />
              </PrivateRoute>
            }
          />
          <Route
            path="/profile"
            element={
              <PrivateRoute>
                <Profile />
              </PrivateRoute>
            }
          />
          <Route
            path="/delivery/:orderId"
            element={
              <PrivateRoute>
                <DeliveryStatus />
              </PrivateRoute>
            }
          />
          {/* 관리자 페이지 */}
          <Route
            path="/admin/orders"
            element={
              <PrivateRoute requireRole="admin">
                <AdminOrderManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/inventory"
            element={
              <PrivateRoute requireRole="admin">
                <AdminInventoryManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/accounts"
            element={
              <PrivateRoute requireRole="admin">
                <AdminAccountManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/approvals"
            element={
              <PrivateRoute requireRole="admin">
                <AdminApprovalManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/change-requests"
            element={
              <PrivateRoute requireRole="admin">
                <AdminReservationChangeRequests />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/schedule-management"
            element={
              <PrivateRoute requireRole="admin">
                <AdminScheduleManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/schedule"
            element={
              <PrivateRoute requireRole="admin">
                <AdminScheduleManagement />
              </PrivateRoute>
            }
          />
          {/* 직원 페이지 */}
          <Route
            path="/employee/orders"
            element={
              <PrivateRoute requireRole="employee">
                <EmployeeOrderManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/employee/inventory"
            element={
              <PrivateRoute requireRole="employee">
                <EmployeeInventoryManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/schedule"
            element={
              <PrivateRoute requireRole="employee">
                <ScheduleCalendar />
              </PrivateRoute>
            }
          />
          {/* 기존 라우트 (하위 호환성) */}
          <Route
            path="/employee"
            element={
              <PrivateRoute requireRole="employee">
                <EmployeeOrderManagement />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <PrivateRoute requireRole="admin">
                <AdminOrderManagement />
              </PrivateRoute>
            }
          />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;

