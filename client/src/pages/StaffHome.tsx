import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './StaffHome.css';

const StaffHome: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const handleOrderManagement = () => {
    navigate('/employee/orders');
  };

  const handleInventory = () => {
    navigate('/employee/inventory');
  };

  return (
    <div className="staff-home">
      <TopLogo showBackButton={false} />
      <div className="home-content">
        <div className="home-grid employee-grid">
          <div className="grid-item grid-item-1">
            <div className="grid-item-content">
              <h2>미스터 대박 서비스</h2>
              <p>직원 페이지입니다</p>
            </div>
          </div>
          <div className="grid-item grid-item-2" onClick={handleOrderManagement}>
            <div className="grid-item-content">
              <div className="grid-icon">📅</div>
              <h3>스케줄 관리</h3>
            </div>
          </div>
          <div className="grid-item grid-item-3" onClick={handleInventory}>
            <div className="grid-item-content">
              <div className="grid-icon">📦</div>
              <h3>재고 관리</h3>
            </div>
          </div>
          <div className="grid-item grid-item-4" onClick={() => navigate('/profile')}>
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

export default StaffHome;




