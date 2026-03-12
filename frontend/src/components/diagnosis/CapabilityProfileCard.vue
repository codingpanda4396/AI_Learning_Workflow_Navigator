<script setup lang="ts">
import type { CapabilityProfile } from '@/types/diagnosis';

defineProps<{
  profile: CapabilityProfile;
}>();
</script>

<template>
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">Profile</p>
    <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">你的能力画像</h2>
    <p class="mt-3 text-sm leading-6 text-slate-600">
      基于这份画像，系统会调整后续学习内容和训练难度，让后面的学习更贴近你现在的状态。
    </p>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前水平</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ profile.currentLevel }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习偏好</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">{{ profile.learningPreference || '系统将按清晰、循序渐进的方式为你安排内容。' }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">时间安排</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">{{ profile.timeBudget || '建议每天安排稳定的学习时间，哪怕不长也没关系。' }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习方向</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">{{ profile.goalOrientation || '系统会先帮你补齐关键基础，再逐步进入训练。' }}</p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div>
        <p class="text-sm font-semibold text-slate-900">你的优势</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in profile.strengths"
            :key="item"
            class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div>
        <p class="text-sm font-semibold text-slate-900">当前薄弱点</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in profile.weaknesses"
            :key="item"
            class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="profile.summary" class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-700">
      {{ profile.summary }}
    </div>
  </section>
</template>
