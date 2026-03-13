import axios from 'axios';
import { clearStoredAuth, getStoredToken } from '@/utils/storage';
import { emitError } from '@/utils/message';

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 90000,
});

apiClient.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      '请求失败，请稍后重试';

    if (status === 401) {
      clearStoredAuth();
      emitError('登录已失效，请重新登录');
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }

    emitError(message);
    return Promise.reject(error);
  },
);

export default apiClient;
