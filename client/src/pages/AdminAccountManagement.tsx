import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface User {
  id: number;
  email: string;
  name: string;
  address: string;
  phone: string;
  role: string;
}

const AdminAccountManagement: React.FC = () => {
  const navigate = useNavigate();
  const [users, setUsers] = useState<User[]>([]);
  const [filter, setFilter] = useState<string>('all');
  const [loading, setLoading] = useState(true);
  const [userError, setUserError] = useState('');
  const [promotingUserId, setPromotingUserId] = useState<number | null>(null);
  const [pendingApprovals, setPendingApprovals] = useState<any[]>([]);
  const [pendingLoading, setPendingLoading] = useState(false);
  const [pendingError, setPendingError] = useState('');
  const [activeTab, setActiveTab] = useState<'accounts' | 'approvals'>('accounts');
  const [accountTypeFilter, setAccountTypeFilter] = useState<'customer' | 'employee' | 'admin'>('customer');
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | null>(null);
  const [customerOrders, setCustomerOrders] = useState<any[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);

  useEffect(() => {
    if (activeTab === 'accounts') {
      fetchUsers();
    } else {
      fetchPendingApprovals();
    }
  }, [activeTab]);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      throw new Error('관리자 로그인이 필요합니다.');
    }
    return {
      Authorization: `Bearer ${token}`
    };
  };

  const fetchUsers = async () => {
    try {
      setLoading(true);
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/admin/users`, { headers });
      setUsers(response.data);
      setUserError('');
    } catch (err: any) {
      setUserError(err.message || '회원 정보를 불러오는데 실패했습니다.');
      if (err.response?.status === 403) {
        setUserError('관리자 권한이 필요합니다.');
      }
    } finally {
      setLoading(false);
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
    if (!window.confirm('이 사용자의 가입을 거부하시겠습니까?')) {
      return;
    }
    try {
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/admin/reject-user/${userId}`, {}, { headers });
      await fetchPendingApprovals();
    } catch (err: any) {
      setPendingError(err.response?.data?.error || err.message || '거부에 실패했습니다.');
    }
  };

  const handlePromoteToAdmin = async (userId: number) => {
    if (!window.confirm('관리자로 승급하시겠습니까?')) {
      return;
    }
    
    try {
      setPromotingUserId(userId);
      const headers = getAuthHeaders();
      await axios.patch(`${API_URL}/admin/users/${userId}/promote`, {}, { headers });
      await fetchUsers(); // Refresh user list
      setUserError('');
    } catch (err: any) {
      setUserError(err.response?.data?.error || err.message || '관리자 승급에 실패했습니다.');
    } finally {
      setPromotingUserId(null);
    }
  };

  const filteredUsers = filter === 'all'
    ? users
    : users.filter(user => user.role === filter);

  const fetchCustomerOrders = async (customerId: number) => {
    try {
      setOrdersLoading(true);
      const headers = getAuthHeaders();
      // Admin can view all orders, so we'll use a different endpoint or pass customer ID
      const response = await axios.get(`${API_URL}/admin/users/${customerId}/orders`, { headers });
      setCustomerOrders(response.data || []);
    } catch (err: any) {
      console.error('고객 주문 내역 조회 실패:', err);
      setCustomerOrders([]);
    } finally {
      setOrdersLoading(false);
    }
  };

  const stats = {
    total: users.length,
    customers: users.filter(u => u.role === 'customer').length,
    employees: users.filter(u => u.role === 'employee').length,
    admins: users.filter(u => u.role === 'admin').length
  };

  if (loading) {
    return (
      <div className="admin-dashboard">
        <TopLogo />
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

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
          <div style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
            <button
              className={`btn ${activeTab === 'accounts' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveTab('accounts')}
            >
              계정 관리
            </button>
            <button
              className={`btn ${activeTab === 'approvals' ? 'btn-primary' : 'btn-secondary'}`}
              onClick={() => setActiveTab('approvals')}
            >
              계정 승인
            </button>
          </div>

          {activeTab === 'accounts' && (
            <>
              <h2>계정 관리</h2>
              {userError && <div className="error">{userError}</div>}

              {/* 계정 타입 탭 */}
              <div style={{ display: 'flex', gap: '10px', marginBottom: '20px', borderBottom: '2px solid #d4af37', paddingBottom: '10px' }}>
                <button
                  className={`btn ${accountTypeFilter === 'customer' ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => {
                    setAccountTypeFilter('customer');
                    setSelectedCustomerId(null);
                    setCustomerOrders([]);
                  }}
                >
                  고객 계정
                </button>
                <button
                  className={`btn ${accountTypeFilter === 'employee' ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => {
                    setAccountTypeFilter('employee');
                    setSelectedCustomerId(null);
                    setCustomerOrders([]);
                  }}
                >
                  직원 계정
                </button>
                <button
                  className={`btn ${accountTypeFilter === 'admin' ? 'btn-primary' : 'btn-secondary'}`}
                  onClick={() => {
                    setAccountTypeFilter('admin');
                    setSelectedCustomerId(null);
                    setCustomerOrders([]);
                  }}
                >
                  관리자 계정
                </button>
              </div>

              {selectedCustomerId ? (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                    <h3>주문 내역</h3>
                    <button
                      className="btn btn-secondary"
                      onClick={() => {
                        setSelectedCustomerId(null);
                        setCustomerOrders([]);
                      }}
                    >
                      ← 계정 목록으로
                    </button>
                  </div>
                  {ordersLoading ? (
                    <div className="loading">로딩 중...</div>
                  ) : customerOrders.length === 0 ? (
                    <div className="no-orders">
                      <p>주문 내역이 없습니다.</p>
                    </div>
                  ) : (
                    <div className="inventory-list">
                      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
                        <thead>
                          <tr style={{ background: '#d4af37', color: '#000' }}>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>주문 ID</th>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>디너</th>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>배달 시간</th>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>배달 주소</th>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>총 금액</th>
                            <th style={{ padding: '10px', border: '1px solid #000' }}>상태</th>
                          </tr>
                        </thead>
                        <tbody>
                          {customerOrders.map((order: any) => (
                            <tr key={order.id}>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{order.id}</td>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{order.dinner_name || '-'}</td>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                                {new Date(order.delivery_time).toLocaleString('ko-KR')}
                              </td>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{order.delivery_address}</td>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{order.total_price?.toLocaleString()}원</td>
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{order.status}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              ) : (
                <div className="inventory-list">
                  {users.filter(u => u.role === accountTypeFilter).length === 0 ? (
                    <div className="no-orders">
                      <p>{accountTypeFilter === 'customer' ? '고객' : accountTypeFilter === 'employee' ? '직원' : '관리자'} 계정이 없습니다.</p>
                    </div>
                  ) : (
                    <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
                      <thead>
                        <tr style={{ background: '#d4af37', color: '#000' }}>
                          <th style={{ padding: '10px', border: '1px solid #000' }}>ID</th>
                          <th style={{ padding: '10px', border: '1px solid #000' }}>이름</th>
                          <th style={{ padding: '10px', border: '1px solid #000' }}>이메일</th>
                          <th style={{ padding: '10px', border: '1px solid #000' }}>전화번호</th>
                          <th style={{ padding: '10px', border: '1px solid #000' }}>주소</th>
                          {accountTypeFilter === 'customer' && (
                            <th style={{ padding: '10px', border: '1px solid #000' }}>주문 내역</th>
                          )}
                          {accountTypeFilter === 'employee' && (
                            <th style={{ padding: '10px', border: '1px solid #000' }}>작업</th>
                          )}
                        </tr>
                      </thead>
                      <tbody>
                        {users.filter(u => u.role === accountTypeFilter).map(user => (
                          <tr key={user.id}>
                            <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{user.id}</td>
                            <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{user.name}</td>
                            <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{user.email}</td>
                            <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{user.phone}</td>
                            <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{user.address}</td>
                            {accountTypeFilter === 'customer' && (
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                                <button
                                  className="btn btn-primary"
                                  onClick={() => {
                                    setSelectedCustomerId(user.id);
                                    fetchCustomerOrders(user.id);
                                  }}
                                  style={{ padding: '5px 10px', fontSize: '12px' }}
                                >
                                  주문 내역 보기
                                </button>
                              </td>
                            )}
                            {accountTypeFilter === 'employee' && (
                              <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                                <button
                                  className="btn btn-primary"
                                  onClick={() => handlePromoteToAdmin(user.id)}
                                  disabled={promotingUserId === user.id}
                                  style={{ padding: '5px 10px', fontSize: '12px' }}
                                >
                                  {promotingUserId === user.id ? '처리 중...' : '관리자 승급'}
                                </button>
                              </td>
                            )}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  )}
                </div>
              )}
            </>
          )}

          {activeTab === 'approvals' && (
            <>
              <h2>계정 승인</h2>
              {pendingError && <div className="error">{pendingError}</div>}
              {pendingLoading ? (
                <div className="loading">로딩 중...</div>
              ) : pendingApprovals.length === 0 ? (
                <p style={{ textAlign: 'center', padding: '20px' }}>승인 대기 중인 계정이 없습니다.</p>
              ) : (
                <div className="users-table">
                  <table>
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>이름</th>
                        <th>이메일</th>
                        <th>전화번호</th>
                        <th>역할</th>
                        <th>작업</th>
                      </tr>
                    </thead>
                    <tbody>
                      {pendingApprovals.map(user => (
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
                          <td>
                            <div style={{ display: 'flex', gap: '5px' }}>
                              <button
                                className="btn btn-success"
                                onClick={() => handleApproveUser(user.id)}
                                style={{ padding: '5px 10px', fontSize: '12px' }}
                              >
                                승인
                              </button>
                              <button
                                className="btn btn-danger"
                                onClick={() => handleRejectUser(user.id)}
                                style={{ padding: '5px 10px', fontSize: '12px' }}
                              >
                                거부
                              </button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminAccountManagement;

