// 路由配置
// 说明：含管理员后台路由，进入前做简单的权限校验（基于 Pinia 中的用户角色）
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/pages/Home.vue')
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/Register.vue')
  },
  {
    path: '/article/:id',
    name: 'ArticleDetail',
    component: () => import('@/pages/ArticleDetail.vue')
  },
  {
    path: '/profile/:username',
    name: 'Profile',
    component: () => import('@/pages/Profile.vue')
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/pages/Search.vue')
  },
  {
    path: '/feedback',
    name: 'Feedback',
    component: () => import('@/pages/Feedback.vue')
  },
  {
    path: '/admin',
    redirect: '/admin/dashboard',
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/pages/admin/Dashboard.vue'),
    meta: { requiresAdmin: true }
  },
  {
    path: '/admin/feedbacks',
    name: 'AdminFeedbackList',
    component: () => import('@/pages/admin/FeedbackList.vue'),
    meta: { requiresAdmin: true }
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

// 全局前置守卫：需要管理员权限的路由做访问控制
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  
  if (to.meta.requiresAdmin) {
    if (!authStore.isAuthenticated) {
      next('/login');
    } else if (!authStore.isAdmin) {
      next('/');
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
