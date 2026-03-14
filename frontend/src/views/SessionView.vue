<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import PageSection from '@/components/common/PageSection.vue';
import PrimaryActionCard from '@/components/common/PrimaryActionCard.vue';
import ProgressSummary from '@/components/common/ProgressSummary.vue';
import SecondaryInfoCard from '@/components/common/SecondaryInfoCard.vue';
import StagePill from '@/components/common/StagePill.vue';
import { formatSessionStatus, formatStage } from '@/utils/format';
import { useSessionStore } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const sessionStore = useSessionStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const summary = computed(() => overview.value?.summary);
const simpleTimeline = computed(() =>
  (overview.value?.timeline ?? []).slice(0, 5).map((item) => `${formatStage(item.stage)} / ${item.status || '待处理'}`),
);

async function loadOverview() {
  if (!sessionId.value) {
    return;
  }
  await sessionStore.fetchOverview(sessionId.value);
}

async function openPrimaryAction() {
  const path = summary.value?.primaryActionPath;
  if (!path) {
    return;
  }
  await router.push(path);
}

onMounted(loadOverview);
</script>

<template>
  <AppShell>
    <LoadingState v-if="sessionStore.loading && !overview" />

    <div v-else-if="sessionStore.error && !overview" class="space-y-4">
      <ErrorState :message="sessionStore.error" />
      <button
        type="button"
        class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
        @click="loadOverview"
      >
        重试加载会话总览
      </button>
    </div>

    <div v-else-if="overview" class="space-y-6">
      <PageSection compact>
        <div class="grid gap-3 rounded-[1.6rem] border border-slate-200 bg-slate-50 p-4 md:grid-cols-3 md:p-5">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">课程</p>
            <p class="mt-2 text-sm font-medium text-slate-900">{{ overview.courseId }} / {{ overview.chapterId }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前阶段</p>
            <div class="mt-2">
              <StagePill :stage="overview.currentStage || 'STRUCTURE'" />
              <p class="mt-2 text-sm font-medium text-slate-700">{{ formatSessionStatus(overview.sessionStatus) }}</p>
            </div>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">进度</p>
            <p class="mt-2 text-sm font-medium text-slate-900">
              {{ overview.progress.completedTaskCount }} / {{ overview.progress.totalTaskCount }}
            </p>
          </div>
        </div>
      </PageSection>

      <PrimaryActionCard
        eyebrow="当前焦点"
        :title="summary?.currentTaskTitle || '当前会话'"
        :description="summary?.currentTaskDescription || '打开最新有效的会话入口。'"
        :helper="summary?.nextStepHint || '下一步会在状态刷新后出现。'"
        :button-label="summary?.primaryActionLabel || '打开会话'"
        @action="openPrimaryAction"
      />

      <div class="grid gap-5 lg:grid-cols-3">
        <ProgressSummary :progress="overview.progress" />
        <SecondaryInfoCard title="最近报告" :description="summary?.recentReportSummary || '暂时还没有报告。'" />
        <SecondaryInfoCard title="学习目标" :description="overview.goalText || '当前会话还没有目标说明。'" />
      </div>

      <PageSection
        v-if="simpleTimeline.length"
        compact
        title="任务时间线"
        description="这里只保留当前会话中已经真实规划出的步骤。"
      >
        <div class="grid gap-3 md:grid-cols-2">
          <div
            v-for="(item, index) in simpleTimeline"
            :key="`${item}-${index}`"
            class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-700"
          >
            {{ item }}
          </div>
        </div>
      </PageSection>
    </div>
  </AppShell>
</template>
