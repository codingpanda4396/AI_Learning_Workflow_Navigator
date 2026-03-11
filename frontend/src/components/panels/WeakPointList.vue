<script setup lang="ts">
import type { WeakPointItem } from '@/types/feedback';
import { formatPercent } from '@/utils/format';

defineProps<{
  items: WeakPointItem[];
}>();
</script>

<template>
  <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <h3 class="text-base font-semibold text-slate-900">薄弱点</h3>
    <div v-if="items.length" class="mt-4 space-y-4">
      <div v-for="item in items" :key="item.nodeId" class="rounded-2xl bg-slate-50 p-4">
        <div class="flex items-center justify-between gap-4">
          <p class="font-medium text-slate-900">{{ item.nodeName }}</p>
          <span class="text-xs text-slate-500">掌握度 {{ formatPercent(item.masteryScore) }}</span>
        </div>
        <p class="mt-2 text-sm text-slate-600">{{ item.reasons.join('；') || '需要更多训练样本' }}</p>
      </div>
    </div>
    <p v-else class="mt-4 text-sm text-slate-500">当前没有返回薄弱点数据。</p>
  </div>
</template>
