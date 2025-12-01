import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

const AdminApprovalManagement: React.FC = () => {
  const navigate = useNavigate();
  const [pendingApprovals, setPendingApprovals] = useState<any[]>([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [pendingError, setPendingError] = useState('');

  useEffect(() => {
    fetchPendingApprovals();
  }, []);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('관리자 로그인이 필요합니다.');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchPendingApprovals = async () => {
    try {
      setPendingLoading(true);
      setPendingError('');
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/pending-approvals`, { headers });
      setPendingApprovals(response.data);
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '승인 대기 목록을 불러오는데 실패했습니다.');
      setPendingApprovals([]);
    } finally {
      setPendingLoading(false);
    }
  };

  const handleApproveUser = async (userId: number) => {
    try {
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/admin/approve-user/${userId}`, {}, { headers });
      await fetchPendingApprovals();
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '승인에 실패했습니다.');
    }
  };

  const handleRejectUser = async (userId: number) => {
    try {
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/admin/reject-user/${userId}`, {}, { headers });
      await fetchPendingApprovals();
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '거부에 실패했습니다.');
    }
  };

  const getRoleLabel = (role: string) => {
    const labels: { [key: string]: string } = {
      customer: '고객',
      employee: '직원',
      admin: '관리자'
    };
    return labels[role] || role;
  };

  const getRoleClass = (role: string) => {
    const classes: { [key: string]: string } = {
      customer: 'role-customer',
      employee: 'role-employee',
      admin: 'role-admin'
    };
    return classes[role] || '';
  };

  return (
    <div className="admin-dashboard">
      <TopLogo showBackButton={false} />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <div className="admin-section">
          <h2>승인 대기</h2>
          {pendingError && <div className="error">{pendingError}</div>}
          {pendingLoading ? (
            <div className="loading">로딩 중...</div>
          ) : (
            <div className="users-table">
              {pendingApprovals.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '20px' }}>승인 대기 중인 사용자가 없습니다.</p>
              ) : (
                <table>
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>이름</th>
                      <th>이메일</th>
                      <th>전화번호</th>
                      <th>역할</th>
                      <th>가입일</th>
                      <th>작업</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pendingApprovals.map((user: any) => (
                      <tr key={user.id}>
                        <td>{user.id}</td>
                        <td>{user.name}</td>
                        <td>{user.email}</td>
                        <td>{user.phone}</td>
                        <td>
                          <span className={`role-badge ${getRoleClass(user.role)}`}>
                            {getRoleLabel(user.role)}
                          </span>
                        </td>
                        <td>{user.createdAt ? new Date(user.createdAt).toLocaleDateString('ko-KR') : '-'}</td>
                        <td>
                          <button
                            onClick={() => handleApproveUser(user.id)}
                            className="btn btn-primary"
                            style={{ marginRight: '10px', padding: '5px 10px', fontSize: '12px' }}
                          >
                            승인
                          </button>
                          <button
                            onClick={() => handleRejectUser(user.id)}
                            className="btn btn-secondary"
                            style={{ padding: '5px 10px', fontSize: '12px' }}
                          >
                            거부
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminApprovalManagement;

