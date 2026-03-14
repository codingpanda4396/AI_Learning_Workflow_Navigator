<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';
import type { StartLearningForm } from '@/types/home';

const model = defineModel<StartLearningForm>({ required: true });

defineProps<{
  disabled: boolean;
  loading?: boolean;
}>();

defineEmits<{
  submit: [];
}>();
</script>

<template>
  <section class="app-card app-card-strong app-card-padding">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
      <div class="max-w-2xl">
        <p class="app-eyebrow">开始学习</p>
        <h2 class="mt-2 text-[28px] font-semibold tracking-[-0.035em] text-slate-950">先把这一步说清楚</h2>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          用一句目标告诉系统你想学什么，后面会自动进入诊断和学习规划。
        </p>
      </div>
      <span class="app-pill">同一时间只保留一个进行中的学习会话</span>
    </div>

    <div class="mt-6 grid gap-4 lg:grid-cols-[1.5fr_1fr_1fr_auto]">
      <label class="block">
        <span class="app-eyebrow">学习目标</span>
        <input
          v-model="model.goal"
          type="text"
          class="app-input mt-2"
          placeholder="例如：理解最短路径算法为什么这样做"
        />
      </label>

      <label class="block">
        <span class="app-eyebrow">课程</span>
        <input
          v-model="model.course"
          type="text"
          class="app-input mt-2"
          placeholder="例如：数据结构"
        />
      </label>

      <label class="block">
        <span class="app-eyebrow">章节</span>
        <input
          v-model="model.chapter"
          type="text"
          class="app-input mt-2"
          placeholder="例如：图"
        />
      </label>

      <div class="flex items-end">
        <AppButton
          size="lg"
          :disabled="disabled"
          :loading="loading"
          class="w-full lg:min-w-[180px]"
          @click="$emit('submit')"
        >
          开始这一小步
        </AppButton>
      </div>
    </div>

    <div v-if="disabled" class="app-hint mt-4">
      你已经有一个进行中的会话了，先继续当前主线会更顺。
    </div>
  </section>
</template>
