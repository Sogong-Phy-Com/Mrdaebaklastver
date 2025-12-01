import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import './Login.css';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      await login(email, password);
      // Check user role and approval status after login
      const loggedInUser = JSON.parse(localStorage.getItem('user') || '{}');
      
      // 승인 대기 상태면 홈으로 이동 (승인 대기 화면 표시)
      if (loggedInUser.approvalStatus === 'pending') {
        navigate('/');
        return;
      }
      
      // 모든 사용자는 메인 페이지로 이동
      navigate('/');
    } catch (err: any) {
      setError(err.message);
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <h1>미스터 대박</h1>
        <h2>특별한 날의 특별한 디너</h2>
        <form onSubmit={handleSubmit} className="login-form">
          <div className="form-group">
            <label>이메일</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-group">
            <label>비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
          {error && <div className="error">{error}</div>}
          <button type="submit" className="btn btn-primary">로그인</button>
          <p className="register-link">
            계정이 없으신가요? <Link to="/register">회원가입</Link>
          </p>
          <p className="register-link">
            <Link to="/forgot-password">비밀번호를 잊으셨나요?</Link>
          </p>
          <div style={{ 
            marginTop: '30px', 
            padding: '20px', 
            background: 'linear-gradient(135deg, #FFD700 0%, #FFA500 100%)', 
            borderRadius: '12px', 
            textAlign: 'center',
            color: '#000',
            fontWeight: 'bold',
            boxShadow: '0 4px 15px rgba(255, 215, 0, 0.3)'
          }}>
            <div style={{ fontSize: '24px', marginBottom: '10px' }}>🎉</div>
            <div style={{ fontSize: '18px', marginBottom: '5px' }}>특별한 날의 특별한 디너</div>
            <div style={{ fontSize: '14px', opacity: 0.8 }}>미스터 대박과 함께하세요</div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;

