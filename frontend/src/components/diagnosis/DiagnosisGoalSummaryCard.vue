<script setup lang="ts">
import { computed } from 'vue';
import FlowPageHero from '@/components/common/FlowPageHero.vue';

const props = defineProps<{
  sessionId: string;
  goal: string;
  course: string;
  chapter: string;
}>();

const contextItems = computed(() => [
  { label: '目标导向', value: props.goal },
  { label: '学习主题', value: props.course },
  { label: '当前章节', value: props.chapter },
]);
</script>

<template>
  <FlowPageHero
    step-label="诊断"
    title="先确认你的当前起点"
    description="这一步不是考试，而是把你现在的基础、节奏和目标偏好整理成一份能力快照，作为下一步学习规划的起点。"
    meta-label="诊断记录"
    :meta-value="sessionId"
    :context-items="contextItems"
  />
</template>
