<script setup lang="ts">
import { useRouter } from 'vue-router';
import AppButton from '@/components/ui/AppButton.vue';
import type { ActiveSession } from '@/types/home';

defineProps<{
  session: ActiveSession | null;
}>();

const router = useRouter();

function continueLearning(session: ActiveSession | null) {
  if (!session) {
    return;
  }
  router.push(`/sessions/${session.id}`);
}
</script>

<template>
  <section class="app-card app-card-padding app-card-strong">
    <div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between">
      <div class="max-w-2xl">
        <p class="app-eyebrow">当前学习</p>
        <h2 class="mt-2 text-[28px] font-semibold tracking-[-0.035em] text-slate-950">
          {{ session ? '继续你刚才的学习主线' : '现在还没有进行中的会话' }}
        </h2>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          {{ session ? '不用重新找入口，直接回到当前该做的那一步。' : '先在上方输入目标，系统会帮你开好第一条学习路径。' }}
        </p>
      </div>
      <span class="app-pill">{{ session ? '进行中' : '待开始' }}</span>
    </div>

    <div v-if="session" class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <div class="app-stat">
        <p class="app-stat-label">学习目标</p>
        <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ session.goal || '未填写' }}</p>
      </div>
      <div class="app-stat">
        <p class="app-stat-label">课程</p>
        <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ session.course || '未填写' }}</p>
      </div>
      <div class="app-stat">
        <p class="app-stat-label">章节</p>
        <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ session.chapter || '未填写' }}</p>
      </div>
      <div class="app-stat">
        <p class="app-stat-label">当前状态</p>
        <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ session.phase || '待开始' }}</p>
      </div>
    </div>

    <div class="mt-6">
      <AppButton v-if="session" size="lg" @click="continueLearning(session)">继续下一步</AppButton>
    </div>
  </section>
</template>
