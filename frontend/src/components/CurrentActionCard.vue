<script setup lang="ts">
import PrimaryButton from '@/components/PrimaryButton.vue'

defineProps<{
  eyebrow?: string
  title: string
  description: string
  helper?: string
  buttonText: string
  statusLabel?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  action: []
}>()
</script>

<template>
  <section class="action-card">
    <div class="action-copy">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <div class="title-row">
        <h2>{{ title }}</h2>
        <span v-if="statusLabel" class="status-chip">{{ statusLabel }}</span>
      </div>
      <p class="description">{{ description }}</p>
      <p v-if="helper" class="helper">{{ helper }}</p>
    </div>

    <PrimaryButton type="button" class="action-button" :disabled="disabled" @click="emit('action')">
      {{ buttonText }}
    </PrimaryButton>
  </section>
</template>

<style scoped>
.action-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
  padding: clamp(22px, 3vw, 30px);
  border: 1px solid rgba(61, 80, 104, 0.48);
  border-radius: var(--radius-xl);
  background: rgba(15, 21, 33, 0.94);
}

.action-copy {
  display: grid;
  gap: 10px;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.title-row h2,
.description,
.helper {
  margin: 0;
}

.description {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.helper {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.status-chip {
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: rgba(107, 159, 255, 0.14);
  color: var(--color-text);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.action-button {
  min-width: 170px;
}

@media (max-width: 720px) {
  .action-card {
    grid-template-columns: 1fr;
  }

  .action-button {
    width: 100%;
  }

  .title-row {
    flex-direction: column;
  }
}
</style>
