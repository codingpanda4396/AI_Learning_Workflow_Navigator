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
    <div class="session-card__copy">
      <p class="app-eyebrow">Current Path</p>
      <h2 class="session-card__title">当前主线</h2>
      <p class="session-card__desc">不用重新找入口，直接回到正在进行的那一步。</p>
    </div>

    <div class="session-card__meta">
      <div class="session-meta">
        <span class="session-meta__label">目标</span>
        <p class="session-meta__value">{{ session.goal || '未填写' }}</p>
      </div>
      <div class="session-meta">
        <span class="session-meta__label">课程 / 章节</span>
        <p class="session-meta__value">{{ session.course || '未填写' }} / {{ session.chapter || '未填写' }}</p>
      </div>
      <div class="session-meta">
        <span class="session-meta__label">当前阶段</span>
        <p class="session-meta__value">{{ session.phase || '待开始' }}</p>
      </div>
    </div>

    <AppButton size="lg" class="session-card__cta" @click="continueLearning">继续下一步</AppButton>
  </section>
</template>

<style scoped>
.session-card {
  display: grid;
  gap: 18px;
  border: 1px solid rgba(15, 23, 42, 0.07);
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.045);
  padding: 28px;
}

.session-card__title {
  margin-top: 10px;
  font-size: clamp(24px, 3vw, 32px);
  line-height: 1.08;
  font-weight: 650;
  letter-spacing: -0.04em;
  color: #0f172a;
}

.session-card__desc {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.session-card__meta {
  display: grid;
  gap: 12px;
}

.session-meta {
  border: 1px solid rgba(15, 23, 42, 0.06);
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.86);
  padding: 16px 18px;
}

.session-meta__label {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #64748b;
}

.session-meta__value {
  margin-top: 8px;
  font-size: 15px;
  line-height: 1.7;
  font-weight: 600;
  color: #0f172a;
}

.session-card__cta {
  width: fit-content;
}

@media (min-width: 1024px) {
  .session-card {
    grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.2fr) auto;
    align-items: end;
    padding: 32px;
  }

  .session-card__meta {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .session-card__cta {
    justify-self: end;
  }
}
</style>
