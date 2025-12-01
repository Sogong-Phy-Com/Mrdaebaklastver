import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopLogo from '../components/TopLogo';

const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

interface InventoryItem {
  menu_item_id: number;
  menu_item_name?: string;
  menu_item_name_en?: string;
  category?: string;
  capacity_per_window: number;
  reserved: number;
  remaining: number;
  weekly_reserved?: number;
  ordered_quantity?: number;
  window_start: string;
  window_end: string;
  notes?: string;
}

const AdminInventoryManagement: React.FC = () => {
  const navigate = useNavigate();
  const [inventoryItems, setInventoryItems] = useState<InventoryItem[]>([]);
  const [inventoryLoading, setInventoryLoading] = useState(false);
  const [inventoryError, setInventoryError] = useState('');
  const [restockValues, setRestockValues] = useState<Record<number, number | ''>>({});
  const [orderedInventory, setOrderedInventory] = useState<Record<number, number>>({});
  const [restockMessage, setRestockMessage] = useState('');
  const [selectedItems, setSelectedItems] = useState<Set<number>>(new Set());
  const [bulkRestockValue, setBulkRestockValue] = useState<number | ''>('');

  useEffect(() => {
    fetchInventory();
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

  const fetchInventory = async () => {
    try {
      setInventoryLoading(true);
      setInventoryError('');
      const headers = getAuthHeaders();
      const response = await axios.get(`${API_URL}/inventory`, { headers });
      if (response.data && Array.isArray(response.data)) {
        setInventoryItems(response.data);
        const defaultValues: Record<number, number | ''> = {};
        const orderedInv: Record<number, number> = {};
        response.data.forEach((item: InventoryItem) => {
          defaultValues[item.menu_item_id] = ''; // Empty by default
          orderedInv[item.menu_item_id] = item.ordered_quantity || 0;
        });
        setRestockValues(defaultValues);
        setOrderedInventory(orderedInv);
      } else {
        setInventoryItems([]);
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '재고 정보를 불러오는데 실패했습니다.';
      setInventoryError(errorMsg);
      setInventoryItems([]);
    } finally {
      setInventoryLoading(false);
    }
  };

  const handleRestock = async (menuItemId: number) => {
    const restockValue = restockValues[menuItemId];
    
    // 보충 수량이 비어있으면 주문 불가
    if (restockValue === '' || restockValue === 0) {
      alert('보충 수량을 입력해주세요.');
      return;
    }
    
    // 보충 수량을 주문 재고로 설정
    const ordered = Number(restockValue);
    
    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      // Save ordered inventory
      await axios.post(`${API_URL}/inventory/${menuItemId}/order`, {
        ordered_quantity: ordered
      }, { headers });
      
      setOrderedInventory(prev => ({ ...prev, [menuItemId]: ordered }));
      setRestockValues(prev => ({ ...prev, [menuItemId]: '' }));
      setRestockMessage('주문 재고가 저장되었습니다.');
      setTimeout(() => setRestockMessage(''), 3000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '주문 재고 저장에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const handleReceiveInventory = async (menuItemId: number) => {
    if (!window.confirm('주문한 재고를 수령하시겠습니까? 수령 후 주문 재고가 현재 보유량에 추가되고 주문 재고는 0으로 초기화됩니다.')) {
      return;
    }

    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      await axios.post(`${API_URL}/inventory/${menuItemId}/receive`, {}, { headers });
      
      setOrderedInventory(prev => ({ ...prev, [menuItemId]: 0 }));
      setRestockMessage('재고 수령이 완료되었습니다.');
      setTimeout(() => setRestockMessage(''), 3000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '재고 수령에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const handleBulkRestock = async () => {
    if (selectedItems.size === 0) {
      alert('보충할 항목을 선택해주세요.');
      return;
    }

    if (bulkRestockValue === '' || bulkRestockValue === 0) {
      alert('보충 수량을 입력해주세요.');
      return;
    }

    try {
      setRestockMessage('');
      const headers = getAuthHeaders();
      let successCount = 0;
      let failCount = 0;

      const promises = Array.from(selectedItems).map(async (menuItemId) => {
        try {
          const currentCapacity = inventoryItems.find(item => item.menu_item_id === menuItemId)?.capacity_per_window || 0;
          const ordered = Math.max(0, Number(bulkRestockValue) - currentCapacity);
          
          await axios.post(`${API_URL}/inventory/${menuItemId}/order`, {
            ordered_quantity: ordered
          }, { headers });
          
          setOrderedInventory(prev => ({ ...prev, [menuItemId]: ordered }));
          successCount++;
          return { success: true, menuItemId };
        } catch (err: any) {
          console.error(`재고 보충 실패 (메뉴 ID: ${menuItemId}):`, err);
          failCount++;
          return { success: false, menuItemId };
        }
      });

      await Promise.all(promises);

      setSelectedItems(new Set());
      setBulkRestockValue('');
      setRestockMessage(`${successCount}개 항목의 주문 재고가 저장되었습니다.${failCount > 0 ? ` (${failCount}개 실패)` : ''}`);
      setTimeout(() => setRestockMessage(''), 5000);
      await fetchInventory();
    } catch (err: any) {
      const errorMsg = err.response?.data?.error || err.message || '일괄 보충에 실패했습니다.';
      setRestockMessage(errorMsg);
      setTimeout(() => setRestockMessage(''), 5000);
    }
  };

  const toggleItemSelection = (menuItemId: number) => {
    setSelectedItems(prev => {
      const newSet = new Set(prev);
      if (newSet.has(menuItemId)) {
        newSet.delete(menuItemId);
      } else {
        newSet.add(menuItemId);
      }
      return newSet;
    });
  };

  const selectAllItems = () => {
    if (selectedItems.size === inventoryItems.length) {
      setSelectedItems(new Set());
    } else {
      setSelectedItems(new Set(inventoryItems.map(item => item.menu_item_id)));
    }
  };

  const formatDateTime = (value: string) => {
    return new Date(value).toLocaleString('ko-KR', { hour12: false });
  };

  return (
    <div className="employee-dashboard">
      <TopLogo showBackButton={false} />
      <div className="container">
        <div style={{ marginBottom: '20px' }}>
          <button onClick={() => navigate('/')} className="btn btn-secondary">
            ← 홈으로
          </button>
        </div>

        <h2>재고 관리</h2>
        {inventoryError && <div className="error">{inventoryError}</div>}
        {restockMessage && <div className="success">{restockMessage}</div>}
        
        {inventoryLoading ? (
          <div className="loading">로딩 중...</div>
        ) : (
          <div className="inventory-list">
            {inventoryItems.length === 0 ? (
              <div className="no-orders">
                <p>재고 정보가 없습니다.</p>
              </div>
            ) : (
              <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: '20px' }}>
                <thead>
                  <tr style={{ background: '#d4af37', color: '#000' }}>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>메뉴 항목</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>카테고리</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>현재 보유량</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>주문 재고</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>이번주 예약 수량</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>예비 수량</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>보충일</th>
                    <th style={{ padding: '10px', border: '1px solid #000' }}>보충</th>
                  </tr>
                </thead>
                <tbody>
                  {inventoryItems.map(item => {
                    const currentCapacity = item.capacity_per_window || 1; // 기본값 1
                    const weeklyReserved = item.weekly_reserved || item.reserved || 0;
                    const spareQuantity = Math.max(0, currentCapacity - weeklyReserved);
                    const orderedQty = orderedInventory[item.menu_item_id] || 0;
                    
                    // 예비 수량이 이번주 수량의 10%를 넘지 않으면 빨간색, 넘으면 초록색
                    const tenPercentThreshold = weeklyReserved * 0.1;
                    const dotColor = spareQuantity <= tenPercentThreshold ? '#ff4444' : '#4CAF50';
                    
                    return (
                      <tr key={item.menu_item_id}>
                        <td style={{ padding: '10px', border: '1px solid #d4af37', position: 'relative' }}>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <div style={{
                              width: '12px',
                              height: '12px',
                              borderRadius: '50%',
                              background: dotColor,
                              border: '1px solid #000',
                              flexShrink: 0
                            }}></div>
                            <span>{item.menu_item_name || `메뉴 ${item.menu_item_id}`} {item.menu_item_name_en && `(${item.menu_item_name_en})`}</span>
                          </div>
                        </td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{item.category || '-'}</td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{currentCapacity.toLocaleString()}</td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{orderedQty.toLocaleString()}</td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>{weeklyReserved.toLocaleString()}</td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37', fontWeight: spareQuantity < 5 ? 'bold' : 'normal' }}>
                          {spareQuantity.toLocaleString()}
                        </td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                          {(() => {
                            const today = new Date();
                            const dayOfWeek = today.getDay();
                            if (dayOfWeek === 1) return '월요일';
                            if (dayOfWeek === 5) return '금요일';
                            const daysUntilMonday = (1 - dayOfWeek + 7) % 7 || 7;
                            const daysUntilFriday = (5 - dayOfWeek + 7) % 7 || 7;
                            const nextRestockDay = Math.min(daysUntilMonday, daysUntilFriday);
                            if (nextRestockDay === daysUntilMonday && nextRestockDay <= daysUntilFriday) return `다음 월요일 (${nextRestockDay}일 후)`;
                            if (nextRestockDay === daysUntilFriday) return `다음 금요일 (${nextRestockDay}일 후)`;
                            return '-';
                          })()}
                        </td>
                        <td style={{ padding: '10px', border: '1px solid #d4af37' }}>
                          <div style={{ display: 'flex', flexDirection: 'column', gap: '5px' }}>
                            <input
                              type="number"
                              min={0}
                              placeholder="보충 수량"
                              value={restockValues[item.menu_item_id] === '' ? '' : (restockValues[item.menu_item_id] || '')}
                              onChange={(e) => {
                                const value = e.target.value === '' ? '' : Number(e.target.value);
                                setRestockValues(prev => ({
                                  ...prev,
                                  [item.menu_item_id]: value
                                }));
                              }}
                              style={{ padding: '5px', width: '100%' }}
                            />
                            <button
                              className="btn btn-primary"
                              onClick={() => handleRestock(item.menu_item_id)}
                              style={{ padding: '5px', fontSize: '12px' }}
                            >
                              주문
                            </button>
                            {orderedQty > 0 && (
                              <button
                                className="btn btn-success"
                                onClick={() => handleReceiveInventory(item.menu_item_id)}
                                style={{ padding: '5px', fontSize: '12px' }}
                              >
                                수령
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminInventoryManagement;

