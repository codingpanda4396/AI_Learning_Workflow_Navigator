<script setup lang="ts">
interface KickoffData {
  firstAction: string;
  systemLead: string;
  firstCheckpoint: string;
  ifPerformWell: string;
  ifStillStruggle: string;
  ifNoTime: string;
  steps: string[];
}

defineProps<{
  kickoff: KickoffData;
}>();
</script>

<template>
  <section class="app-card app-card-strong app-card-padding">
    <h2 class="text-xl font-semibold text-slate-950">确认后怎么开始</h2>
    <div class="mt-4 grid gap-3 md:grid-cols-3">
      <article class="rounded-2xl border border-slate-100 bg-white px-4 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">第一步</p>
        <p class="mt-2 text-sm leading-7 text-slate-800">{{ kickoff.firstAction }}</p>
      </article>
      <article class="rounded-2xl border border-slate-100 bg-white px-4 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">系统先怎么带你</p>
        <p class="mt-2 text-sm leading-7 text-slate-800">{{ kickoff.systemLead }}</p>
      </article>
      <article class="rounded-2xl border border-slate-100 bg-white px-4 py-4">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">第一个检查点</p>
        <p class="mt-2 text-sm leading-7 text-slate-800">{{ kickoff.firstCheckpoint }}</p>
      </article>
    </div>

    <ol class="mt-4 space-y-2">
      <li
        v-for="(item, index) in kickoff.steps.slice(0, 4)"
        :key="`kickoff-${index}`"
        class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-7 text-slate-700"
      >
        {{ index + 1 }}. {{ item }}
      </li>
    </ol>

    <div class="mt-4 grid gap-3 md:grid-cols-3">
      <article class="rounded-2xl border border-emerald-100 bg-emerald-50/70 px-4 py-3">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-700">如果表现稳定</p>
        <p class="mt-2 text-sm leading-7 text-emerald-900">{{ kickoff.ifPerformWell }}</p>
      </article>
      <article class="rounded-2xl border border-amber-100 bg-amber-50/70 px-4 py-3">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-amber-700">如果还在卡住</p>
        <p class="mt-2 text-sm leading-7 text-amber-900">{{ kickoff.ifStillStruggle }}</p>
      </article>
      <article class="rounded-2xl border border-slate-200 bg-slate-100/70 px-4 py-3">
        <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-600">如果今天时间少</p>
        <p class="mt-2 text-sm leading-7 text-slate-800">{{ kickoff.ifNoTime }}</p>
      </article>
    </div>
  </section>
</template>
