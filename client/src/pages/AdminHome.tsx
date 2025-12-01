import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './AdminHome.css';

const AdminHome: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleOrderManagement = () => {
    navigate('/admin/orders');
  };

  const handleInventory = () => {
    navigate('/admin/inventory');
  };

  const handleAccountManagement = () => {
    navigate('/admin/accounts');
  };

  const handleAccountApproval = () => {
    navigate('/admin/approvals');
  };

  const handleChangeRequests = () => {
    navigate('/admin/change-requests');
  };

  return (
    <div className="admin-home">
      <TopLogo showBackButton={false} />
      <div className="home-content">
        <div className="home-grid admin-grid">
          <div className="grid-item grid-item-1">
            <div className="grid-item-content">
              <h2>미스터 대박 서비스</h2>
              <p>관리자 페이지입니다</p>
            </div>
          </div>
          <div className="grid-item grid-item-2" onClick={handleOrderManagement}>
            <div className="grid-item-content">
              <div className="grid-icon">📋</div>
              <h3>주문 관리</h3>
            </div>
          </div>
          <div className="grid-item grid-item-3" onClick={handleChangeRequests}>
            <div className="grid-item-content">
              <div className="grid-icon">📝</div>
              <h3>예약 변경 요청</h3>
            </div>
          </div>
          <div className="grid-item grid-item-3" onClick={handleInventory}>
            <div className="grid-item-content">
              <div className="grid-icon">📦</div>
              <h3>재고 관리</h3>
            </div>
          </div>
          <div className="grid-item grid-item-4" onClick={() => navigate('/admin/schedule-management')}>
            <div className="grid-item-content">
              <div className="grid-icon">📅</div>
              <h3>스케줄 캘린더</h3>
            </div>
          </div>
          <div className="grid-item grid-item-5" onClick={handleAccountManagement}>
            <div className="grid-item-content">
              <div className="grid-icon">👥</div>
              <h3>계정 관리</h3>
            </div>
          </div>
          <div className="grid-item grid-item-6" onClick={() => navigate('/profile')}>
            <div className="grid-item-content">
              <div className="grid-icon">👤</div>
              <h3>내정보</h3>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminHome;

