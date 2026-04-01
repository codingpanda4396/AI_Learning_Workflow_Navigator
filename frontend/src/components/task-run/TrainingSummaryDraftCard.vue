<template>
  <Transition
    enter-active-class="transition duration-400 ease-out"
    enter-from-class="opacity-0 translate-y-3"
    enter-to-class="opacity-100 translate-y-0"
  >
    <div
      v-if="finalDraft"
      class="rounded-2xl border border-emerald-200 bg-emerald-50/60 p-6 shadow-sm"
    >
      <p class="mb-2 text-xs font-semibold uppercase tracking-wide text-emerald-600">
        你们共同整理出的最终版理解
      </p>
      <p class="text-sm leading-relaxed text-slate-800 whitespace-pre-wrap">{{ finalDraft }}</p>
    </div>
  </Transition>
</template>

<script setup lang="ts">
defineProps<{
  finalDraft: string | null
}>()
</script>
