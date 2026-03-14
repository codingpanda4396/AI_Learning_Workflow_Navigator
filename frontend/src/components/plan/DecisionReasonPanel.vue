<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';

interface ReasonItem {
  title: string;
  description: string;
}

interface AlternativeItem {
  key: string;
  title: string;
  description: string;
}

defineProps<{
  learnerGoal: string;
  currentWeaknesses: string[];
  masteryScore: string;
  basedOnCurrentState: ReasonItem[];
  decisionReasons: ReasonItem[];
  alternatives: AlternativeItem[];
}>();
</script>

<template>
  <div class="grid gap-4 xl:grid-cols-3">
    <SectionCard title="基于你的当前状态" description="先看系统拿什么做判断。">
      <div class="space-y-3">
        <div class="app-option app-option-selected">
          <p class="text-sm font-semibold text-slate-950">目标</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ learnerGoal }}</p>
        </div>
        <div class="app-option">
          <p class="text-sm font-semibold text-slate-950">当前短板</p>
          <div class="mt-2 flex flex-wrap gap-2">
            <span v-for="item in currentWeaknesses" :key="item" class="app-pill">{{ item }}</span>
          </div>
        </div>
        <div class="app-option">
          <p class="text-sm font-semibold text-slate-950">当前掌握度</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ masteryScore === '--' ? '系统暂未返回具体分值，但已经识别出现在还不适合跳过这一步。' : `当前约为 ${masteryScore}，还不到可以放心跳过的状态。` }}</p>
        </div>
        <div v-for="item in basedOnCurrentState.slice(0, 2)" :key="item.title" class="app-option">
          <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
        </div>
      </div>
    </SectionCard>

    <SectionCard title="系统为什么先推这里" description="像一张可读的 AI 权衡记录。">
      <div class="space-y-3">
        <div v-for="item in decisionReasons" :key="item.title" class="app-option app-option-selected">
          <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
        </div>
      </div>
    </SectionCard>

    <SectionCard title="为什么不是先学别的" description="系统也看过其他方向，但暂时没有把它们排到第一位。">
      <div class="space-y-3">
        <div v-for="item in alternatives" :key="item.key" class="app-option">
          <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
          <p class="mt-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
        </div>
      </div>
    </SectionCard>
  </div>
</template>
