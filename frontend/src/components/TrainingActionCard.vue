<script setup lang="ts">
import PrimaryButton from '@/components/PrimaryButton.vue'

defineProps<{
  title: string
  description: string
  buttonText: string
  loading?: boolean
  disabled?: boolean
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <section class="action-card">
    <div class="action-copy">
      <span class="action-label">当前主操作</span>
      <h2>{{ title }}</h2>
      <p>{{ description }}</p>
    </div>

    <PrimaryButton class="action-button" :loading="loading" :disabled="disabled" @click="emit('action')">
      {{ buttonText }}
    </PrimaryButton>
  </section>
</template>

<style scoped>
.action-card {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) auto;
  gap: var(--space-lg);
  align-items: center;
  padding: clamp(22px, 3vw, 32px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(107, 159, 255, 0.35);
  background:
    radial-gradient(circle at top left, rgba(107, 159, 255, 0.22), transparent 35%),
    linear-gradient(145deg, rgba(20, 31, 48, 0.98), rgba(12, 18, 28, 0.96));
  box-shadow: 0 18px 40px rgba(7, 13, 24, 0.4);
}

.action-copy {
  display: grid;
  gap: 10px;
}

.action-label {
  color: var(--color-primary-hover);
  font-size: var(--font-size-sm);
}

.action-copy p {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.action-button {
  min-width: 180px;
}

@media (max-width: 780px) {
  .action-card {
    grid-template-columns: 1fr;
  }

  .action-button {
    width: 100%;
  }
}
</style>
