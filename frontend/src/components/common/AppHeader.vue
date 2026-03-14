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
  <header class="fixed inset-x-0 top-0 z-40 border-b border-slate-200/70 bg-white/78 backdrop-blur-xl">
    <div class="mx-auto flex max-w-[1120px] items-center justify-between px-4 py-4 md:px-0">
      <div class="flex items-center gap-4">
        <div class="flex h-11 w-11 items-center justify-center rounded-2xl border border-slate-200/80 bg-white shadow-[0_10px_24px_rgba(15,23,42,0.06)]">
          <span class="text-sm font-semibold tracking-[0.12em] text-slate-700">AI</span>
        </div>
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Learning Navigator</p>
          <h1 class="text-lg font-semibold tracking-[-0.02em] text-slate-950">{{ APP_TITLE }}</h1>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <div class="hidden text-right md:block">
          <p class="text-sm font-medium text-slate-800">{{ authStore.username || '已登录用户' }}</p>
          <p class="text-xs text-slate-500">按当前主线继续往前走</p>
        </div>
        <button class="app-btn app-btn-secondary app-btn-sm" @click="logout">
          退出登录
        </button>
      </div>
    </div>
  </header>
</template>
