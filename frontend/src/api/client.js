import axios from 'axios';

// Business API through the gateway. In dev, Vite proxies /api -> :8080.
export const api = axios.create({ baseURL: '/' });

// Attach the JWT to every request if present.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Unwrap the platform ApiResponse envelope; surface business errors.
api.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject(new Error(body.message || 'Request failed'));
      }
      return body.data;
    }
    return body;
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
    }
    const msg = error.response?.data?.message || error.message || 'Network error';
    return Promise.reject(new Error(msg));
  }
);

// AI assistant service (proxied /api/assistant -> :8087). Returns raw JSON.
export const assistant = axios.create({ baseURL: '/' });
