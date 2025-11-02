import axios from 'axios';
import { getXssMode } from '@/utils/xss';

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 10000
});

// 全局 Axios 实例
// 说明：
// - VULN 模式：从 localStorage 读取 JWT 并放到 Authorization 头（教学用，故意不安全）
// - SECURE 模式：使用 HttpOnly Cookie（JS 无法读取），通过 withCredentials 让浏览器自动携带
// - 与后端双态配置保持一致，便于演示对比
// 请求拦截器
instance.interceptors.request.use(
  (config) => {
    const xssMode = getXssMode();
    
    if (xssMode === 'vuln') {
      // VULN 模式：从 localStorage 取 Token 并加到请求头（不安全示范）
      const token = localStorage.getItem('accessToken');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    } else {
      // SECURE 模式：开启 withCredentials，浏览器自动携带 HttpOnly Cookie
      config.withCredentials = true;
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
instance.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // 401 未认证：清理本地 Token 并跳转登录页
      localStorage.removeItem('accessToken');
      // 避免重定向循环：不在登录页/个人主页再跳转
      if (!window.location.pathname.includes('/login') && 
          !window.location.pathname.includes('/profile/')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default instance;
