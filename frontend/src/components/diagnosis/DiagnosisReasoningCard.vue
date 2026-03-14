<script setup lang="ts">
import { computed } from 'vue';
import type { DiagnosisEvidenceSource, DiagnosisReasoningStep } from '@/types/diagnosis';

const props = defineProps<{
  reasoningSteps: DiagnosisReasoningStep[];
  strengthSources: DiagnosisEvidenceSource[];
  weaknessSources: DiagnosisEvidenceSource[];
}>();

const dimensionLabelMap: Record<string, string> = {
  FOUNDATION: '基础掌握',
  EXPERIENCE: '实践经验',
  GOAL_STYLE: '学习目标',
  TIME_BUDGET: '时间投入',
  LEARNING_PREFERENCE: '学习方式',
};

function resolveDimensionLabel(rawDimension?: string, fallback = '相关维度') {
  const normalized = String(rawDimension || '').trim().toUpperCase();
  return dimensionLabelMap[normalized] || String(rawDimension || '').trim() || fallback;
}

const reasoningItems = computed(() =>
  props.reasoningSteps
    .map((item) => {
      const questionTitle = item.questionTitle?.trim() || resolveDimensionLabel(item.dimension);
      const answerLabel = item.selectedAnswerLabel?.trim();
      const conclusion = item.inferredConclusion?.trim();
      if (!conclusion) {
        return '';
      }
      if (answerLabel) {
        return `基于你在“${questionTitle}”中的回答“${answerLabel}”，系统判断：${conclusion}`;
      }
      return `基于你在“${questionTitle}”中的回答，系统判断：${conclusion}`;
    })
    .filter(Boolean)
    .slice(0, 2),
);

const strengthItems = computed(() =>
  props.strengthSources
    .map((item) => {
      const label = item.label?.trim();
      if (!label) {
        return '';
      }
      return `你在“${resolveDimensionLabel(item.dimension)}”中的回答显示：${label}，这支持了你的优势判断。`;
    })
    .filter(Boolean)
    .slice(0, 1),
);

const weaknessItems = computed(() =>
  props.weaknessSources
    .map((item) => {
      const label = item.label?.trim();
      if (!label) {
        return '';
      }
      return `你在“${resolveDimensionLabel(item.dimension)}”中的回答显示：${label}，这提示这里是当前更需要补强的环节。`;
    })
    .filter(Boolean)
    .slice(0, 1),
);

const hasAnyReasoning = computed(() =>
  reasoningItems.value.length > 0 || strengthItems.value.length > 0 || weaknessItems.value.length > 0,
);
</script>

<template>
  <section class="rounded-[2rem] border border-slate-200 bg-white px-6 py-7 shadow-[0_18px_50px_rgba(15,23,42,0.06)] md:px-8 md:py-8">
    <div class="flex items-center justify-between gap-3">
      <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">AI 个性化解释</p>
      <span class="rounded-full bg-sky-50 px-3 py-1 text-xs font-medium text-sky-700">基于你的作答</span>
    </div>
    <h3 class="mt-4 text-xl font-semibold tracking-tight text-slate-950">AI 为什么这样判断</h3>

    <div v-if="hasAnyReasoning" class="mt-5 space-y-3 text-sm leading-7 text-slate-700 md:text-base">
      <p v-for="item in reasoningItems" :key="`reasoning-${item}`" class="rounded-[1.25rem] bg-slate-50 px-4 py-3">
        {{ item }}
      </p>
      <p v-for="item in strengthItems" :key="`strength-${item}`" class="rounded-[1.25rem] bg-emerald-50 px-4 py-3 text-emerald-900">
        {{ item }}
      </p>
      <p v-for="item in weaknessItems" :key="`weakness-${item}`" class="rounded-[1.25rem] bg-amber-50 px-4 py-3 text-amber-900">
        {{ item }}
      </p>
    </div>

    <p v-else class="mt-5 rounded-[1.25rem] bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-600 md:text-base">
      系统已根据你的回答生成能力判断，后续会在学习路径中继续补充更细致的解释。
    </p>
  </section>
</template>
