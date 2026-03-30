<template>
  <!-- 挂到 body；定位用内联 style，避免 Tailwind 任意值含逗号时类名未生成导致按钮跑出视口 -->
  <Teleport to="body">
    <button
      v-if="!store.visible"
      type="button"
      class="ai-tutor-float flex items-center gap-2 rounded-full bg-primary px-4 py-3 text-sm font-medium text-white shadow-lg transition hover:bg-primary-hover focus:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2"
      :style="floatBtnStyle"
      aria-label="打开导师"
      @click="store.toggleVisible()"
    >
      <span class="text-base leading-none" aria-hidden="true">💬</span>
      <span>{{ store.context.floatingLabel || '不懂这一步？' }}</span>
    </button>
  </Teleport>
</template>

<script setup lang="ts">
import { useAiTutorStore } from '@/stores/aiTutor'

const store = useAiTutorStore()

const floatBtnStyle = {
  position: 'fixed',
  right: '24px',
  bottom: 'max(24px, env(safe-area-inset-bottom, 0px))',
  zIndex: 10050,
} as const
</script>

<style scoped>
.ai-tutor-float {
  animation: ai-tutor-breathe 2.8s ease-in-out infinite;
}

/* 仅用阴影呼吸，避免 transform 与 position:fixed 在部分内核下叠出异常包含块 */
@keyframes ai-tutor-breathe {
  0%,
  100% {
    box-shadow:
      0 10px 25px rgba(79, 70, 229, 0.35),
      0 0 0 0 rgba(79, 70, 229, 0.25);
  }
  50% {
    box-shadow:
      0 14px 32px rgba(79, 70, 229, 0.5),
      0 0 0 8px rgba(79, 70, 229, 0.06);
  }
}
</style>
