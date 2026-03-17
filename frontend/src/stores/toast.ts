import { ref } from 'vue'

export const toastMessage = ref<string | null>(null)

export function showToast(msg: string) {
  toastMessage.value = msg
  setTimeout(() => {
    toastMessage.value = null
  }, 4000)
}
