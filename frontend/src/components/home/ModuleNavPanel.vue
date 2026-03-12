<script setup lang="ts">
import { useRouter } from 'vue-router';
import type { ModuleEntry } from '@/mocks/home';

defineProps<{
  entries: ModuleEntry[];
}>();

const router = useRouter();

function goTo(route: string) {
  router.push(route);
}
</script>

<template>
  <section class="rounded-[2rem] bg-white p-6 shadow-[0_18px_50px_rgba(15,23,42,0.06)] ring-1 ring-slate-200/70 md:p-7">
    <div class="flex items-center justify-between gap-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-400">Module Navigation</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">核心模块入口</h2>
        <p class="mt-2 text-sm leading-6 text-slate-600">你可以直接进入任一模块，查看当前状态或继续学习。</p>
      </div>
      <div class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">模块入口</div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      <button
        v-for="entry in entries"
        :key="entry.key"
        type="button"
        class="group rounded-[1.5rem] bg-slate-50/90 p-5 text-left transition hover:-translate-y-1 hover:bg-white hover:shadow-[0_18px_40px_rgba(14,165,233,0.10)] hover:ring-1 hover:ring-sky-200"
        @click="goTo(entry.route)"
      >
        <div class="flex items-start justify-between gap-3">
          <p class="text-base font-semibold text-slate-950 transition group-hover:text-sky-700">{{ entry.title }}</p>
          <span class="rounded-full bg-white px-2.5 py-1 text-[11px] font-medium tracking-[0.12em] text-slate-400 transition group-hover:text-sky-500">模块</span>
        </div>
        <p class="mt-3 text-sm leading-6 text-slate-600">{{ entry.description }}</p>
        <p class="mt-6 text-xs font-medium uppercase tracking-[0.2em] text-slate-400 transition group-hover:text-sky-500">进入模块</p>
      </button>
    </div>
  </section>
</template>
