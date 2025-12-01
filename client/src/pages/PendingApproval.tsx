import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './PendingApproval.css';

const PendingApproval: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <div className="pending-approval-page">
      <div className="pending-approval-container">
        <div className="pending-approval-content">
          <div style={{ marginBottom: '20px' }}>
            <button onClick={() => navigate('/login')} className="btn btn-secondary">
              ← 로그인으로
            </button>
          </div>
          <h1 className="logo">미스터 대박</h1>
          <div className="pending-message">
            <h2>승인 중입니다</h2>
            <p>관리자 승인 후 서비스를 이용하실 수 있습니다.</p>
            <p className="user-info">안녕하세요, {user?.name}님</p>
          </div>
          <button onClick={logout} className="btn btn-primary">
            로그아웃
          </button>
        </div>
      </div>
    </div>
  );
};

export default PendingApproval;

