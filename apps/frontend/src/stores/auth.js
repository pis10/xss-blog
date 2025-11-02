// 认证 Store
// - VULN 模式：将 JWT 存在 localStorage（教学用，故意不安全）
// - SECURE 模式：使用 HttpOnly Cookie（前端 JS 不可见）
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import axios from '@/api/axios';

export const useAuthStore = defineStore('auth', () => {
  const user = ref(null);
  const loading = ref(false);
  
  const isAuthenticated = computed(() => !!user.value);
  const isAdmin = computed(() => user.value?.role === 'ADMIN');
  
  async function login(credentials) {
    loading.value = true;
    try {
      const response = await axios.post('/auth/login', credentials);
      
      const xssMode = window.__XSS_MODE__ || import.meta.env.VITE_XSS_MODE;
      if (xssMode === 'vuln' && response.data.accessToken) {
        // VULN 模式：把后端返回的 Token 暂存到 localStorage（不安全）
        localStorage.setItem('accessToken', response.data.accessToken);
      }
      // SECURE 模式：Token 已在 Cookie 中，无需前端存储
      
      await fetchCurrentUser();
      return true;
    } catch (error) {
      console.error('Login failed:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }
  
  async function register(data) {
    loading.value = true;
    try {
      const response = await axios.post('/auth/register', data);
      
      const xssMode = window.__XSS_MODE__ || import.meta.env.VITE_XSS_MODE;
      if (xssMode === 'vuln' && response.data.accessToken) {
        localStorage.setItem('accessToken', response.data.accessToken);
      }
      
      await fetchCurrentUser();
      return true;
    } catch (error) {
      console.error('Register failed:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }
  
  async function logout() {
    try {
      await axios.post('/auth/logout');
    } catch (error) {
      console.error('Logout error:', error);
    }
    
    localStorage.removeItem('accessToken');
    user.value = null;
  }
  
  async function fetchCurrentUser() {
    try {
      const response = await axios.get('/auth/me');
      user.value = response.data;
    } catch (error) {
      console.error('Fetch current user failed:', error);
      user.value = null;
    }
  }
  
  async function init() {
    const xssMode = window.__XSS_MODE__ || import.meta.env.VITE_XSS_MODE;
    
    // 初始化当前用户：
    // - VULN：只有在本地存在 Token 时才请求 /auth/me
    // - SECURE：直接请求，凭证由浏览器带上
    if (xssMode === 'vuln' && !localStorage.getItem('accessToken')) {
      return;
    }
    
    await fetchCurrentUser();
  }
  
  return {
    user,
    loading,
    isAuthenticated,
    isAdmin,
    login,
    register,
    logout,
    fetchCurrentUser,
    init
  };
});
