<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';
import type { ActiveSession, StartLearningForm } from '@/types/home';

const model = defineModel<StartLearningForm>({ required: true });

defineProps<{
  session: ActiveSession | null;
  loading?: boolean;
}>();

defineEmits<{
  submit: [];
  continue: [];
}>();
</script>

<template>
  <section class="entry-card">
    <div class="entry-card__header">
      <div>
        <p class="app-eyebrow">First Step</p>
        <h2 class="entry-card__title">{{ session ? '先回到当前主线' : '从这里开始第一步' }}</h2>
        <p class="entry-card__desc">
          {{ session ? '当前已经有进行中的学习主线。' : '先输入目标，系统会从诊断开始往后推进。' }}
        </p>
      </div>

      <AppButton
        v-if="session"
        size="lg"
        :loading="loading"
        class="entry-card__primary"
        @click="$emit('continue')"
      >
        继续当前主线
      </AppButton>
    </div>

    <div class="entry-form" :class="{ 'entry-form--muted': Boolean(session) }">
      <label class="entry-field entry-field--goal">
        <span class="entry-field__label">学习目标</span>
        <input
          v-model="model.goal"
          type="text"
          class="app-input entry-field__input"
          :disabled="Boolean(session)"
          placeholder="例如：理解最短路径算法为什么这样设计"
        />
      </label>

      <label class="entry-field">
        <span class="entry-field__label">课程</span>
        <input
          v-model="model.course"
          type="text"
          class="app-input entry-field__input"
          :disabled="Boolean(session)"
          placeholder="例如：数据结构"
        />
      </label>

      <label class="entry-field">
        <span class="entry-field__label">章节</span>
        <input
          v-model="model.chapter"
          type="text"
          class="app-input entry-field__input"
          :disabled="Boolean(session)"
          placeholder="例如：图"
        />
      </label>

      <div v-if="!session" class="entry-card__actions">
        <AppButton size="lg" :loading="loading" class="entry-card__primary" @click="$emit('submit')">
          开始这一小步
        </AppButton>
      </div>
    </div>
  </section>
</template>

<style scoped>
.entry-card {
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 28px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.96));
  box-shadow: 0 22px 60px rgba(15, 23, 42, 0.05);
  padding: 28px;
}

.entry-card__header {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-card__title {
  margin-top: 10px;
  font-size: clamp(28px, 3.8vw, 36px);
  line-height: 1.08;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.entry-card__desc {
  margin-top: 10px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.entry-form {
  display: grid;
  gap: 14px;
  margin-top: 24px;
}

.entry-form--muted {
  opacity: 0.72;
}

.entry-field {
  display: grid;
  gap: 10px;
}

.entry-field--goal .entry-field__input {
  min-height: 58px;
  font-size: 16px;
}

.entry-field__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #64748b;
}

.entry-field__input:disabled {
  cursor: not-allowed;
}

.entry-card__actions {
  display: flex;
  margin-top: 6px;
}

.entry-card__primary {
  min-width: 188px;
}

@media (min-width: 1024px) {
  .entry-card {
    padding: 32px;
  }

  .entry-card__header {
    flex-direction: row;
    align-items: end;
    justify-content: space-between;
  }

  .entry-form {
    grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr) minmax(0, 1fr) auto;
    align-items: end;
  }
}
</style>
