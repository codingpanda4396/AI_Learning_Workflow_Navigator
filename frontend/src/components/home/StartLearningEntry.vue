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
        <p class="app-eyebrow">{{ session ? 'CURRENT PATH' : 'START NEW PATH' }}</p>
        <h2 class="entry-card__title">
          {{ session ? '继续当前主线' : '开始新的学习主线' }}
        </h2>
        <p class="entry-card__desc">
          {{
            session
              ? '直接回到上一次停下来的地方。'
              : '告诉系统你想学什么，后面的诊断和规划会自动完成。'
          }}
        </p>
      </div>

      <div v-if="session" class="entry-card__actions entry-card__actions--top">
        <AppButton size="lg" :loading="loading" class="entry-card__primary" @click="$emit('continue')">
          继续当前主线
        </AppButton>
        <AppButton size="lg" variant="secondary" disabled>
          新建学习主线
        </AppButton>
      </div>
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

    <p v-if="session" class="entry-card__note">
      同一时间只保留一条进行中的学习主线，先把当前路径走完会更顺。
    </p>
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
  gap: 18px;
}

.entry-card__title {
  margin-top: 12px;
  font-size: clamp(30px, 4vw, 40px);
  line-height: 1.06;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.entry-card__desc {
  margin-top: 12px;
  max-width: 520px;
  font-size: 15px;
  line-height: 1.75;
  color: #64748b;
}

.entry-form {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.entry-form--muted {
  opacity: 0.82;
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
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 6px;
}

.entry-card__actions--top {
  margin-top: 0;
}

.entry-card__primary {
  min-width: 188px;
}

.entry-card__note {
  margin-top: 18px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
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
    grid-template-columns: minmax(0, 1.6fr) minmax(0, 1fr) minmax(0, 1fr) auto;
    align-items: end;
  }

  .entry-card__actions {
    justify-content: flex-end;
  }
}
</style>
