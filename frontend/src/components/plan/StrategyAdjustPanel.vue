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
  { key: 'faster', title: '更快一点', description: '先走最短的一步。' },
  { key: 'steadier', title: '更稳一点', description: '先把基础补齐。' },
  { key: 'practice-first', title: '先做再学', description: '先试一题，再回头学。' },
  { key: 'ten-minute', title: '压缩到10分钟', description: '先开始一个轻量版本。' },
  { key: 'already-know', title: '我已经会了', description: '跳过这一步，继续重算。' },
  { key: 'not-enough-time', title: '我现在时间不多', description: '先给我更短的版本。' },
  { key: 'not-clear', title: '我还没看明白', description: '换一种更好懂的说法。' },
];

const panelTitle = computed(() => (props.mode === 'disagree' ? '我不认同这个建议' : '换一种学法'));
const panelSubtitle = computed(() => (
  props.mode === 'disagree'
    ? '告诉我哪里不对。'
    : '选一个更适合现在的方式。'
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
            <p class="app-eyebrow">调整方式</p>
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
          <AppButton variant="secondary" block @click="$emit('close')">先按这个来</AppButton>
        </div>
      </div>
    </div>
  </teleport>
</template>
