<template>
  <header
    class="sticky top-0 z-10 border-b border-border bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/80"
  >
    <div class="mx-auto flex h-14 max-w-4xl items-center justify-between px-6">
      <router-link
        to="/goal"
        class="text-lg font-semibold text-text-primary transition-colors hover:text-primary"
      >
        Lumina AI
      </router-link>
      <WorkflowStepper :current="current ?? 'goal'" />
    </div>
  </header>
</template>

<script setup lang="ts">
import WorkflowStepper from './WorkflowStepper.vue'

defineProps<{
  current?: 'goal' | 'diagnosis' | 'plan' | 'task' | 'report'
}>()
</script>
