<script setup lang="ts">
import { ref } from 'vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';

interface PromptItem {
  key: string;
  label: string;
}

defineProps<{
  prompts: PromptItem[];
  strategyNote?: string;
}>();

defineEmits<{
  adjust: [];
  disagree: [];
  ask: [prompt: string];
}>();

const open = ref(false);
</script>

<template>
  <SectionCard strong title="想换一种学习方式？" description="需要时再展开。">
    <button
      type="button"
      class="flex w-full items-center justify-between rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-4 text-left"
      @click="open = !open"
    >
      <span class="text-base font-semibold text-slate-950">可选操作</span>
      <span class="text-sm text-slate-500">{{ open ? '收起' : '展开' }}</span>
    </button>

    <div v-if="open" class="mt-4 grid gap-5 lg:grid-cols-2">
      <div class="rounded-[24px] border border-slate-200 bg-white px-5 py-5">
        <p class="text-sm font-semibold text-slate-950">换一种学法</p>
        <div class="mt-4 flex flex-col gap-3">
          <AppButton variant="secondary" @click="$emit('adjust')">换一种学法</AppButton>
          <AppButton variant="secondary" @click="$emit('disagree')">我不认同这个建议</AppButton>
        </div>
        <p v-if="strategyNote" class="mt-4 text-sm leading-7 text-slate-600">{{ strategyNote }}</p>
      </div>

      <div class="rounded-[24px] border border-slate-200 bg-white px-5 py-5">
        <p class="text-sm font-semibold text-slate-950">问AI</p>
        <div class="mt-4 flex flex-wrap gap-3">
          <button
            v-for="item in prompts"
            :key="item.key"
            type="button"
            class="rounded-full border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-100"
            @click="$emit('ask', item.label)"
          >
            {{ item.label }}
          </button>
        </div>
      </div>
    </div>
  </SectionCard>
</template>
