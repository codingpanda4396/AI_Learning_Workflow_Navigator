<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppButton from '@/components/ui/AppButton.vue';
import { useAuthStore } from '@/stores/auth';
import { APP_TITLE } from '@/constants/app';

const authStore = useAuthStore();
const router = useRouter();
const mode = ref<'login' | 'register'>('login');
const form = reactive({
  username: '',
  password: '',
});

async function submit() {
  if (mode.value === 'register') {
    await authStore.register(form);
  }
  await authStore.login(form);
  await router.push('/');
}
</script>

<template>
  <div class="flex min-h-screen items-center justify-center px-4 py-8">
    <div class="grid w-full max-w-[1120px] gap-6 lg:grid-cols-[1.1fr_420px]">
      <section class="app-hero">
        <p class="app-eyebrow">AI Learning Workflow</p>
        <h1 class="app-title-xl mt-4">{{ APP_TITLE }}</h1>
        <p class="app-text-lead mt-5 max-w-xl">
          从登录开始，系统会把诊断、规划、任务学习、练习和反馈收成一条稳定主线，适合直接演示完整闭环。
        </p>
      </section>

      <section class="app-card app-card-padding app-card-strong">
        <div class="grid grid-cols-2 gap-2 rounded-[18px] bg-slate-100 p-1">
          <button
            class="rounded-[14px] px-4 py-2.5 text-sm font-medium transition"
            :class="mode === 'login' ? 'bg-white text-slate-900 shadow-[0_8px_16px_rgba(15,23,42,0.08)]' : 'text-slate-500'"
            @click="mode = 'login'"
          >
            登录
          </button>
          <button
            class="rounded-[14px] px-4 py-2.5 text-sm font-medium transition"
            :class="mode === 'register' ? 'bg-white text-slate-900 shadow-[0_8px_16px_rgba(15,23,42,0.08)]' : 'text-slate-500'"
            @click="mode = 'register'"
          >
            注册
          </button>
        </div>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="app-eyebrow">用户名</span>
            <input v-model="form.username" class="app-input mt-2" required />
          </label>
          <label class="block">
            <span class="app-eyebrow">密码</span>
            <input v-model="form.password" type="password" class="app-input mt-2" required />
          </label>
          <AppButton type="submit" block size="lg" :loading="authStore.loading">
            {{ mode === 'login' ? '登录并进入首页' : '注册并进入首页' }}
          </AppButton>
        </form>

        <p v-if="authStore.error" class="mt-4 text-sm text-rose-600">{{ authStore.error }}</p>
      </section>
    </div>
  </div>
</template>
