<template>
  <section
    class="mt-8 rounded-xl border-l-4 border-violet-500/80 bg-violet-50/80 px-4 py-4 shadow-sm ring-1 ring-violet-200/60 md:px-5 md:py-5"
    aria-labelledby="judgment-tips-heading"
  >
    <h2
      id="judgment-tips-heading"
      class="text-sm font-bold tracking-tight text-violet-950 md:text-base"
    >
      看题时，可以先这样提醒自己：
    </h2>
    <ul
      class="mt-3 space-y-2.5 text-sm font-medium leading-snug text-violet-950/95 md:text-[15px]"
    >
      <li
        v-for="(tip, i) in tips"
        :key="i"
        class="flex gap-2 pl-0.5"
      >
        <span
          class="mt-0.5 shrink-0 font-bold text-violet-600"
          aria-hidden="true"
        >
          ·
        </span>
        <span>{{ tip }}</span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    tips?: string[]
  }>(),
  {
    tips: () => [],
  }
)
</script>
