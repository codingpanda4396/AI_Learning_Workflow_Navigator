<script setup lang="ts">
import PrimaryButton from '@/components/PrimaryButton.vue'

defineProps<{
  title: string
  subtitle: string
  progress: string
  updatedAt: string
  stage: string
  loading?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  continue: []
}>()
</script>

<template>
  <section class="continue-card">
    <div class="continue-copy">
      <p class="eyebrow">继续上次学习</p>
      <h3>{{ title }}</h3>
      <p class="subtitle">{{ subtitle }}</p>
      <div class="meta-row">
        <span>{{ stage }}</span>
        <span>{{ progress }}</span>
        <span>{{ updatedAt }}</span>
      </div>
    </div>

    <PrimaryButton type="button" :disabled="disabled" :loading="loading" @click="emit('continue')">
      继续学习
    </PrimaryButton>
  </section>
</template>

<style scoped>
.continue-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: center;
  padding: 22px;
  border: 1px solid rgba(61, 80, 104, 0.48);
  border-radius: var(--radius-xl);
  background: rgba(14, 20, 31, 0.92);
}

.continue-copy {
  display: grid;
  gap: 8px;
}

.eyebrow,
.meta-row {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.continue-copy h3,
.subtitle {
  margin: 0;
}

.subtitle {
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

@media (max-width: 720px) {
  .continue-card {
    grid-template-columns: 1fr;
  }
}
</style>
