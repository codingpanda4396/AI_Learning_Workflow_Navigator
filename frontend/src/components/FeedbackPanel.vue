<script setup lang="ts">
import { computed } from 'vue'
import { getSystemAdjustments } from '@/constants/trainingStage'
import type { PracticeFeedbackReport } from '@/types'

const props = defineProps<{
  mode: 'empty' | 'loading' | 'ready'
  report?: PracticeFeedbackReport | null
}>()

const emit = defineEmits<{
  review: []
  nextRound: []
}>()

const summary = computed(() => props.report?.diagnosisSummary ?? '')
const weakPoints = computed(() => props.report?.weaknesses?.slice(0, 3) ?? [])
const nextSuggestions = computed(() => {
  if (!props.report) {
    return []
  }
  const items = [props.report.nextRoundAdvice]
  if (props.report.recommendedAction === 'REVIEW') {
    items.unshift('建议先回到薄弱点复习，再进入下一轮。')
  } else {
    items.unshift('可以继续下一轮学习，保持当前节奏。')
  }
  return items.filter((item) => item.trim().length > 0)
})
const systemAdjustments = computed(() => getSystemAdjustments(props.report ?? null))
</script>

<template>
  <section class="feedback-card" :class="mode">
    <div class="feedback-head">
      <div>
        <span class="feedback-label">结果反馈</span>
        <h3>{{ mode === 'ready' ? '本轮结果' : mode === 'loading' ? '系统正在整理结果' : '结果反馈' }}</h3>
      </div>
      <span v-if="mode === 'ready'" class="ready-badge">已生成</span>
    </div>

    <template v-if="mode === 'empty'">
      <p class="feedback-copy">完成练习后，这里会显示你的整体表现、薄弱点和下一步建议。</p>
    </template>

    <template v-else-if="mode === 'loading'">
      <p class="feedback-copy">请稍等，系统正在分析你的作答情况。</p>
      <div class="loading-bar"></div>
    </template>

    <template v-else>
      <div class="feedback-section">
        <h4>总结</h4>
        <p class="feedback-copy">{{ summary }}</p>
      </div>

      <div class="feedback-section">
        <h4>薄弱点</h4>
        <ul v-if="weakPoints.length" class="feedback-list">
          <li v-for="item in weakPoints" :key="item">{{ item }}</li>
        </ul>
        <p v-else class="feedback-copy">当前没有明显薄弱点，可以继续保持。</p>
      </div>

      <div class="feedback-section" v-if="systemAdjustments.length">
        <h4>系统调整</h4>
        <ul class="feedback-list">
          <li v-for="item in systemAdjustments" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="feedback-section" v-if="nextSuggestions.length">
        <h4>下一步</h4>
        <ul class="feedback-list">
          <li v-for="item in nextSuggestions" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="feedback-actions">
        <button type="button" class="ghost-btn" @click="emit('review')">先复习</button>
        <button type="button" class="primary-btn" @click="emit('nextRound')">继续下一轮</button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.feedback-card {
  display: grid;
  gap: var(--space-lg);
  padding: clamp(18px, 2.6vw, 28px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(61, 80, 104, 0.48);
  background: rgba(16, 22, 33, 0.92);
}

.feedback-card.empty {
  background: rgba(13, 19, 30, 0.86);
}

.feedback-card.ready {
  border-color: rgba(93, 212, 166, 0.32);
}

.feedback-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: center;
}

.feedback-label {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.ready-badge {
  padding: 6px 10px;
  border-radius: var(--radius-full);
  background: rgba(93, 212, 166, 0.14);
  color: var(--color-success);
  font-size: var(--font-size-xs);
}

.feedback-section {
  display: grid;
  gap: 8px;
}

.feedback-section h4 {
  font-size: var(--font-size-md);
  margin: 0;
}

.feedback-copy,
.feedback-list {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.feedback-list {
  padding-left: 20px;
}

.feedback-actions {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.ghost-btn,
.primary-btn {
  min-height: 44px;
  padding: 0 18px;
  border-radius: var(--radius-md);
  font-weight: 600;
}

.ghost-btn {
  border: 1px solid rgba(61, 80, 104, 0.56);
  color: var(--color-text);
  background: rgba(10, 15, 24, 0.9);
}

.primary-btn {
  color: #fff;
  background: var(--color-primary);
}

.loading-bar {
  width: 100%;
  height: 10px;
  border-radius: var(--radius-full);
  background:
    linear-gradient(90deg, rgba(107, 159, 255, 0.12), rgba(107, 159, 255, 0.6), rgba(107, 159, 255, 0.12));
  background-size: 220px 100%;
  animation: loading 1.2s linear infinite;
}

@keyframes loading {
  from {
    background-position: -220px 0;
  }
  to {
    background-position: 220px 0;
  }
}
</style>
