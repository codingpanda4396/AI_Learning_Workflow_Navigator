<script setup lang="ts">
type PracticeStatus = 'idle' | 'generating' | 'ready' | 'failed' | 'unavailable'

defineProps<{
  status: PracticeStatus
  badge: string
  title: string
  description: string
  helper?: string
}>()
</script>

<template>
  <section class="practice-status-card" :class="status">
    <div class="status-head">
      <span class="status-label">检测状态</span>
      <span class="status-badge">{{ badge }}</span>
    </div>
    <h3>{{ title }}</h3>
    <p class="status-description">{{ description }}</p>
    <p v-if="helper" class="status-helper">{{ helper }}</p>
  </section>
</template>

<style scoped>
.practice-status-card {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.42);
  background: rgba(11, 17, 28, 0.78);
}

.status-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.status-label,
.status-helper {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.status-badge {
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: rgba(107, 159, 255, 0.14);
  color: var(--color-text);
  font-size: var(--font-size-xs);
}

h3,
.status-description {
  margin: 0;
}

.status-description {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.ready .status-badge {
  background: rgba(93, 212, 166, 0.16);
  color: var(--color-success);
}

.failed .status-badge {
  background: rgba(255, 122, 138, 0.16);
  color: var(--color-error);
}
</style>
