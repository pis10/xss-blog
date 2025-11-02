<template>
  <header class="app-header">
    <div class="container header-content">
      <div class="logo-section">
        <router-link to="/" class="logo">
          <span class="logo-icon">⚡</span>
          <span class="logo-text">TechBlog</span>
          <el-tooltip :content="'点击切换到 ' + (configStore.xssMode === 'vuln' ? 'SECURE' : 'VULN') + ' 模式'" placement="bottom">
            <span class="badge" :class="modeBadgeClass" @click.prevent="toggleMode">{{ modeText }}</span>
          </el-tooltip>
        </router-link>
      </div>
      
      <nav class="nav-menu">
        <router-link to="/" class="nav-link">首页</router-link>
        <router-link to="/search" class="nav-link">搜索</router-link>
        <router-link to="/feedback" class="nav-link">反馈</router-link>
      </nav>
      
      <div class="user-section">
        <template v-if="authStore.isAuthenticated">
          <router-link 
            v-if="authStore.isAdmin" 
            to="/admin/dashboard" 
            class="nav-link admin-link">
            <el-icon><Tools /></el-icon>
            管理后台
          </router-link>
          <el-dropdown @command="handleUserCommand">
            <div class="user-avatar">
              <img :src="authStore.user.avatarUrl" :alt="authStore.user.username" />
              <span>{{ authStore.user.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item :command="`profile:${authStore.user.username}`">
                  <el-icon><User /></el-icon>
                  我的主页
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-primary">登录</router-link>
          <router-link to="/register" class="btn-secondary">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup>
// 顶部导航
// - 点击徽章在 VULN/SECURE 间切换（切换后会登出以确保模式生效）
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useConfigStore } from '@/stores/config';
import { ElMessage, ElMessageBox } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();
const configStore = useConfigStore();

const modeText = computed(() => configStore.xssMode === 'vuln' ? 'VULN' : 'SECURE');
const modeBadgeClass = computed(() => configStore.xssMode === 'vuln' ? 'badge-vuln' : 'badge-secure');

const toggleMode = async () => {
  const newMode = configStore.xssMode === 'vuln' ? 'secure' : 'vuln';
  const modeText = newMode === 'vuln' ? 'VULN（漏洞）' : 'SECURE（安全）';
  
  try {
    await ElMessageBox.confirm(
      `切换到 ${modeText} 模式后需要重新登录，是否继续？`,
      '切换模式',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );
    
    await configStore.switchMode(newMode);
    window.__XSS_MODE__ = newMode;
    
    // 退出登录并跳转登录页，避免旧模式的登录态残留
    await authStore.logout();
    ElMessage.success(`已切换到 ${modeText} 模式`);
    router.push('/login');
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('切换模式失败');
    }
  }
};

const handleUserCommand = async (command) => {
  if (command === 'logout') {
    await authStore.logout();
    ElMessage.success('已退出登录');
    router.push('/');
  } else if (command.startsWith('profile:')) {
    const username = command.split(':')[1];
    router.push(`/profile/${username}`);
  }
};
</script>

<style scoped>
.app-header {
  background: var(--color-bg-card);
  border-bottom: 1px solid var(--color-border);
  padding: var(--spacing-md) 0;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--spacing-xl);
}

.logo-section {
  display: flex;
  align-items: center;
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-text-primary);
  text-decoration: none;
}

.logo-icon {
  font-size: 2rem;
}

.logo-text {
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.badge {
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s;
}

.badge:hover {
  transform: scale(1.1);
  opacity: 0.8;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  flex: 1;
}

.nav-link {
  color: var(--color-text-secondary);
  font-weight: 500;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  gap: var(--spacing-xs);
}

.nav-link:hover,
.nav-link.router-link-active {
  color: var(--color-primary);
  background: rgba(34, 211, 238, 0.1);
}

.admin-link {
  color: var(--color-accent);
}

.admin-link:hover {
  background: rgba(251, 146, 60, 0.1);
}

.user-section {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.user-avatar {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.user-avatar:hover {
  background: var(--color-bg-elevated);
}

.user-avatar img {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
}

.user-avatar span {
  font-weight: 500;
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .nav-menu {
    display: none;
  }
  
  .header-content {
    gap: var(--spacing-md);
  }
}
</style>
