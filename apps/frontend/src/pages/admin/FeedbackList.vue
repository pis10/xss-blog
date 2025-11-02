<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-brand">
        <el-icon size="32"><Tools /></el-icon>
        <h2>管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#121826"
        text-color="#E2E8F0"
        active-text-color="#22D3EE">
        <el-menu-item index="/admin/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/feedbacks">
          <el-icon><ChatDotRound /></el-icon>
          <span>反馈管理</span>
        </el-menu-item>
        <el-menu-item index="/" divided>
          <el-icon><HomeFilled /></el-icon>
          <span>返回前台</span>
        </el-menu-item>
      </el-menu>
    </aside>
    
    <main class="admin-main">
      <div class="admin-content">
        <div class="page-header">
          <h1>反馈管理</h1>
          <span class="badge" :class="configStore.xssMode === 'vuln' ? 'badge-vuln' : 'badge-secure'">
            {{ configStore.xssMode.toUpperCase() }} 模式
          </span>
        </div>
        
        <div v-if="!loading" class="feedback-table">
          <el-table
            :data="feedbacks"
            style="width: 100%"
            :row-class-name="getRowClassName">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="email" label="邮箱" width="220" />
            <el-table-column label="内容" min-width="300">
              <template #default="{ row }">
                <div class="feedback-preview">{{ getPreview(row.contentHtml) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'NEW' ? 'danger' : 'success'">
                  {{ row.status === 'NEW' ? '未读' : '已读' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="viewFeedback(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <div v-else class="loading">
          <div class="spinner"></div>
        </div>
      </div>
    </main>
    
    <!-- 反馈详情弹窗（L3 落点）：VULN 直接渲染，SECURE 净化后渲染 -->
    <el-dialog
      v-model="showDetailDialog"
      title="反馈详情"
      width="700px"
      @close="currentFeedback = null">
      <div v-if="currentFeedback" class="feedback-detail">
        <div class="detail-row">
          <label>邮箱：</label>
          <span>{{ currentFeedback.email }}</span>
        </div>
        <div class="detail-row">
          <label>提交时间：</label>
          <span>{{ formatDate(currentFeedback.createdAt) }}</span>
        </div>
        <div class="detail-row">
          <label>状态：</label>
          <el-tag :type="currentFeedback.status === 'NEW' ? 'danger' : 'success'">
            {{ currentFeedback.status === 'NEW' ? '未读' : '已读' }}
          </el-tag>
        </div>
        <div class="detail-content">
          <label>反馈内容：</label>
          <!-- 盲 XSS（L3）渲染点：
               - VULN：直接渲染用户提交的 HTML
               - SECURE：使用 DOMPurify 净化后再渲染 -->
          <div 
            v-if="configStore.xssMode === 'vuln'" 
            class="content-html" 
            v-html="currentFeedback.contentHtml">
          </div>
          <div 
            v-else 
            class="content-html" 
            v-html="pure(currentFeedback.contentHtml)">
          </div>
        </div>
        
        <div v-if="configStore.xssMode === 'vuln'" class="xss-warning">
          <el-alert
            title="⚠️ XSS 盲点演示"
            type="warning"
            :closable="false">
            <p>此内容来自用户提交的反馈，在 VULN 模式下未经过滤直接渲染。</p>
            <p>如果用户提交了恶意脚本，管理员查看时会触发 XSS（盲 XSS）。</p>
          </el-alert>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
// 管理后台 - 反馈列表与详情（L3：盲 XSS 落点）
// - VULN：详情内的 HTML 原样渲染，可能执行恶意脚本
// - SECURE：先用 DOMPurify 白名单净化再渲染
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useConfigStore } from '@/stores/config';
import { pure } from '@/utils/xss';
import axios from '@/api/axios';
import { ElMessage } from 'element-plus';

const route = useRoute();
const configStore = useConfigStore();

const feedbacks = ref([]);
const loading = ref(true);
const showDetailDialog = ref(false);
const currentFeedback = ref(null);

const activeMenu = computed(() => route.path);

const fetchFeedbacks = async () => {
  try {
    const response = await axios.get('/admin/feedbacks?page=0&size=50');
    feedbacks.value = response.data.content || [];
  } catch (error) {
    console.error('Failed to fetch feedbacks:', error);
    ElMessage.error('加载反馈失败');
  } finally {
    loading.value = false;
  }
};

const viewFeedback = async (feedback) => {
  try {
    const response = await axios.get(`/admin/feedbacks/${feedback.id}`);
    currentFeedback.value = response.data;
    showDetailDialog.value = true;
    
    // 刷新列表以更新『已读/未读』状态
    await fetchFeedbacks();
  } catch (error) {
    console.error('Failed to fetch feedback detail:', error);
    ElMessage.error('加载反馈详情失败');
  }
};

const getPreview = (html) => {
  if (!html) return '';
  const text = html.replace(/<[^>]*>/g, '');
  return text.length > 100 ? text.substring(0, 100) + '...' : text;
};

const getRowClassName = ({ row }) => {
  return row.status === 'NEW' ? 'new-feedback-row' : '';
};

const formatDate = (date) => {
  if (!date) return '';
  return new Date(date).toLocaleString('zh-CN');
};

onMounted(() => {
  fetchFeedbacks();
});
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg-base);
}

.admin-sidebar {
  width: 250px;
  background: var(--color-bg-card);
  border-right: 1px solid var(--color-border);
  padding: var(--spacing-lg);
  position: fixed;
  height: 100vh;
  overflow-y: auto;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-lg);
  margin-bottom: var(--spacing-xl);
  color: var(--color-primary);
}

.admin-brand h2 {
  font-size: 1.25rem;
}

.admin-main {
  flex: 1;
  margin-left: 250px;
  padding: var(--spacing-2xl);
}

.admin-content {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--spacing-2xl);
}

.page-header h1 {
  font-size: 2.5rem;
}

.feedback-preview {
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.new-feedback-row) {
  background: rgba(239, 68, 68, 0.05);
}

.feedback-detail {
  padding: var(--spacing-md);
}

.detail-row {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-md);
  padding-bottom: var(--spacing-md);
  border-bottom: 1px solid var(--color-border);
}

.detail-row label {
  font-weight: 600;
  color: var(--color-text-secondary);
  min-width: 100px;
}

.detail-content {
  margin-top: var(--spacing-lg);
}

.detail-content label {
  display: block;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-md);
}

.content-html {
  background: var(--color-bg-elevated);
  padding: var(--spacing-lg);
  border-radius: var(--radius-md);
  line-height: 1.8;
  color: var(--color-text-primary);
  min-height: 100px;
}

.xss-warning {
  margin-top: var(--spacing-lg);
}

.xss-warning p {
  margin: var(--spacing-sm) 0;
  font-size: 0.9rem;
}
</style>
