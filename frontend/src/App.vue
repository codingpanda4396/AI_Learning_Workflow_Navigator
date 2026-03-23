<template>
  <router-view />
  <!-- 执行页导师入口挂在 App 根级，避免 router-view 子树层叠/裁剪导致悬浮钮不可见 -->
  <template v-if="isExecutionRoute">
    <AiTutorFloating />
    <AiTutorPanel />
  </template>
  <Teleport to="body">
    <Transition name="toast">
      <div
        v-if="toastMessage"
        class="fixed bottom-6 left-1/2 z-50 -translate-x-1/2 rounded-input bg-text-primary px-5 py-3 text-sm text-white shadow-lg"
      >
        {{ toastMessage }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { toastMessage } from '@/stores/toast'
import AiTutorFloating from '@/components/ai-tutor/AiTutorFloating.vue'
import AiTutorPanel from '@/components/ai-tutor/AiTutorPanel.vue'

const route = useRoute()
const isExecutionRoute = computed(() => route.name === 'execution')
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}
</style>
