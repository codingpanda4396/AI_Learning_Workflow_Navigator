<script setup lang="ts">
import { computed } from 'vue';
import AppButton from '@/components/ui/AppButton.vue';
import { STATE_COPY } from '@/constants/learningFlow';

const props = withDefaults(
  defineProps<{
    state: 'loading' | 'empty' | 'error' | 'blocked';
    message?: string;
    actionLabel?: string;
  }>(),
  {
    message: '',
    actionLabel: '重新加载',
  },
);

const emit = defineEmits<{ (e: 'action'): void }>();

const copy = computed(() => {
  switch (props.state) {
    case 'loading':
      return { title: STATE_COPY.LOADING, hint: STATE_COPY.LOADING_HINT };
    case 'empty':
      return { title: props.message || STATE_COPY.EMPTY, hint: STATE_COPY.EMPTY_HINT };
    case 'error':
      return { title: STATE_COPY.ERROR, hint: props.message || STATE_COPY.ERROR_HINT };
    case 'blocked':
      return { title: STATE_COPY.BLOCKED, hint: props.message || STATE_COPY.BLOCKED_HINT };
    default:
      return { title: '', hint: '' };
  }
});

const showAction = computed(() => (props.state === 'error' || props.state === 'blocked') && props.actionLabel);
</script>

<template>
  <div class="mx-auto max-w-md rounded-2xl border border-slate-200/80 bg-slate-50/50 p-8 text-center">
    <p class="text-base font-semibold text-slate-900">{{ copy.title }}</p>
    <p class="mt-2 text-sm text-slate-600">{{ copy.hint }}</p>
    <div v-if="state === 'loading'" class="mt-6 flex justify-center">
      <div class="h-8 w-8 animate-pulse rounded-full bg-slate-200" />
    </div>
    <div v-else-if="showAction" class="mt-6">
      <AppButton variant="secondary" @click="emit('action')">{{ actionLabel }}</AppButton>
    </div>
    <div v-else-if="state === 'empty'" class="mt-6">
      <slot />
    </div>
  </div>
</template>
