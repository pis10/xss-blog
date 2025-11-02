// 配置 Store：保存与切换 XSS 模式
// - 从后端拉取当前模式（vuln/secure），供前端渲染策略使用
// - 提供 switchMode 以便一键切换演示模式
import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from '@/api/axios';

export const useConfigStore = defineStore('config', () => {
  const xssMode = ref('vuln');
  const loading = ref(false);
  
  async function fetchConfig() {
    loading.value = true;
    try {
      const response = await axios.get('/config');
      xssMode.value = response.data.xssMode || 'vuln';
    } catch (error) {
      console.error('Fetch config failed:', error);
    } finally {
      loading.value = false;
    }
  }
  
  async function switchMode(newMode) {
    loading.value = true;
    try {
      const response = await axios.post('/config/mode', { mode: newMode });
      xssMode.value = response.data.xssMode || newMode;
      return true;
    } catch (error) {
      console.error('Switch mode failed:', error);
      throw error;
    } finally {
      loading.value = false;
    }
  }
  
  return {
    xssMode,
    loading,
    fetchConfig,
    switchMode
  };
});
