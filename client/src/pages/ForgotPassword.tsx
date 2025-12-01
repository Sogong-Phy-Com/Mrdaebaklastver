import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import './Login.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState<'email' | 'question' | 'reset'>('email');
  const [email, setEmail] = useState('');
  const [securityQuestion, setSecurityQuestion] = useState('');
  const [securityAnswer, setSecurityAnswer] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');

  const handleEmailSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    try {
      const response = await axios.get(`${API_URL}/auth/user/${encodeURIComponent(email)}/security-question`);
      setSecurityQuestion(response.data.securityQuestion || '');
      if (response.data.securityQuestion) {
        setStep('question');
      } else {
        setError('보안 질문을 찾을 수 없습니다.');
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '오류가 발생했습니다.';
      if (errorMsg.includes('존재하지 않는')) {
        setError('존재하지 않는 계정입니다');
      } else {
        setError(errorMsg);
      }
    }
  };

  const handleQuestionSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!securityAnswer.trim()) {
      setError('답변을 입력해주세요.');
      return;
    }

    setStep('reset');
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!newPassword || newPassword.length < 6) {
      setError('비밀번호는 최소 6자 이상이어야 합니다.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }

    try {
      await axios.post(`${API_URL}/auth/forgot-password`, {
        email,
        securityQuestion,
        securityAnswer,
        newPassword
      });

      alert('비밀번호가 변경되었습니다. 로그인해주세요.');
      navigate('/login');
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '오류가 발생했습니다.';
      if (errorMsg.includes('답변이 올바르지 않습니다')) {
        setError('보안 질문 답변이 올바르지 않습니다');
      } else {
        setError(errorMsg);
      }
    }
  };

  return (
    <div className="login-page">
      <div className="login-container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/login')} className="btn btn-secondary">
            ← 로그인으로
          </button>
        </div>
        <h1>비밀번호 찾기</h1>
        
        {step === 'email' && (
          <form onSubmit={handleEmailSubmit} className="login-form">
            <div className="form-group">
              <label>이메일</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                placeholder="가입하신 이메일을 입력하세요"
              />
            </div>
            {error && <div className="error">{error}</div>}
            <button type="submit" className="btn btn-primary">다음</button>
            <p className="register-link">
              <Link to="/login">로그인으로 돌아가기</Link>
            </p>
          </form>
        )}

        {step === 'question' && (
          <form onSubmit={handleQuestionSubmit} className="login-form">
            <div className="form-group">
              <label>보안 질문</label>
              <div style={{ padding: '15px', background: '#1a1a1a', borderRadius: '8px', marginBottom: '15px', color: '#FFD700', border: '1px solid #d4af37' }}>
                {securityQuestion || '보안 질문을 불러올 수 없습니다.'}
              </div>
            </div>
            <div className="form-group">
              <label>답변</label>
              <input
                type="text"
                value={securityAnswer}
                onChange={(e) => setSecurityAnswer(e.target.value)}
                required
                placeholder="답변을 입력하세요"
              />
            </div>
            {error && <div className="error">{error}</div>}
            <button type="submit" className="btn btn-primary">다음</button>
            <button
              type="button"
              onClick={() => {
                setStep('email');
                setSecurityAnswer('');
                setError('');
              }}
              className="btn btn-secondary"
              style={{ marginTop: '10px' }}
            >
              이전
            </button>
          </form>
        )}

        {step === 'reset' && (
          <form onSubmit={handleResetPassword} className="login-form">
            <div className="form-group">
              <label>새 비밀번호 (최소 6자)</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
            <div className="form-group">
              <label>새 비밀번호 확인</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                minLength={6}
              />
            </div>
            {error && <div className="error">{error}</div>}
            <button type="submit" className="btn btn-primary">비밀번호 변경</button>
            <button
              type="button"
              onClick={() => {
                setStep('question');
                setNewPassword('');
                setConfirmPassword('');
                setError('');
              }}
              className="btn btn-secondary"
              style={{ marginTop: '10px' }}
            >
              이전
            </button>
          </form>
        )}
      </div>
    </div>
  );
};

export default ForgotPassword;

