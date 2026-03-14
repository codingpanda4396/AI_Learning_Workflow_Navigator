<script setup lang="ts">
import { useRouter } from 'vue-router';
import AppButton from '@/components/ui/AppButton.vue';
import type { ActiveSession } from '@/types/home';

const props = defineProps<{
  session: ActiveSession;
}>();

const router = useRouter();

function continueLearning() {
  router.push(`/sessions/${props.session.id}`);
}
</script>

<template>
  <section class="session-card">
    <div class="session-card__header">
      <div>
        <p class="app-eyebrow">IN PROGRESS</p>
        <h2 class="session-card__title">继续你刚才的学习主线</h2>
        <p class="session-card__desc">直接回到上一次停下来的地方。</p>
      </div>
      <AppButton size="lg" class="session-card__cta" @click="continueLearning">继续下一步</AppButton>
    </div>

    <div class="session-card__grid">
      <div class="session-meta">
        <span class="session-meta__label">当前目标</span>
        <p class="session-meta__value">{{ session.goal || '未填写' }}</p>
      </div>
      <div class="session-meta">
        <span class="session-meta__label">课程</span>
        <p class="session-meta__value">{{ session.course || '未填写' }}</p>
      </div>
      <div class="session-meta">
        <span class="session-meta__label">章节</span>
        <p class="session-meta__value">{{ session.chapter || '未填写' }}</p>
      </div>
      <div class="session-meta">
        <span class="session-meta__label">当前阶段</span>
        <p class="session-meta__value">{{ session.phase || '待开始' }}</p>
      </div>
    </div>
  </section>
</template>

<style scoped>
.session-card {
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.84);
  box-shadow: 0 20px 56px rgba(15, 23, 42, 0.045);
  padding: 28px;
}

.session-card__header {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.session-card__title {
  margin-top: 12px;
  font-size: clamp(28px, 3vw, 36px);
  line-height: 1.08;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.session-card__desc {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.75;
  color: #64748b;
}

.session-card__cta {
  min-width: 168px;
}

.session-card__grid {
  display: grid;
  gap: 14px;
  margin-top: 28px;
}

.session-meta {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.85);
  padding: 18px;
}

.session-meta__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #64748b;
}

.session-meta__value {
  margin-top: 10px;
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
  color: #0f172a;
}

@media (min-width: 1024px) {
  .session-card {
    padding: 32px;
  }

  .session-card__header {
    flex-direction: row;
    align-items: end;
    justify-content: space-between;
  }

  .session-card__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
