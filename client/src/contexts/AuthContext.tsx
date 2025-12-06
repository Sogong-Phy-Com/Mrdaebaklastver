import React, { createContext, useContext, useState, useEffect, ReactNode, useCallback } from 'react';
import axios from 'axios';

interface User {
  id: number;
  email: string;
  name: string | null;
  address: string | null;
  phone: string | null;
  role: string;
  approvalStatus?: string;
  cardNumber?: string;
  cardExpiry?: string;
  cardCvv?: string;
  cardHolderName?: string;
  hasCard?: boolean;
  consent?: boolean;
  loyaltyConsent?: boolean;
}

interface RegisterOptions {
  consent?: boolean;
  loyaltyConsent?: boolean;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (
    email: string,
    password: string,
    name: string,
    address: string,
    phone: string,
    role?: string,
    securityQuestion?: string,
    securityAnswer?: string,
    options?: RegisterOptions
  ) => Promise<void>;
  logout: () => void;
  updateUser: (updatedUser: User) => void;
  loading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Use relative path for production (same domain), or environment variable
const API_URL = process.env.REACT_APP_API_URL || (window.location.protocol === 'https:' ? '/api' : 'http://localhost:5000/api');

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    delete axios.defaults.headers.common['Authorization'];
  }, []);

  const fetchCurrentUser = useCallback(async (tokenOverride?: string) => {
    const activeToken = tokenOverride || token || localStorage.getItem('token');
    if (!activeToken) {
      return;
    }
    try {
      const response = await axios.get(`${API_URL}/auth/me`, {
        headers: {
          Authorization: `Bearer ${activeToken}`
        }
      });
      setUser(response.data);
      localStorage.setItem('user', JSON.stringify(response.data));
    } catch (error) {
      console.error('[AuthContext] 사용자 정보 조회 실패:', error);
      logout();
    }
  }, [logout, token]);

  useEffect(() => {
    const storedToken = localStorage.getItem('token');
    const storedUser = localStorage.getItem('user');

    console.log('[AuthContext] 초기화 - 토큰:', storedToken ? `존재 (길이: ${storedToken.length})` : '없음');
    console.log('[AuthContext] 초기화 - 사용자:', storedUser ? '존재' : '없음');

    if (storedToken) {
      setToken(storedToken);
      axios.defaults.headers.common['Authorization'] = `Bearer ${storedToken}`;
      console.log('[AuthContext] axios 기본 헤더에 토큰 설정 완료');
      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }
      fetchCurrentUser(storedToken).finally(() => setLoading(false));
      return;
    }
    console.log('[AuthContext] 토큰 또는 사용자 정보가 없어서 axios 헤더를 설정하지 않음');
    setLoading(false);
  }, [fetchCurrentUser]);

  const login = async (email: string, password: string) => {
    try {
      const response = await axios.post(`${API_URL}/auth/login`, { email, password });
      const { token: newToken, user: newUser, message } = response.data;
      
      // 승인 대기 상태면 토큰이 없을 수 있음
      if (newToken) {
        setToken(newToken);
        setUser(newUser);
        localStorage.setItem('token', newToken);
        localStorage.setItem('user', JSON.stringify(newUser));
        axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
        await fetchCurrentUser(newToken);
      } else {
        // 승인 대기 상태
        throw new Error(message || '회원가입이 완료되었습니다. 관리자 승인 후 로그인할 수 있습니다.');
      }
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Login failed');
    }
  };

  const register = async (
    email: string,
    password: string,
    name: string,
    address: string,
    phone: string,
    role?: string,
    securityQuestion?: string,
    securityAnswer?: string,
    options?: RegisterOptions
  ) => {
    try {
      const response = await axios.post(`${API_URL}/auth/register`, {
        email,
        password,
        name,
        address,
        phone,
        role: role || 'customer',
        securityQuestion: securityQuestion || '',
        securityAnswer: securityAnswer || '',
        consent: options?.consent ?? false,
        loyaltyConsent: options?.loyaltyConsent ?? false
      });
      const { token: newToken, user: newUser } = response.data;
      
      if (newToken) {
        setToken(newToken);
        setUser(newUser);
        localStorage.setItem('token', newToken);
        localStorage.setItem('user', JSON.stringify(newUser));
        axios.defaults.headers.common['Authorization'] = `Bearer ${newToken}`;
        await fetchCurrentUser(newToken);
      } else {
        setUser(newUser);
      }
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Registration failed');
    }
  };

  const updateUser = (updatedUser: User) => {
    setUser(updatedUser);
    localStorage.setItem('user', JSON.stringify(updatedUser));
  };

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, updateUser, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

