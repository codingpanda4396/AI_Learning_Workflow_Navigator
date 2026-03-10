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

const summary = computed(() => props.report?.overallSummary ?? '')
const questionResults = computed(() => props.report?.questionResults ?? [])
const weakPoints = computed(() => props.report?.weaknesses ?? [])
const systemAdjustments = computed(() => getSystemAdjustments(props.report ?? null))
</script>

<template>
  <section class="feedback-card" :class="mode">
    <div class="feedback-head">
      <div>
        <span class="feedback-label">结果反馈</span>
        <h3>{{ mode === 'ready' ? '本轮反馈' : mode === 'loading' ? '系统正在整理反馈' : '反馈结果' }}</h3>
      </div>
      <span v-if="mode === 'ready'" class="ready-badge">已生成</span>
    </div>

    <template v-if="mode === 'empty'">
      <p class="feedback-copy">提交检测后，这里会展示整体总结、逐题结果、薄弱点和下一步建议。</p>
    </template>

    <template v-else-if="mode === 'loading'">
      <p class="feedback-copy">请稍等，系统正在整理你的检测反馈。</p>
      <div class="loading-bar"></div>
    </template>

    <template v-else>
      <div class="feedback-section">
        <h4>overallSummary</h4>
        <p class="feedback-copy">{{ summary }}</p>
      </div>

      <div class="feedback-section">
        <h4>questionResults</h4>
        <div v-if="questionResults.length" class="result-list">
          <article v-for="result in questionResults" :key="result.questionId" class="result-item">
            <div class="result-head">
              <strong>Q{{ result.questionId }}</strong>
              <span>{{ result.correct ? '正确' : '待加强' }}</span>
            </div>
            <p class="result-copy">{{ result.stem }}</p>
            <p class="result-copy">你的答案：{{ result.userAnswer || '未填写' }}</p>
            <p class="result-copy">反馈：{{ result.feedback || '暂无逐题反馈' }}</p>
          </article>
        </div>
        <p v-else class="feedback-copy">暂无逐题结果。</p>
      </div>

      <div class="feedback-section">
        <h4>weaknesses</h4>
        <ul v-if="weakPoints.length" class="feedback-list">
          <li v-for="item in weakPoints" :key="item">{{ item }}</li>
        </ul>
        <p v-else class="feedback-copy">当前没有明显薄弱点。</p>
      </div>

      <div class="feedback-section" v-if="systemAdjustments.length">
        <h4>suggestedNextAction</h4>
        <ul class="feedback-list">
          <li v-for="item in systemAdjustments" :key="item">{{ item }}</li>
        </ul>
      </div>

      <div class="feedback-section" v-if="report?.nextRoundAdvice">
        <h4>nextRoundAdvice</h4>
        <p class="feedback-copy">{{ report.nextRoundAdvice }}</p>
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

.feedback-section h4,
.result-head,
.feedback-head h3 {
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

.result-list {
  display: grid;
  gap: 12px;
}

.result-item {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.4);
  background: rgba(10, 16, 26, 0.78);
}

.result-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.result-copy {
  margin: 0;
  color: var(--color-text-secondary);
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
