<script setup lang="ts">
import { computed } from 'vue';
import AppButton from '@/components/ui/AppButton.vue';
import type { StrategyAdjustAction } from '@/types/learningPlan';

interface StrategyOption {
  key: StrategyAdjustAction;
  title: string;
  description: string;
}

const props = withDefaults(defineProps<{
  open: boolean;
  mode?: 'strategy' | 'disagree';
  loading?: boolean;
  pendingNote?: string;
}>(), {
  mode: 'strategy',
  loading: false,
  pendingNote: '',
});

const emit = defineEmits<{
  close: [];
  select: [action: StrategyAdjustAction];
}>();

const options: StrategyOption[] = [
  { key: 'faster', title: '更快一点', description: '压缩路径，只保留现在最关键的一步。' },
  { key: 'steadier', title: '更稳一点', description: '先把前置基础补牢，再往后推。' },
  { key: 'practice-first', title: '先做再学', description: '先通过练习暴露问题，再回头补知识。' },
  { key: 'ten-minute', title: '压缩到10分钟', description: '把这一步切成更轻的开始版本。' },
  { key: 'already-know', title: '我已经会了', description: '尝试跳过当前建议，重算下一步。' },
  { key: 'not-enough-time', title: '我现在时间不多', description: '让系统优先适配你当前的可用时间。' },
  { key: 'not-clear', title: '这个解释不够清楚', description: '保留建议，但换一种表达方式。' },
];

const panelTitle = computed(() => (props.mode === 'disagree' ? '我不认同这个建议' : '换一种学法'));
const panelSubtitle = computed(() => (
  props.mode === 'disagree'
    ? '告诉系统你不同意的点，它会按新的判断重新给你第一步。'
    : '选一种更贴近你当下状态的策略，系统会据此重算路径。'
));

function onMaskClick(event: MouseEvent) {
  if (event.target === event.currentTarget) {
    emit('close');
  }
}
</script>

<template>
  <teleport to="body">
    <div v-if="open" class="fixed inset-0 z-50 bg-slate-950/30 backdrop-blur-sm" @click="onMaskClick">
      <div class="absolute inset-y-0 right-0 w-full max-w-[440px] overflow-y-auto border-l border-slate-200 bg-white px-5 py-6 shadow-[0_24px_80px_rgba(15,23,42,0.22)] sm:px-6">
        <div class="flex items-start justify-between gap-4">
          <div>
            <p class="app-eyebrow">策略调整</p>
            <h2 class="mt-2 text-[28px] font-semibold tracking-[-0.03em] text-slate-950">{{ panelTitle }}</h2>
            <p class="mt-2 text-sm leading-7 text-slate-600">{{ panelSubtitle }}</p>
          </div>
          <button type="button" class="rounded-full p-2 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700" @click="$emit('close')">
            关闭
          </button>
        </div>

        <div class="mt-6 space-y-3">
          <button
            v-for="item in options"
            :key="item.key"
            type="button"
            class="w-full rounded-[22px] border border-slate-200 bg-white px-4 py-4 text-left transition hover:-translate-y-[1px] hover:border-slate-300 hover:shadow-[0_16px_36px_rgba(15,23,42,0.08)]"
            :disabled="loading"
            @click="$emit('select', item.key)"
          >
            <p class="text-base font-semibold text-slate-950">{{ item.title }}</p>
            <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
          </button>
        </div>

        <p v-if="pendingNote" class="mt-5 rounded-[18px] border border-sky-100 bg-sky-50 px-4 py-3 text-sm leading-7 text-sky-700">
          {{ pendingNote }}
        </p>

        <div class="mt-6">
          <AppButton variant="secondary" block @click="$emit('close')">先保留当前建议</AppButton>
        </div>
      </div>
    </div>
  </teleport>
</template>
