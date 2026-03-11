<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import ReportSummaryCard from '@/components/cards/ReportSummaryCard.vue';
import NextActionPanel from '@/components/panels/NextActionPanel.vue';
import WeakPointList from '@/components/panels/WeakPointList.vue';
import { useFeedbackStore } from '@/stores/feedback';

const route = useRoute();
const router = useRouter();
const feedbackStore = useFeedbackStore();
const sessionId = computed(() => Number(route.params.sessionId));
const report = computed(() => feedbackStore.report);

async function submitAction(action: string) {
  await feedbackStore.submitNextAction(sessionId.value, action);
}

async function goSession() {
  await router.push(`/sessions/${sessionId.value}`);
}

async function goQuiz() {
  await router.push(`/sessions/${sessionId.value}/quiz`);
}

async function goGrowth() {
  await router.push(`/sessions/${sessionId.value}/growth`);
}

onMounted(async () => {
  await feedbackStore.fetchReport(sessionId.value);
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="feedbackStore.loading && !report" />
    <ErrorState v-else-if="feedbackStore.error && !report" :message="feedbackStore.error" />
    <div v-else-if="report" class="space-y-8">
      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <ReportSummaryCard title="本轮总体结论" :content="report.overallSummary || report.diagnosisSummary" />
        <ReportSummaryCard title="学会了什么" :content="report.strengths.join('；')" />
        <ReportSummaryCard title="哪些地方薄弱" :content="report.weaknesses.join('；')" />
        <ReportSummaryCard title="系统为什么这样建议" :content="report.nextStep?.reason || report.nextRoundAdvice" />
      </section>

      <section class="grid gap-8 lg:grid-cols-[1.1fr_0.9fr]">
        <div class="space-y-6">
          <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <h3 class="text-base font-semibold text-slate-900">各题结果摘要</h3>
            <div class="mt-4 space-y-4">
              <div v-for="item in report.questionResults" :key="item.questionId" class="rounded-2xl bg-slate-50 p-4">
                <div class="flex items-center justify-between gap-4">
                  <p class="font-medium text-slate-900">{{ item.stem }}</p>
                  <span class="text-sm text-slate-500">{{ item.score ?? '--' }} 分</span>
                </div>
                <p class="mt-2 text-sm text-slate-600">{{ item.feedback || '暂无单题反馈' }}</p>
              </div>
            </div>
          </div>

          <WeakPointList :items="report.weakPoints" />
        </div>

        <div class="space-y-6">
          <ReportSummaryCard title="掌握度 / 成长摘要" :content="`growth_recorded: ${report.growthRecorded ? 'yes' : 'no'}`" />
          <NextActionPanel
            :loading="feedbackStore.loading"
            :recommended-action="report.recommendedAction || report.nextStep?.recommendedAction"
            :suggested-action="report.suggestedNextAction"
            @submit="submitAction"
          />

          <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <div class="flex flex-col gap-3">
              <button class="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-medium text-white" @click="submitAction('REVIEW')">
                进入复习
              </button>
              <button class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-medium text-slate-700" @click="goQuiz">
                开启下一轮
              </button>
              <button class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-medium text-slate-700" @click="goSession">
                返回 session 总览
              </button>
              <button class="rounded-2xl bg-slate-100 px-5 py-3 text-sm font-medium text-slate-700" @click="goGrowth">
                查看成长看板
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>
