import React, { useEffect, useState } from 'react';
import axios from 'axios';
import TopLogo from '../components/TopLogo';
import './AdminReservationChangeRequests.css';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface AdminChangeRequest {
  id: number;
  order_id: number;
  status: string;
  original_total_amount: number;
  new_total_amount: number;
  change_fee_amount: number;
  extra_charge_amount: number;
  expected_refund_amount: number;
  requires_additional_payment: boolean;
  requires_refund: boolean;
  new_dinner_type_id: number;
  new_serving_style: string;
  new_delivery_time: string;
  new_delivery_address: string;
  reason?: string;
  admin_comment?: string;
  requested_at: string;
  approved_at?: string;
  rejected_at?: string;
}

const AdminReservationChangeRequests: React.FC = () => {
  const [requests, setRequests] = useState<AdminChangeRequest[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('REQUESTED');
  const [fromDate, setFromDate] = useState('');
  const [toDate, setToDate] = useState('');
  const [decisionComment, setDecisionComment] = useState('');

  useEffect(() => {
    fetchRequests();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter, fromDate, toDate]);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('인증이 필요합니다.');
      }
      const params: any = {};
      if (statusFilter) params.status = statusFilter;
      if (fromDate) params.from = fromDate;
      if (toDate) params.to = toDate;
      const response = await axios.get(`${API_URL}/admin/change-requests`, {
        headers: { 'Authorization': `Bearer ${token}` },
        params
      });
      setRequests(Array.isArray(response.data) ? response.data : []);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || '변경 요청을 불러오지 못했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const mutateRequest = async (id: number, action: 'approve' | 'reject') => {
    try {
      const token = localStorage.getItem('token');
      if (!token) {
        throw new Error('인증이 필요합니다.');
      }
      await axios.post(`${API_URL}/admin/change-requests/${id}/${action}`, {
        adminComment: decisionComment || undefined
      }, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      setDecisionComment('');
      await fetchRequests();
    } catch (err: any) {
      alert(err.response?.data?.error || err.message || '요청 처리에 실패했습니다.');
    }
  };

  const formatStatus = (status: string) => {
    switch (status) {
      case 'APPROVED':
        return '승인됨';
      case 'REJECTED':
        return '거절됨';
      case 'PAYMENT_FAILED':
        return '결제 실패';
      case 'REFUND_FAILED':
        return '환불 실패';
      case 'REQUESTED':
      default:
        return '승인 대기';
    }
  };

  return (
    <div className="admin-change-page">
      <TopLogo showBackButton />
      <div className="admin-change-container">
        <div className="admin-change-header">
          <h2>예약 변경 요청 관리</h2>
          <div className="admin-change-filters">
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              <option value="">전체 상태</option>
              <option value="REQUESTED">승인 대기</option>
              <option value="APPROVED">승인 완료</option>
              <option value="REJECTED">거절됨</option>
              <option value="PAYMENT_FAILED">결제 실패</option>
              <option value="REFUND_FAILED">환불 실패</option>
            </select>
            <input type="date" value={fromDate} onChange={(e) => setFromDate(e.target.value)} />
            <input type="date" value={toDate} onChange={(e) => setToDate(e.target.value)} />
          </div>
        </div>

        {error && <div className="error">{error}</div>}
        {loading ? (
          <div className="loading">변경 요청을 불러오는 중입니다...</div>
        ) : (
          <div className="admin-change-list">
            {requests.map(req => (
              <div key={req.id} className="admin-change-card">
                <div className="admin-change-card__header">
                  <div>
                    <h4>요청 #{req.id}</h4>
                    <div className="admin-change-card__sub">
                      주문 #{req.order_id} · 요청일 {new Date(req.requested_at).toLocaleString('ko-KR')}
                    </div>
                  </div>
                  <span className={`change-status-badge ${req.status.toLowerCase()}`}>
                    {formatStatus(req.status)}
                  </span>
                </div>
                <div className="admin-change-card__body">
                  <div>기존 금액: {req.original_total_amount.toLocaleString()}원</div>
                  <div>새 금액: {req.new_total_amount.toLocaleString()}원</div>
                  {req.change_fee_amount > 0 && <div>변경 수수료: {req.change_fee_amount.toLocaleString()}원</div>}
                  {req.requires_additional_payment && (
                    <div className="change-delta charge">
                      추가 결제 예정: {req.extra_charge_amount.toLocaleString()}원
                    </div>
                  )}
                  {req.requires_refund && (
                    <div className="change-delta refund">
                      환불 예정: {req.expected_refund_amount.toLocaleString()}원
                    </div>
                  )}
                  <div>새 서빙 스타일: {req.new_serving_style}</div>
                  <div>새 배달 시간: {new Date(req.new_delivery_time).toLocaleString('ko-KR')}</div>
                  <div>새 배달 주소: {req.new_delivery_address}</div>
                  {req.reason && <div className="admin-change-card__reason">고객 사유: {req.reason}</div>}
                  {req.admin_comment && req.status !== 'REQUESTED' && (
                    <div className="admin-change-card__comment">관리자 메모: {req.admin_comment}</div>
                  )}
                </div>
                {req.approved_at && (
                  <div className="admin-change-card__foot">
                    처리 시각: {new Date(req.approved_at).toLocaleString('ko-KR')}
                  </div>
                )}
                {req.status === 'REQUESTED' || req.status === 'PAYMENT_FAILED' || req.status === 'REFUND_FAILED' ? (
                  <div className="admin-change-card__actions">
                    <input
                      type="text"
                      placeholder="관리자 메모 (선택)"
                      value={decisionComment}
                      onChange={(e) => setDecisionComment(e.target.value)}
                    />
                    <button className="btn btn-secondary" onClick={() => mutateRequest(req.id, 'reject')}>
                      거절
                    </button>
                    <button className="btn btn-primary" onClick={() => mutateRequest(req.id, 'approve')}>
                      승인
                    </button>
                  </div>
                ) : null}
              </div>
            ))}
            {requests.length === 0 && !loading && !error && (
              <div className="info-banner">조건에 맞는 변경 요청이 없습니다.</div>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminReservationChangeRequests;

