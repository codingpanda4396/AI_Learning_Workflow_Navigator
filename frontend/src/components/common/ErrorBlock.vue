<template>
  <div class="p-4 bg-red-50 border border-red-200 rounded-xl">
    <div class="flex items-start gap-3">
      <svg class="w-5 h-5 text-red-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
      </svg>
      <div class="flex-1">
        <p class="text-sm text-red-600">{{ message }}</p>
        <button
          v-if="showRetry"
          @click="$emit('retry')"
          class="mt-2 text-sm text-red-700 font-medium hover:text-red-800 cursor-pointer"
        >
          点击重试
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: '发生错误，请重试'
  },
  showRetry: {
    type: Boolean,
    default: false
  }
})

defineEmits(['retry'])
</script>
