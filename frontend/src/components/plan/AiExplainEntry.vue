<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';

interface PromptItem {
  key: string;
  label: string;
}

defineProps<{
  prompts: PromptItem[];
}>();

defineEmits<{
  ask: [prompt: string];
}>();
</script>

<template>
  <SectionCard strong title="先问 AI 一句" description="不想直接开始时，可以先追问一句，把这次规划看明白。">
    <div class="flex flex-wrap gap-3">
      <button
        v-for="item in prompts"
        :key="item.key"
        type="button"
        class="rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
        @click="$emit('ask', item.label)"
      >
        {{ item.label }}
      </button>
    </div>
  </SectionCard>
</template>
