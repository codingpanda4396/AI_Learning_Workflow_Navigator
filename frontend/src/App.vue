<template>
  <router-view />
  <!-- 执行工作台：仅抽屉导师；DFS/BFS STRUCTURE 骨架台另开右下角浮球 -->
  <template v-if="showAiTutorChrome">
    <AiTutorFloating />
    <AiTutorPanel />
  </template>
  <template v-else-if="showAiTutorPanelOnly">
    <AiTutorFloating v-if="aiTutor.showTaskRunFloatingFab" />
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
import { useAuthStore } from '@/stores/auth'
import { useAiTutorStore } from '@/stores/aiTutor'

const route = useRoute()
const aiTutor = useAiTutorStore()
const showAiTutorChrome = computed(() => {
  if (route.name === 'task' || route.name === 'taskRun') return false
  return route.name === 'execution'
})

/** 任务跑页需挂载面板，顶栏「求助」才能打开抽屉（不挂全局浮球） */
const showAiTutorPanelOnly = computed(
  () => route.name === 'task' || route.name === 'taskRun'
)
useAuthStore().ensureReady()
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
