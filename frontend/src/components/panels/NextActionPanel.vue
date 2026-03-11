<script setup lang="ts">
defineProps<{
  recommendedAction?: string;
  suggestedAction?: string;
  loading?: boolean;
}>();

const emit = defineEmits<{
  submit: [action: string];
}>();

const presetActions = ['REVIEW', 'NEXT_ROUND', 'CONTINUE'];

function trigger(action: string) {
  emit('submit', action);
}
</script>

<template>
  <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <h3 class="text-base font-semibold text-slate-900">下一步动作</h3>
    <p class="mt-2 text-sm text-slate-600">
      推荐：{{ recommendedAction || suggestedAction || '等待后端建议' }}
    </p>
    <div class="mt-4 flex flex-wrap gap-3">
      <button
        v-for="action in presetActions"
        :key="action"
        class="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-700 transition hover:border-slate-900 hover:text-slate-900 disabled:opacity-50"
        :disabled="loading"
        @click="trigger(action)"
      >
        {{ action }}
      </button>
    </div>
  </div>
</template>
