<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PrimaryButton from '@/components/PrimaryButton.vue'

type AuthTab = 'login' | 'register'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<AuthTab>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const formError = ref('')

const isSubmitting = computed(() =>
  activeTab.value === 'login' ? authStore.signingIn : authStore.signingUp,
)

function resetFormError() {
  formError.value = ''
  authStore.clearError()
}

function switchTab(tab: AuthTab) {
  activeTab.value = tab
  resetFormError()
}

function validate() {
  if (!username.value.trim()) {
    formError.value = '请输入用户名。'
    return false
  }
  if (!password.value.trim()) {
    formError.value = '请输入密码。'
    return false
  }
  if (activeTab.value === 'register') {
    if (password.value.length < 6) {
      formError.value = '密码至少 6 位。'
      return false
    }
    if (password.value !== confirmPassword.value) {
      formError.value = '两次输入的密码不一致。'
      return false
    }
  }
  return true
}

function resolveRedirectPath() {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/')) {
    return redirect
  }
  return '/'
}

async function handleRegister() {
  await authStore.signUp({
    username: username.value.trim(),
    password: password.value,
  })
  activeTab.value = 'login'
  formError.value = '注册成功，请登录。'
  confirmPassword.value = ''
}

async function handleLogin() {
  await authStore.signIn({
    username: username.value.trim(),
    password: password.value,
  })
  await router.replace(resolveRedirectPath())
}

async function handleSubmit() {
  resetFormError()
  if (!validate()) {
    return
  }
  try {
    if (activeTab.value === 'register') {
      await handleRegister()
      return
    }
    await handleLogin()
  } catch {
    formError.value = authStore.error ?? '操作失败，请稍后重试。'
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card">
      <header class="auth-header">
        <p class="eyebrow">AI Learning Navigator</p>
        <h1>{{ activeTab === 'login' ? '登录' : '注册' }}</h1>
        <p class="subtitle">登录后可继续你的学习流程和会话历史。</p>
      </header>

      <div class="tabs">
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'login' }" @click="switchTab('login')">
          登录
        </button>
        <button type="button" class="tab-btn" :class="{ active: activeTab === 'register' }" @click="switchTab('register')">
          注册
        </button>
      </div>

      <form class="auth-form" @submit.prevent="handleSubmit">
        <label class="field">
          <span>用户名</span>
          <input v-model="username" type="text" autocomplete="username" />
        </label>
        <label class="field">
          <span>密码</span>
          <input v-model="password" type="password" autocomplete="current-password" />
        </label>
        <label v-if="activeTab === 'register'" class="field">
          <span>确认密码</span>
          <input v-model="confirmPassword" type="password" autocomplete="new-password" />
        </label>

        <p v-if="formError" class="error">{{ formError }}</p>
        <PrimaryButton type="submit" :loading="isSubmitting">
          {{ activeTab === 'login' ? '登录并进入' : '创建账号' }}
        </PrimaryButton>
      </form>
    </section>
  </main>
</template>

<style scoped>
.auth-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-card {
  width: min(460px, 100%);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, rgba(14, 24, 44, 0.95), rgba(7, 12, 24, 0.95));
  box-shadow: var(--shadow-md);
  padding: clamp(20px, 4vw, 30px);
  display: grid;
  gap: var(--space-lg);
}

.auth-header {
  display: grid;
  gap: var(--space-sm);
}

.eyebrow {
  color: var(--color-primary-hover);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  font-weight: 600;
}

.subtitle {
  color: var(--color-text-secondary);
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.tab-btn {
  min-height: 40px;
  color: var(--color-text-secondary);
  background: rgba(13, 21, 40, 0.7);
}

.tab-btn.active {
  color: var(--color-text);
  background: rgba(62, 140, 255, 0.2);
}

.auth-form {
  display: grid;
  gap: var(--space-md);
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.field input {
  height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(11, 18, 34, 0.9);
  color: var(--color-text);
  padding: 0 12px;
}

.error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}
</style>
