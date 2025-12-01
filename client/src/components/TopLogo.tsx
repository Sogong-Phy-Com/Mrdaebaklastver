import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './TopLogo.css';

interface TopLogoProps {
  showLogoutButton?: boolean;
  showBackButton?: boolean;
  backLabel?: string;
  backTo?: string | number;
}

const TopLogo: React.FC<TopLogoProps> = ({
  showLogoutButton = true,
  showBackButton = true,
  backLabel = '← 이전 페이지',
  backTo
}) => {
  const navigate = useNavigate();
  const { logout } = useAuth();

  const handleBack = () => {
    if (typeof backTo === 'number') {
      navigate(backTo);
      return;
    }
    if (typeof backTo === 'string') {
      navigate(backTo);
      return;
    }
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate('/');
    }
  };

  return (
    <div className="top-logo-wrapper">
      <div className="top-logo-container">
        <h1 className="top-logo" onClick={() => navigate('/')}>
          미스터 대박 서비스
        </h1>
        {showLogoutButton && (
          <button 
            onClick={logout} 
            className="btn btn-secondary"
            style={{ 
              marginLeft: 'auto',
              padding: '8px 16px',
              fontSize: '14px'
            }}
          >
            로그아웃
          </button>
        )}
      </div>
      {showBackButton && (
        <div className="top-logo-back">
          <button className="btn btn-secondary back-under-logo" onClick={handleBack}>
            {backLabel}
          </button>
        </div>
      )}
    </div>
  );
};

export default TopLogo;

