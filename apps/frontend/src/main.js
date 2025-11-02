// 应用入口：创建 Vue 应用并装配 Pinia、路由、Element Plus 等
// 这里会先从后端获取配置（当前 XSS 模式），用于演示 VULN/SECURE 的对比
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import 'element-plus/theme-chalk/dark/css-vars.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';

import App from './App.vue';
import router from './router';
import './theme/dark.scss';
import { useConfigStore } from '@/stores/config';
import { useAuthStore } from '@/stores/auth';

const app = createApp(App);
const pinia = createPinia();

// 注册 Element Plus 图标（全局可用）
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}

app.use(pinia);
app.use(router);
app.use(ElementPlus, { size: 'default', zIndex: 3000 });

// 初始化 Store（配置/认证）
const configStore = useConfigStore();
const authStore = useAuthStore();

// 拉取后端配置并同步到全局变量（仅用于演示场景的模式读取）
await configStore.fetchConfig();
window.__XSS_MODE__ = configStore.xssMode;

// 初始化登录态：
// - VULN 模式：如果 localStorage 有 Token 会尝试获取当前用户
// - SECURE 模式：依赖 HttpOnly Cookie，直接尝试获取当前用户
await authStore.init();

app.mount('#app');
