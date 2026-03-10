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
    items.unshift('推荐先进入复习，优先补齐本轮训练中的薄弱点')
  } else {
    items.unshift('可以进入下一轮学习，继续推进当前章节')
  }
  return items.filter((item) => item.trim().length > 0)
})
const systemAdjustments = computed(() => getSystemAdjustments(props.report ?? null))
</script>

<template>
  <section class="feedback-card" :class="mode">
    <div class="feedback-head">
      <div>
        <span class="feedback-label">作答反馈</span>
        <h3>{{ mode === 'ready' ? '训练反馈' : mode === 'loading' ? '正在分析你的作答表现' : '作答反馈' }}</h3>
      </div>
      <span v-if="mode === 'ready'" class="ready-badge">已生成</span>
    </div>

    <template v-if="mode === 'empty'">
      <p class="feedback-copy">
        你还没有完成本章测验。提交答案后，这里会展示：
      </p>
      <ul class="feedback-list">
        <li>总体表现</li>
        <li>薄弱点分析</li>
        <li>下一步建议</li>
      </ul>
      <p class="feedback-tip">提交答案后，系统会给出反馈总结、薄弱点与下一步建议</p>
    </template>

    <template v-else-if="mode === 'loading'">
      <p class="feedback-copy">请稍候，系统将生成薄弱点分析与后续建议。</p>
      <div class="loading-bar"></div>
    </template>

    <template v-else>
      <div class="feedback-section">
        <h4>反馈摘要</h4>
        <p class="feedback-copy">{{ summary }}</p>
      </div>

      <div class="feedback-section">
        <h4>薄弱点分析</h4>
        <ul v-if="weakPoints.length" class="feedback-list">
          <li v-for="item in weakPoints" :key="item">{{ item }}</li>
        </ul>
        <p v-else class="feedback-copy">当前没有明显的集中薄弱点，可以继续保持训练节奏。</p>
      </div>

      <div class="feedback-section">
        <h4>系统调整</h4>
        <ul class="feedback-list">
          <li v-for="item in systemAdjustments" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="feedback-section">
        <h4>下一步建议</h4>
        <ul class="feedback-list">
          <li v-for="item in nextSuggestions" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="feedback-actions">
        <button type="button" class="ghost-btn" @click="emit('review')">进入复习</button>
        <button type="button" class="primary-btn" @click="emit('nextRound')">下一轮学习</button>
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
  background: linear-gradient(160deg, rgba(12, 30, 28, 0.9), rgba(14, 21, 31, 0.94));
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
}

.feedback-copy,
.feedback-tip,
.feedback-list {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.feedback-list {
  margin: 0;
  padding-left: 20px;
}

.feedback-actions {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.ghost-btn,
.primary-btn {
  min-height: 46px;
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
