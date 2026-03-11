<script setup lang="ts">
import { APP_TITLE } from '@/constants/app';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();

function logout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <header class="fixed inset-x-0 top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
    <div class="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
      <div>
        <p class="text-xs uppercase tracking-[0.24em] text-slate-400">Panda Navigator</p>
        <h1 class="text-lg font-semibold text-slate-900">{{ APP_TITLE }}</h1>
      </div>
      <div class="flex items-center gap-4">
        <div class="text-right">
          <p class="text-sm font-medium text-slate-800">{{ authStore.username || '已登录用户' }}</p>
          <p class="text-xs text-slate-500">学习流程主链路</p>
        </div>
        <button
          class="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
          @click="logout"
        >
          退出
        </button>
      </div>
    </div>
  </header>
</template>
