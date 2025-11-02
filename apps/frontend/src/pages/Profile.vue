<template>
  <div class="profile-page">
    <div v-if="!loading && user" class="profile-container">
      <!-- 顶部横幅 -->
      <div class="profile-banner" :style="{ backgroundImage: `url(${user.bannerUrl})` }">
        <div class="banner-overlay"></div>
      </div>
      
      <!-- 用户信息 -->
      <div class="container">
        <div class="profile-header">
          <img :src="user.avatarUrl" :alt="user.username" class="profile-avatar" />
          <div class="profile-info">
            <h1>{{ user.username }}</h1>
            <p class="user-role">{{ user.role }}</p>
          </div>
        </div>
        
        <!-- 个人简介（L2 落点）：VULN 直接渲染，SECURE 先净化再渲染 -->
        <div class="profile-bio card">
          <h3>个人简介</h3>
          <div v-if="configStore.xssMode === 'vuln'" class="bio-content" v-html="user.bio || '暂无简介'"></div>
          <div v-else class="bio-content" v-html="pure(user.bio) || '暂无简介'"></div>
          
          <div v-if="isOwnProfile" class="bio-edit-section">
            <el-button @click="showEditDialog = true" type="primary">编辑简介</el-button>
          </div>
        </div>
        
        <!-- 用户文章列表 -->
        <div class="user-articles">
          <h2>发布的文章 ({{ articles.length }})</h2>
          <div class="articles-grid">
            <article-card v-for="article in articles" :key="article.id" :article="article" />
          </div>
        </div>
      </div>
    </div>
    
    <div v-else-if="loading" class="loading">
      <div class="spinner"></div>
    </div>
    
    <!-- 编辑个人简介弹窗 -->
    <el-dialog v-model="showEditDialog" title="编辑个人简介" width="600px">
      <el-input
        v-model="bioContent"
        type="textarea"
        :rows="6"
        placeholder="支持HTML格式（用于XSS演示）"
      />
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="updateBio" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
// 个人主页（L2）：演示存储型 XSS（伪装登录表单）
// - VULN：user.bio 原样渲染（可能执行恶意脚本）
// - SECURE：使用 DOMPurify 净化后再渲染
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useConfigStore } from '@/stores/config';
import { pure } from '@/utils/xss';
import axios from '@/api/axios';
import ArticleCard from '@/components/ArticleCard.vue';
import { ElMessage } from 'element-plus';

const route = useRoute();
const authStore = useAuthStore();
const configStore = useConfigStore();

const user = ref(null);
const articles = ref([]);
const loading = ref(true);
const showEditDialog = ref(false);
const bioContent = ref('');
const saving = ref(false);

const isOwnProfile = computed(() => {
  return authStore.isAuthenticated && authStore.user?.username === route.params.username;
});

const fetchProfile = async () => {
  try {
    const response = await axios.get(`/profile/${route.params.username}`);
    user.value = response.data.user;
    articles.value = response.data.articles.content || [];
    bioContent.value = user.value.bio || '';
  } catch (error) {
    console.error('Failed to fetch profile:', error);
  } finally {
    loading.value = false;
  }
};

const updateBio = async () => {
  saving.value = true;
  try {
    await axios.post('/profile/bio', { bio: bioContent.value });
    user.value.bio = bioContent.value;
    showEditDialog.value = false;
    ElMessage.success('简介已更新');
  } catch (error) {
    ElMessage.error('更新失败');
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  fetchProfile();
});
</script>

<style scoped>
.profile-banner {
  width: 100%;
  height: 300px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, transparent, var(--color-bg-base));
}

.profile-header {
  display: flex;
  align-items: center;
  gap: var(--spacing-lg);
  margin-top: -80px;
  margin-bottom: var(--spacing-2xl);
  position: relative;
  z-index: 10;
}

.profile-avatar {
  width: 160px;
  height: 160px;
  border-radius: 50%;
  border: 6px solid var(--color-bg-base);
  box-shadow: var(--shadow-lg);
}

.profile-info h1 {
  font-size: 2.5rem;
  margin-bottom: var(--spacing-sm);
}

.user-role {
  color: var(--color-text-muted);
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 1px;
}

.profile-bio {
  margin-bottom: var(--spacing-2xl);
}

.profile-bio h3 {
  margin-bottom: var(--spacing-md);
}

.bio-content {
  color: var(--color-text-secondary);
  line-height: 1.8;
  min-height: 60px;
}

.bio-edit-section {
  margin-top: var(--spacing-lg);
  padding-top: var(--spacing-lg);
  border-top: 1px solid var(--color-border);
}

.user-articles h2 {
  margin-bottom: var(--spacing-xl);
}

.articles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: var(--spacing-lg);
}

@media (max-width: 768px) {
  .profile-header {
    flex-direction: column;
    text-align: center;
  }
  
  .articles-grid {
    grid-template-columns: 1fr;
  }
}
</style>
