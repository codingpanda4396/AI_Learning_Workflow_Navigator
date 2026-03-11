<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import InfoCard from '@/components/cards/InfoCard.vue';
import { useFeedbackStore } from '@/stores/feedback';
import { formatPercent } from '@/utils/format';

const route = useRoute();
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
    <div v-else-if="dashboard" class="space-y-8">
      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoCard title="已学习节点" :value="dashboard.learnedNodeCount" />
        <InfoCard title="已掌握节点" :value="dashboard.masteredNodeCount" />
        <InfoCard title="平均掌握度" :value="formatPercent(dashboard.averageMasteryScore)" />
        <InfoCard title="当前节点" :value="dashboard.currentNodeName || '--'" :hint="dashboard.currentStageLabel" />
      </section>

      <section class="grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
        <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <h3 class="text-base font-semibold text-slate-900">趋势摘要</h3>
          <p class="mt-4 text-sm text-slate-600">最近作答次数：{{ dashboard.recentPerformance?.attemptCount ?? '--' }}</p>
          <p class="mt-2 text-sm text-slate-600">最近平均得分：{{ dashboard.recentPerformance?.averageScore ?? '--' }}</p>
          <p class="mt-2 text-sm text-slate-600">最近一次得分：{{ dashboard.recentPerformance?.latestScore ?? '--' }}</p>
          <p class="mt-4 text-sm text-slate-600">高频错误标签：{{ dashboard.recentPerformance?.topErrorTags.join('、') || '暂无' }}</p>
          <p class="mt-4 text-sm text-slate-600">Top weak points：{{ dashboard.topWeakPoints.join('、') || '暂无' }}</p>
        </div>

        <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <h3 class="text-base font-semibold text-slate-900">掌握度分布</h3>
          <div class="mt-5 space-y-4">
            <div v-for="item in dashboard.masteryNodes" :key="item.nodeId">
              <div class="flex items-center justify-between text-sm">
                <span class="font-medium text-slate-900">{{ item.nodeName }}</span>
                <span class="text-slate-500">{{ formatPercent(item.masteryScore) }}</span>
              </div>
              <div class="mt-2 h-2 rounded-full bg-slate-100">
                <div class="h-2 rounded-full bg-slate-900" :style="{ width: `${Math.min((item.masteryScore || 0) * 100, 100)}%` }" />
              </div>
            </div>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>
