import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAuth } from '../contexts/AuthContext';
import TopLogo from '../components/TopLogo';
import ScheduleCalendar from './ScheduleCalendar';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface OrderItem {
  id: number;
  menu_item_id: number;
  name: string;
  name_en: string;
  price: number;
  quantity: number;
}

interface Order {
  id: number;
  customer_name: string;
  customer_phone: string;
  dinner_name: string;
  dinner_name_en: string;
  serving_style: string;
  delivery_time: string;
  delivery_address: string;
  total_price: number;
  status: string;
  payment_status: string;
  created_at: string;
  delivery_employee_id?: number;
  cooking_employee_id?: number;
  cooking_employee_name?: string;
  delivery_employee_name?: string;
  items: OrderItem[];
  admin_approval_status?: string;
}

const EmployeeOrderManagement: React.FC = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [showSchedule, setShowSchedule] = useState(true);
  const [selectedScheduleOrderId, setSelectedScheduleOrderId] = useState<number | null>(null);

  useEffect(() => {
    fetchOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filterStatus, showSchedule]);

  const fetchOrders = async () => {
    console.log('[EmployeeOrderManagement] 주문 목록 조회 시작');
    
    try {
      const token = localStorage.getItem('token');
      console.log('[EmployeeOrderManagement] 토큰 확인:', token ? '토큰 존재' : '토큰 없음');
      
      if (!token) {
        setError('[에러] 로그인이 필요합니다. (토큰 없음)');
        setLoading(false);
        return;
      }

      const url = filterStatus
        ? `${API_URL}/employee/orders?status=${filterStatus}`
        : `${API_URL}/employee/orders`;
      
      console.log('[EmployeeOrderManagement] API 요청 URL:', url);
      
      const response = await axios.get(url, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      console.log('[EmployeeOrderManagement] API 응답 성공:', response.data);
      setOrders(response.data);
    } catch (err: any) {
      console.error('[EmployeeOrderManagement] 주문 목록 조회 실패');
      console.error('[EmployeeOrderManagement] 에러:', err);
      
      if (err.response) {
        const status = err.response.status;
        const errorData = err.response.data;
        console.error('[EmployeeOrderManagement] HTTP 상태 코드:', status);
        console.error('[EmployeeOrderManagement] 응답 데이터:', errorData);
        
        if (status === 403) {
          const userStr = localStorage.getItem('user');
          const user = userStr ? JSON.parse(userStr) : null;
          setError(`[권한 없음] 직원 권한이 필요합니다. (상태: 403)\n현재 역할: ${user?.role || '알 수 없음'}\n상세: ${JSON.stringify(errorData)}`);
        } else if (status === 401) {
          setError(`[인증 실패] 로그인이 필요합니다. (상태: 401)\n상세: ${JSON.stringify(errorData)}`);
        } else {
          setError(`[오류] 주문 목록을 불러오는데 실패했습니다. (상태: ${status})\n상세: ${JSON.stringify(errorData)}`);
        }
      } else {
        setError('[오류] 주문 목록을 불러오는데 실패했습니다.\n서버에 연결할 수 없습니다.');
      }
    } finally {
      setLoading(false);
    }
  };

  const updateOrderStatus = async (orderId: number, newStatus: string) => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        setError('로그인이 필요합니다.');
        return;
      }

      await axios.patch(`${API_URL}/employee/orders/${orderId}/status`, {
        status: newStatus
      }, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      fetchOrders();
    } catch (err: any) {
      console.error('[EmployeeOrderManagement] 주문 상태 업데이트 실패:', err);
      if (err.response) {
        setError(`주문 상태 업데이트에 실패했습니다. (상태: ${err.response.status})`);
      } else {
        setError('주문 상태 업데이트에 실패했습니다.');
      }
    }
  };

  const getStatusLabel = (status: string) => {
    const labels: { [key: string]: string } = {
      pending: '주문 접수',
      cooking: '조리 중',
      ready: '준비 완료',
      out_for_delivery: '배달 중',
      delivered: '배달 완료',
      cancelled: '취소됨'
    };
    return labels[status] || status;
  };

  const getStatusClass = (status: string) => {
    const classes: { [key: string]: string } = {
      pending: 'status-pending',
      cooking: 'status-cooking',
      ready: 'status-ready',
      out_for_delivery: 'status-delivery',
      delivered: 'status-delivered',
      cancelled: 'status-cancelled'
    };
    return classes[status] || '';
  };

  const getNextStatus = (currentStatus: string): string | null => {
    const statusFlow: { [key: string]: string } = {
      pending: 'cooking',
      cooking: 'ready',
      ready: 'out_for_delivery',
      out_for_delivery: 'delivered'
    };
    return statusFlow[currentStatus] || null;
  };

  if (loading) {
    return (
      <div className="employee-dashboard">
        <TopLogo />
        <div className="loading">로딩 중...</div>
      </div>
    );
  }

  return (
    <div className="employee-dashboard">
      <TopLogo showBackButton={false} />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <h2>스케줄 관리</h2>

        {/* Tab Menu */}
        <div style={{ 
          display: 'flex', 
          gap: '10px', 
          marginBottom: '20px',
          borderBottom: '2px solid #FFD700',
          paddingBottom: '10px'
        }}>
          <button
            className={`btn ${showSchedule ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setShowSchedule(true)}
            style={{
              borderBottom: showSchedule ? '3px solid #FFD700' : 'none',
              borderRadius: '4px 4px 0 0'
            }}
          >
            📅 스케줄 캘린더
          </button>
          <button
            className={`btn ${!showSchedule ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setShowSchedule(false)}
            style={{
              borderBottom: !showSchedule ? '3px solid #FFD700' : 'none',
              borderRadius: '4px 4px 0 0'
            }}
          >
            📋 주문 캘린더
          </button>
        </div>

        {showSchedule ? (
          <div>
            <ScheduleCalendar type="schedule" />
          </div>
        ) : (
          <div>
            <ScheduleCalendar type="orders" />
          </div>
        )}

      </div>
    </div>
  );
};

export default EmployeeOrderManagement;

