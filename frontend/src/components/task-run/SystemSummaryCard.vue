<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">系统总结</p>
    <p class="mt-1 text-sm leading-relaxed text-slate-900">{{ summary }}</p>
    <ul class="mt-2 list-disc space-y-1 pl-5 text-xs text-slate-600">
      <li>你本轮已经完成从轮廓到机制再到表达的完整链路。</li>
      <li>后续重点是把最易混淆点转成可执行判断动作。</li>
      <li>反思完成后即可进入学习报告。</li>
    </ul>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  summary: string
}>()
</script>
