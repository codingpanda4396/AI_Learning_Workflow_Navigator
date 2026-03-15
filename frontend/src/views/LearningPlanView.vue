<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import SkeletonBlock from '@/components/ui/SkeletonBlock.vue';
import { DEFAULT_PLAN_ADJUSTMENTS } from '@/constants/learningPlan';
import { useLearningPlanStore } from '@/stores/learningPlan';
import { getPreviewMetricsSnapshot, trackPreviewAccepted, trackPreviewShown } from '@/utils/previewMetrics';
import { buildPreviewViewModel } from '@/utils/usePreviewViewModel';

const route = useRoute();
const router = useRouter();
const learningPlanStore = useLearningPlanStore();

const preview = computed(() => learningPlanStore.preview);
const error = computed(() => learningPlanStore.error);
const previewVm = computed(() => (preview.value ? buildPreviewViewModel(preview.value) : null));
const explanationExpanded = ref(false);

const context = computed(() => {
  const sessionId = Number(route.query.sessionId ?? 0);
  const diagnosisId = String(route.query.diagnosisId ?? '').trim();
  return {
    sessionId: Number.isFinite(sessionId) && sessionId > 0 ? sessionId : undefined,
    diagnosisId,
    goalText: String(route.query.goal ?? '').trim() || '先把当前最值得推进的一步学明白',
    courseName: String(route.query.course ?? '').trim() || '当前学习主题',
    chapterName: String(route.query.chapter ?? '').trim() || '当前章节',
  };
});

const viewState = computed(() => {
  if (learningPlanStore.loading && !preview.value) return 'loading';
  if (error.value && !preview.value) return 'error';
  if (learningPlanStore.confirming) return 'confirming';
  return 'ready';
});

function normalizeTaskName(value: string): string {
  const source = value.trim();
  if (!source) {
    return '理解当前知识点的基本结构';
  }
  if (source.includes('链表') || /linked\s*list/i.test(source)) {
    return '理解链表的基本结构';
  }
  if (!/[a-zA-Z]/.test(source)) {
    return source;
  }
  const matchedZh = source.match(/[\u4e00-\u9fa5]{2,}/g)?.[0];
  if (matchedZh) {
    return `理解${matchedZh}的基本结构`;
  }
  const dict: Record<string, string> = {
    'foundation of tree': '理解树的基本结构',
    'foundation of stack': '理解栈的基本结构',
    'foundation of queue': '理解队列的基本结构',
    'foundation of hash table': '理解哈希表的基本结构',
  };
  const key = source.toLowerCase();
  return dict[key] || '理解当前知识点的基本结构';
}

const taskName = computed(() => {
  const fromAction = preview.value?.nextActionsDetail?.[0]?.title;
  const fromEntry = preview.value?.recommendedEntry?.title;
  const fromTaskPreview = preview.value?.taskPreviews?.[0]?.title;
  return normalizeTaskName(String(fromAction || fromEntry || fromTaskPreview || ''));
});

const taskEstimate = computed(() => previewVm.value?.hero.task.estimate || '5 分钟');

const taskChecklist = computed(() => {
  const detailActions = (preview.value?.nextActionsDetail ?? [])
    .map((item) => item.learnerAction || item.title)
    .filter((item): item is string => Boolean(item && item.trim()))
    .slice(0, 3);
  if (detailActions.length) {
    return detailActions;
  }
  const fallback = (preview.value?.nextActionsV2 ?? [])
    .filter((item): item is string => Boolean(item && item.trim()))
    .slice(0, 3);
  return fallback.length ? fallback : ['写出链表节点结构 Node', '实现头插法 insertHead', '使用测试数据运行程序'];
});

const outcomeList = computed(() => {
  const expectedGain = preview.value?.expectedGain?.trim();
  if (expectedGain) {
    return [expectedGain];
  }
  return ['理解链表节点结构', '理解 next 指针', '能写出基础链表结构'];
});

async function loadPlan() {
  if (!context.value.diagnosisId || !context.value.goalText) {
    learningPlanStore.error = '还缺少生成规划所需的诊断信息，请先完成诊断。';
    return;
  }
  try {
    await learningPlanStore.generatePreview({
      ...context.value,
      adjustments: learningPlanStore.request?.adjustments ?? DEFAULT_PLAN_ADJUSTMENTS,
    });
  } catch {
    return;
  }
}

async function startLearning() {
  try {
    const result = await learningPlanStore.confirmPlan();
    if (preview.value?.id) {
      trackPreviewAccepted(preview.value.id, result.firstTaskId);
      console.info('[metrics] preview accepted', getPreviewMetricsSnapshot());
    }
    if (result.firstTaskId) {
      await router.push(`/tasks/${result.firstTaskId}/run`);
      return;
    }
    if (result.nextPage) {
      await router.push(result.nextPage);
      return;
    }
    if (result.sessionId) {
      await router.push(`/sessions/${result.sessionId}`);
    }
  } catch {
    return;
  }
}

