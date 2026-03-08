<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const router = useRouter()
const sessionStore = useSessionStore()

const formData = ref({
  user_id: 'mock_user_001',
  course_id: 'computer_network',
  chapter_id: 'tcp',
  goal_text: '',
})

const isCreating = ref(false)

async function handleSubmit() {
  if (!formData.value.goal_text.trim()) {
    return
  }

  isCreating.value = true
  try {
    const sessionId = await sessionStore.createSession(formData.value)
    await sessionStore.planSession(sessionId)
    await sessionStore.fetchSessionOverview(sessionId)
    router.push(`/session/${sessionId}`)
  } catch (e) {
    console.error('Failed to create session:', e)
  } finally {
    isCreating.value = false
  }
}
</script>

<template>
  <div class="home">
    <div class="hero">
      <h1 class="title">AI Learning Navigator</h1>
      <p class="subtitle">自适应学习流程导航系统</p>
    </div>

    <form class="create-form" @submit.prevent="handleSubmit">
      <div class="form-group">
        <label for="goal">学习目标</label>
        <textarea
          id="goal"
          v-model="formData.goal_text"
          placeholder="例如：理解 TCP 可靠传输机制并能做题"
          rows="3"
          required
        ></textarea>
      </div>

      <div class="form-row">
        <div class="form-group">
          <label for="course">课程</label>
          <select id="course" v-model="formData.course_id">
            <option value="computer_network">计算机网络</option>
          </select>
        </div>

        <div class="form-group">
          <label for="chapter">章节</label>
          <select id="chapter" v-model="formData.chapter_id">
            <option value="tcp">TCP 协议</option>
          </select>
        </div>
      </div>

      <button
        type="submit"
        class="submit-btn"
        :disabled="isCreating || !formData.goal_text.trim()"
      >
        {{ isCreating ? '创建中...' : '开始学习' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.hero {
  text-align: center;
  margin-bottom: 2rem;
}

.title {
  font-size: 2.5rem;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
  letter-spacing: -0.02em;
}

.subtitle {
  font-size: 1rem;
  color: var(--color-text-secondary);
  margin-top: 0.5rem;
}

.create-form {
  width: 100%;
  max-width: 480px;
  background: var(--color-bg-elevated);
  border-radius: 16px;
  padding: 1.5rem;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.08);
}

.form-group {
  margin-bottom: 1.25rem;
}

.form-group label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 0.75rem 1rem;
  font-size: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.submit-btn {
  width: 100%;
  padding: 1rem;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: transform 0.2s, background-color 0.2s;
}

.submit-btn:hover:not(:disabled) {
  background: var(--color-primary-hover);
  transform: translateY(-1px);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 768px) {
  .home {
    padding: 2rem;
  }

  .hero {
    margin-bottom: 3rem;
  }

  .title {
    font-size: 3rem;
  }

  .subtitle {
    font-size: 1.25rem;
  }

  .create-form {
    padding: 2rem;
  }

  .form-group {
    margin-bottom: 1.5rem;
  }
}
</style>
