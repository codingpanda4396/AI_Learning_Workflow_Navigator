<script setup lang="ts">
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
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
  <div class="flex min-h-screen items-center justify-center px-6">
    <div class="grid w-full max-w-5xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <section class="rounded-[2rem] bg-slate-900 p-10 text-white">
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">AI Learning Workflow</p>
        <h1 class="mt-4 text-4xl font-semibold leading-tight">{{ APP_TITLE }}</h1>
        <p class="mt-6 max-w-lg text-sm leading-7 text-slate-300">
          从登录开始，围绕真实后端链路完成学习会话创建、任务执行、训练测验、反馈报告和成长看板。
        </p>
      </section>

      <section class="rounded-[2rem] bg-white p-8 shadow-sm ring-1 ring-slate-200">
        <div class="flex rounded-full bg-slate-100 p-1">
          <button
            class="flex-1 rounded-full px-4 py-2 text-sm"
            :class="mode === 'login' ? 'bg-white font-medium text-slate-900 shadow-sm' : 'text-slate-500'"
            @click="mode = 'login'"
          >
            登录
          </button>
          <button
            class="flex-1 rounded-full px-4 py-2 text-sm"
            :class="mode === 'register' ? 'bg-white font-medium text-slate-900 shadow-sm' : 'text-slate-500'"
            @click="mode = 'register'"
          >
            注册
          </button>
        </div>

        <form class="mt-8 space-y-5" @submit.prevent="submit">
          <label class="block">
            <span class="text-sm text-slate-600">用户名</span>
            <input v-model="form.username" class="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-slate-400" required />
          </label>
          <label class="block">
            <span class="text-sm text-slate-600">密码</span>
            <input v-model="form.password" type="password" class="mt-2 w-full rounded-2xl border border-slate-200 px-4 py-3 outline-none focus:border-slate-400" required />
          </label>
          <button
            class="w-full rounded-2xl bg-slate-900 px-4 py-3 text-sm font-medium text-white transition hover:bg-slate-800 disabled:opacity-60"
            :disabled="authStore.loading"
          >
            {{ mode === 'login' ? '登录进入首页' : '注册并登录' }}
          </button>
        </form>

        <p v-if="authStore.error" class="mt-4 text-sm text-rose-600">{{ authStore.error }}</p>
      </section>
    </div>
  </div>
</template>
