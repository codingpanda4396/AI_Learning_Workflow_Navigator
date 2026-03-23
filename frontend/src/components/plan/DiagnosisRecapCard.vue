<template>
  <section
    class="mb-8 rounded-card border-2 border-primary/30 bg-gradient-to-b from-primary/[0.07] to-white p-5 shadow-md ring-1 ring-primary/15 md:p-6"
    aria-labelledby="diagnosis-recap-heading"
  >
    <h2
      id="diagnosis-recap-heading"
      class="text-lg font-bold text-text-primary md:text-xl"
    >
      我大概知道你现在的问题了<span aria-hidden="true">👇</span>
    </h2>
    <ul class="mt-4 space-y-2.5 text-sm leading-relaxed text-text-primary md:text-base">
      <li
        v-for="(line, i) in bullets"
        :key="i"
        class="flex gap-2"
      >
        <span class="mt-0.5 shrink-0 text-primary" aria-hidden="true">·</span>
        <span class="min-w-0">{{ line }}</span>
      </li>
    </ul>
    <p class="mt-4 text-sm font-semibold leading-snug text-text-primary md:text-base">
      <span aria-hidden="true">👉</span>
      {{ closingLine }}
    </p>
    <div class="mt-5 flex flex-wrap gap-3">
      <SecondaryButton @click="$emit('dismiss')">
        我知道了，看看怎么学
      </SecondaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import SecondaryButton from '@/components/ui/SecondaryButton.vue'

defineProps<{
  bullets: string[]
  closingLine: string
}>()

defineEmits<{
  dismiss: []
}>()
</script>