watch(
  () => [route.query.sessionId, route.query.diagnosisId, route.query.goal, route.query.course, route.query.chapter],
  async () => {
    explanationExpanded.value = false;
    await loadPlan();
  },
  { immediate: true },
);

watch(
  () => preview.value?.id,
  (previewId) => {
    if (!previewId) {
      return;
    }
    trackPreviewShown(previewId);
    console.info('[metrics] preview shown', getPreviewMetricsSnapshot());
  },
);

onBeforeUnmount(() => {
  learningPlanStore.reset();
});
</script>

<template>
  <AppShell>
    <div class="mx-auto max-w-[1120px] space-y-8 pb-12">
      <div v-if="viewState === 'loading'" class="space-y-6">
        <section class="app-hero min-h-[320px]">
          <div class="grid h-full gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
            <div class="space-y-4">
              <SkeletonBlock width="96px" :height="16" rounded="999px" />
              <SkeletonBlock width="72%" :height="56" rounded="14px" />
              <SkeletonBlock width="88%" :height="20" rounded="10px" />
              <SkeletonBlock width="70%" :height="20" rounded="10px" />
            </div>
            <SkeletonBlock :height="220" rounded="20px" />
          </div>
        </section>
        <SkeletonBlock :height="180" rounded="24px" />
        <SkeletonBlock :height="220" rounded="24px" />
      </div>

      <div v-else-if="viewState === 'error'" class="space-y-4">
        <ErrorState :message="error || '学习规划生成失败。'" />
        <AppButton @click="loadPlan">重新生成学习规划</AppButton>
      </div>

      <template v-else-if="preview && previewVm">
        <section class="space-y-5">
          <header class="space-y-2">
            <p class="text-sm font-medium text-slate-500">学习规划</p>
            <h1 class="text-3xl font-semibold tracking-tight text-slate-900">AI 学习导航</h1>
          </header>

          <section class="app-card rounded-2xl p-8 shadow-[0_12px_32px_rgba(15,23,42,0.08)]">
            <p class="text-sm font-medium text-slate-500">下一步任务</p>
            <h2 class="mt-2 text-2xl font-semibold text-slate-900">{{ taskName }}</h2>
            <p class="mt-2 text-sm text-slate-500">预计时间：{{ taskEstimate }}</p>

            <div class="mt-8">
              <h3 class="text-base font-semibold text-slate-900">你的任务</h3>
              <ol class="mt-3 space-y-2 text-sm leading-7 text-slate-700">
                <li v-for="(item, index) in taskChecklist" :key="`task-${index}`">{{ index + 1 }} {{ item }}</li>
              </ol>
            </div>

            <div class="mt-8">
              <h3 class="text-base font-semibold text-slate-900">完成后你将掌握</h3>
              <ul class="mt-3 space-y-2 text-sm leading-7 text-slate-700">
                <li v-for="(item, index) in outcomeList" :key="`gain-${index}`">- {{ item }}</li>
              </ul>
            </div>

            <div class="mt-8">
              <AppButton
                variant="primary"
                block
                :loading="learningPlanStore.confirming"
                class="!min-h-[44px] !rounded-xl !bg-slate-900 !shadow-none transition-transform duration-200 hover:-translate-y-0.5 hover:!bg-slate-800"
                @click="startLearning"
              >
                开始学习
              </AppButton>
            </div>
          </section>

          <section class="app-card rounded-2xl p-6">
            <button
              type="button"
              class="flex w-full items-center justify-between text-left"
              @click="explanationExpanded = !explanationExpanded"
            >
              <div>
                <p class="text-sm text-slate-500">AI 解释</p>
                <h3 class="mt-1 text-lg font-semibold text-slate-900">为什么推荐这一步</h3>
              </div>
              <span class="text-sm font-medium text-slate-600">{{ explanationExpanded ? '收起' : '展开' }}</span>
            </button>
            <div v-if="explanationExpanded" class="mt-4 space-y-3 text-sm leading-7 text-slate-700">
              <p><span class="font-medium text-slate-900">学习偏好：</span>{{ preview.recommendedStrategy?.label || '系统默认策略' }}</p>
              <p><span class="font-medium text-slate-900">时间节奏：</span>{{ previewVm.hero.task.estimate }}</p>
              <p><span class="font-medium text-slate-900">当前基础：</span>{{ preview.learnerSnapshotV2?.currentState || '系统正在根据你的完成情况持续校准' }}</p>
              <p><span class="font-medium text-slate-900">推荐原因：</span>{{ previewVm.explanation.whyThisStep }}</p>
            </div>
          </section>
        </section>
      </template>

      <EmptyStatePanel
        v-else
        title="还没有拿到这次学习规划"
        description="先完成诊断，系统才能判断你现在最该先学哪一步。"
      >
        <AppButton @click="router.push('/')">返回首页</AppButton>
      </EmptyStatePanel>
    </div>
  </AppShell>
</template>
