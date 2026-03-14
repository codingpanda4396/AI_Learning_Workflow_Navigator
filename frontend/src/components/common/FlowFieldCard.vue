<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  label: string;
  hint?: string;
  tone?: 'default' | 'soft' | 'strong';
}>(), {
  hint: '',
  tone: 'default',
});

const toneClass = computed(() => {
  if (props.tone === 'strong') {
    return 'border-slate-950 bg-slate-950 text-white shadow-[0_18px_45px_rgba(15,23,42,0.16)]';
  }
  if (props.tone === 'soft') {
    return 'border-slate-200 bg-slate-50/90 text-slate-950';
  }
  return 'border-slate-200 bg-white text-slate-950 shadow-sm';
});

const labelClass = computed(() => (props.tone === 'strong' ? 'text-slate-300/85' : 'text-slate-400'));
const hintClass = computed(() => (props.tone === 'strong' ? 'text-slate-300/90' : 'text-slate-500'));
</script>

<template>
  <div class="rounded-[1.6rem] border p-4 md:p-5" :class="toneClass">
    <p class="text-[11px] font-semibold uppercase tracking-[0.18em]" :class="labelClass">{{ label }}</p>
    <div class="mt-3 min-h-[2.75rem] break-words">
      <slot />
    </div>
    <p v-if="hint" class="mt-3 text-xs leading-5 break-words" :class="hintClass">{{ hint }}</p>
  </div>
</template>
