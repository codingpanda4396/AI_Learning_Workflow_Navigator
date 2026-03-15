<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useFeedbackStore } from '@/stores/feedback';
import { formatPercent } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const feedbackStore = useFeedbackStore();
const sessionId = computed(() => Number(route.params.sessionId));
const dashboard = computed(() => feedbackStore.growthDashboard);

onMounted(async () => {
  await feedbackStore.fetchGrowthDashboard(sessionId.value);
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="feedbackStore.loading && !dashboard" />
    <ErrorState v-else-if="feedbackStore.error && !dashboard" :message="feedbackStore.error" />

    <div v-else-if="dashboard" class="app-stack-lg">
      <section class="app-hero">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-2xl">
            <p class="app-eyebrow">成长看板</p>
            <h1 class="app-title-lg mt-4">把进步看清楚，但不过度打扰</h1>
            <p class="app-text-lead mt-4">
              这里保留最有用的结果：你已经学了多少、现在卡在哪、下一步更适合怎么走。
            </p>
          </div>
          <AppButton variant="secondary" @click="router.push(`/learn/${sessionId}`)">返回学习导航</AppButton>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="app-stat">
          <p class="app-stat-label">已学习节点</p>
          <p class="app-stat-value">{{ dashboard.learnedNodeCount ?? '--' }}</p>
        </div>
        <div class="app-stat">
          <p class="app-stat-label">已掌握节点</p>
          <p class="app-stat-value">{{ dashboard.masteredNodeCount ?? '--' }}</p>
        </div>
        <div class="app-stat">
          <p class="app-stat-label">平均掌握度</p>
          <p class="app-stat-value">{{ formatPercent(dashboard.averageMasteryScore) }}</p>
        </div>
        <div class="app-stat">
          <p class="app-stat-label">当前节点</p>
          <p class="mt-2 text-sm font-semibold leading-6 text-slate-900">{{ dashboard.currentNodeName || '--' }}</p>
        </div>
      </section>

      <div class="grid gap-5 lg:grid-cols-[320px_minmax(0,1fr)]">
        <SectionCard title="最近表现">
          <div class="space-y-3 text-sm leading-7 text-slate-600">
            <p>最近作答次数：{{ dashboard.recentPerformance?.attemptCount ?? '--' }}</p>
            <p>最近平均得分：{{ dashboard.recentPerformance?.averageScore ?? '--' }}</p>
            <p>最近一次得分：{{ dashboard.recentPerformance?.latestScore ?? '--' }}</p>
            <p>高频错误标签：{{ dashboard.recentPerformance?.topErrorTags.join('、') || '暂无' }}</p>
            <p>主要薄弱点：{{ dashboard.topWeakPoints.join('、') || '暂无' }}</p>
          </div>
        </SectionCard>

        <SectionCard title="掌握度分布">
          <div class="grid gap-4">
            <div v-for="item in dashboard.masteryNodes" :key="item.nodeId" class="app-option">
              <div class="flex items-center justify-between gap-4 text-sm">
                <span class="font-semibold text-slate-900">{{ item.nodeName }}</span>
                <span class="text-slate-500">{{ formatPercent(item.masteryScore) }}</span>
              </div>
              <div class="app-progress-track mt-3">
                <div class="app-progress-fill" :style="{ width: `${Math.min((item.masteryScore || 0) * 100, 100)}%` }" />
              </div>
            </div>
          </div>
        </SectionCard>
      </div>
    </div>
  </AppShell>
</template>
