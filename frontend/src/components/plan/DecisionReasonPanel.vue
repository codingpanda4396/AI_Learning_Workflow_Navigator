<script setup lang="ts">
import SectionCard from '@/components/ui/SectionCard.vue';

interface ReasonCard {
  title: string;
  description: string;
}

defineProps<{
  cards: ReasonCard[];
}>();
</script>

<template>
  <SectionCard
    strong
    title="系统为什么建议这一步"
    description="把判断压缩成三件最关键的事。"
  >
    <div class="grid gap-3 md:grid-cols-3">
      <article
        v-for="item in cards"
        :key="item.title"
        class="rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-5"
      >
        <p class="text-sm font-semibold text-slate-950">{{ item.title }}</p>
        <p class="mt-3 line-clamp-2 text-sm leading-7 text-slate-600">{{ item.description }}</p>
      </article>
    </div>
  </SectionCard>
</template>
