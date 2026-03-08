<script setup lang="ts">
import SkeletonBox from './SkeletonBox.vue'
</script>

<template>
  <div class="session-skeleton">
    <!-- Header -->
    <div class="skeleton-header">
      <SkeletonBox width="60px" height="36px" />
      <SkeletonBox width="120px" height="32px" />
    </div>

    <!-- Goal Section -->
    <div class="skeleton-section">
      <SkeletonBox width="80px" height="16px" />
      <SkeletonBox width="100%" height="24px" class="mt-sm" />
    </div>

    <!-- Mastery Section -->
    <div class="skeleton-section">
      <SkeletonBox width="60px" height="16px" />
      <div class="mastery-items">
        <div class="mastery-item" v-for="i in 2" :key="i">
          <SkeletonBox width="80px" height="16px" />
          <SkeletonBox width="100%" height="8px" class="mt-sm" />
          <SkeletonBox width="40px" height="16px" />
        </div>
      </div>
    </div>

    <!-- Timeline Section -->
    <div class="skeleton-section">
      <SkeletonBox width="80px" height="16px" />
      <div class="timeline-items">
        <div class="timeline-item" v-for="i in 3" :key="i">
          <SkeletonBox width="24px" height="24px" variant="circle" />
          <div class="task-info">
            <SkeletonBox width="60px" height="16px" />
            <SkeletonBox width="50px" height="14px" class="mt-xs" />
          </div>
        </div>
      </div>
    </div>

    <!-- Next Action -->
    <SkeletonBox width="200px" height="48px" class="skeleton-btn" />
  </div>
</template>

<style scoped>
.session-skeleton {
  padding: 1.5rem;
}

.skeleton-header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.skeleton-section {
  margin-bottom: 2rem;
}

.mt-sm {
  margin-top: 0.5rem;
}

.mt-xs {
  margin-top: 0.25rem;
}

.mastery-items {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.mastery-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.timeline-items {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-top: 1rem;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--color-bg-elevated);
  border-radius: 8px;
}

.task-info {
  flex: 1;
}

.skeleton-btn {
  display: block;
  margin: 2rem auto;
  border-radius: 8px;
}
</style>
