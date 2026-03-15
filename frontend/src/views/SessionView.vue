<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { formatSessionStatus, formatStage } from '@/utils/format';
import { useSessionStore } from '@/stores/session';
import { useLearningFlowStore } from '@/stores/learningFlow';

const route = useRoute();
const sessionStore = useSessionStore();
const flowStore = useLearningFlowStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const summary = computed(() => overview.value?.summary);
const timeline = computed(() => overview.value?.timeline ?? []);

async function openPrimaryAction() {
  const cta = flowStore.snapshot?.primaryCTA;
  if (cta) await flowStore.goToStage(cta.stage);
}

async function loadOverview() {
  if (!sessionId.value) return;
  await flowStore.loadSessionFlow(sessionId.value);
}
</script>

<template>
  <AppShell>
    <LoadingState v-if="sessionStore.loading && !overview" />

    <div v-else-if="sessionStore.error && !overview" class="space-y-4">
      <ErrorState :message="sessionStore.error" />
      <AppButton variant="secondary" @click="loadOverview">重新加载会话</AppButton>
    </div>

    <div v-else-if="overview" class="app-stack-lg">
      <section class="app-hero">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-2xl">
            <p class="app-eyebrow">学习导航</p>
            <h1 class="app-title-lg mt-4">{{ summary?.currentTaskTitle || '你现在正在这条学习主线里' }}</h1>
            <p class="app-text-lead mt-4">
              {{ summary?.currentTaskDescription || '页面只保留当前最该做的动作，让你不用再理解系统术语。' }}
            </p>
          </div>
          <div class="flex flex-col gap-3">
            <AppButton size="lg" @click="openPrimaryAction">
              {{ flowStore.snapshot?.primaryCTA?.label ?? summary?.primaryActionLabel ?? '继续下一步' }}
            </AppButton>
            <span class="app-badge justify-center">{{ formatSessionStatus(overview.sessionStatus) }}</span>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-3">
        <div class="app-stat">
          <p class="app-stat-label">课程 / 章节</p>
          <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ overview.courseId }} / {{ overview.chapterId }}</p>
        </div>
        <div class="app-stat">
          <p class="app-stat-label">当前阶段</p>
          <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ formatStage(overview.currentStage) }}</p>
        </div>
        <div class="app-stat">
          <p class="app-stat-label">任务进度</p>
          <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">
            {{ overview.progress.completedTaskCount }} / {{ overview.progress.totalTaskCount }}
          </p>
        </div>
      </section>

      <div class="grid gap-5 lg:grid-cols-[minmax(0,1fr)_300px]">
        <SectionCard strong title="你现在该做什么" :description="summary?.nextStepHint || '先完成当前任务，后面的步骤会自动接上。'">
          <div class="flex flex-wrap items-center gap-3">
            <AppButton size="lg" @click="openPrimaryAction">
              {{ flowStore.snapshot?.primaryCTA?.label ?? summary?.primaryActionLabel ?? '继续下一步' }}
            </AppButton>
            <AppButton variant="secondary" @click="$router.push('/')">回到首页</AppButton>
          </div>
        </SectionCard>

        <SectionCard title="当前目标">
          <p class="text-sm leading-7 text-slate-600">{{ overview.goalText || '先完成当前任务，系统会继续推进整条学习主线。' }}</p>
          <div class="app-divider my-5" />
          <p class="app-eyebrow">最近反馈</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ summary?.recentReportSummary || '这一轮还没有新的反馈报告。' }}</p>
        </SectionCard>
      </div>

      <SectionCard title="接下来的学习节奏" description="这里只展示这次会话里真实存在的关键步骤。">
        <div class="grid gap-3 md:grid-cols-2">
          <div
            v-for="item in timeline.slice(0, 6)"
            :key="`${item.taskId}-${item.stage}`"
            class="app-option"
          >
            <p class="app-eyebrow">任务 {{ item.taskId }}</p>
            <p class="mt-2 text-sm font-semibold text-slate-900">{{ formatStage(item.stage) }}</p>
            <p class="mt-2 text-sm text-slate-600">{{ item.status || '待处理' }}</p>
          </div>
        </div>
      </SectionCard>
    </div>
  </AppShell>
</template>
