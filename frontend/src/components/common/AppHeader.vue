<script setup lang="ts">
import { useRouter } from 'vue-router';
import { APP_TITLE } from '@/constants/app';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const router = useRouter();

function logout() {
  authStore.logout();
  router.push('/login');
}
</script>

<template>
  <header class="fixed inset-x-0 top-0 z-40 border-b border-slate-200/80 bg-white/88 backdrop-blur">
    <div class="mx-auto flex max-w-6xl items-center justify-between px-5 py-4 md:px-6">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Panda Navigator</p>
        <h1 class="text-lg font-semibold text-slate-950">{{ APP_TITLE }}</h1>
      </div>
      <div class="flex items-center gap-4">
        <div class="text-right">
          <p class="text-sm font-medium text-slate-800">{{ authStore.username || '已登录用户' }}</p>
          <p class="text-xs text-slate-500">顺着当前学习闭环继续前进</p>
        </div>
        <button
          class="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
          @click="logout"
        >
          退出登录
        </button>
      </div>
    </div>
  </header>
</template>
