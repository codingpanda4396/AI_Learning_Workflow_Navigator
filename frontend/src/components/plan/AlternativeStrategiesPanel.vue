<script setup lang="ts">
import { ref, watch } from 'vue';
import type { PlanAlternative } from '@/types/learningPlan';

const props = defineProps<{
  alternatives: PlanAlternative[];
  openByDefault?: boolean;
}>();

const open = ref(Boolean(props.openByDefault));

watch(
  () => props.openByDefault,
  (next) => {
    if (typeof next === 'boolean') {
      open.value = next;
    }
  },
);
</script>

<template>
  <section v-if="alternatives.length" class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-5">
    <button
      type="button"
      class="flex w-full items-center justify-between text-left"
      @click="open = !open"
    >
      <div>
        <p class="text-sm font-semibold text-slate-900">备选方案</p>
        <p class="mt-1 text-xs text-slate-500">主推荐更优先，备选仅供你按情况切换。</p>
      </div>
      <span class="text-xs text-slate-500">{{ open ? '收起' : '展开' }}</span>
    </button>

    <div v-if="open" class="mt-4 space-y-3">
      <article
        v-for="item in alternatives"
        :key="item.key"
        class="rounded-2xl border border-slate-200 bg-white px-4 py-3"
      >
        <p class="text-sm font-semibold text-slate-900">{{ item.title }}</p>
        <p class="mt-1 text-sm leading-7 text-slate-600">{{ item.description }}</p>
      </article>
    </div>
  </section>
</template>
