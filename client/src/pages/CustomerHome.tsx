import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import './Home.css';

const CustomerHome: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="home-page">
      <TopLogo showBackButton={false} />
      <div className="home-content">
        <div className="home-grid customer-grid">
          <div className="grid-item grid-item-1" onClick={() => navigate('/')}>
            <div className="grid-item-content">
              <h2>미스터 대박 서비스</h2>
              <p>특별한 날의 특별한 디너</p>
            </div>
          </div>
          <div className="grid-item grid-item-2" onClick={() => navigate('/order')}>
            <div className="grid-item-content">
              <div className="grid-icon">🛒</div>
              <h3>주문하기</h3>
            </div>
          </div>
          <div className="grid-item grid-item-2b" onClick={() => navigate('/voice-order')}>
            <div className="grid-item-content">
              <div className="grid-icon">🎙️</div>
              <h3>음성 주문</h3>
            </div>
          </div>
          <div className="grid-item grid-item-3" onClick={() => navigate('/orders')}>
            <div className="grid-item-content">
              <div className="grid-icon">📋</div>
              <h3>주문 내역</h3>
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

export default CustomerHome;

