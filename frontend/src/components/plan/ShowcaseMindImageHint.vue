<template>
  <section
    class="mb-8 rounded-card border border-primary/20 bg-gradient-to-b from-primary/[0.07] to-white px-5 py-4 shadow-sm shadow-primary/[0.06] md:mb-10 md:px-6 md:py-5"
    aria-labelledby="mind-image-hint-title"
  >
    <h2
      id="mind-image-hint-title"
      class="text-sm font-bold tracking-wide text-primary md:text-base"
    >
      {{ hint.title }}
    </h2>
    <p
      class="mt-2 text-base leading-relaxed text-text-primary md:text-[1.05rem]"
    >
      {{ hint.body }}
    </p>
    <template v-if="hint.showSimpleBinaryTreeAscii">
      <pre
        class="mt-4 select-none rounded-lg border border-border/80 bg-white/90 px-4 py-3 text-center font-mono text-sm leading-relaxed text-text-secondary"
        aria-hidden="true"
      >        ●
       / \
      ●   ●
     /     \
    ●       ●</pre>
      <p class="mt-2 text-center text-xs text-text-secondary">
        极简示意：上面是根，往下最多分出左右两支
      </p>
    </template>
  </section>
</template>

<script setup lang="ts">
import type { ShowcaseMindImageHint } from '@/constants/showcaseKnowledgeConfig'

defineProps<{
  hint: ShowcaseMindImageHint
}>()
</script>
