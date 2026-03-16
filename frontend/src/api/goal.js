import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export const goalApi = {
  createGoal(data) {
    return api.post('/goals', data)
  }
}

export const diagnosisApi = {
  createSession(goalId) {
    return api.post('/diagnosis/sessions', { goalId })
  },

  submitAnswers(diagnosisId, answers) {
    return api.post('/diagnosis/submissions', { diagnosisId, answers })
  }
}

export const learningPlanApi = {
  preview(goalId, diagnosisId) {
    return api.post('/learning-plans/preview', { goalId, diagnosisId })
  },

  commit(planId) {
    return api.post('/learning-plans/commit', { planId })
  }
}

export const sessionApi = {
  getCurrentTask(sessionId) {
    return api.get(`/sessions/${sessionId}/current-task`)
  },

  getReport(sessionId) {
    return api.get(`/sessions/${sessionId}/report`)
  },

  requestNextAction(sessionId, actionType) {
    return api.post(`/sessions/${sessionId}/next-action`, { actionType })
  }
}

export const taskApi = {
  sendInteraction(taskId, data) {
    return api.post(`/tasks/${taskId}/interactions`, data)
  },

  completeTask(taskId, data) {
    return api.post(`/tasks/${taskId}/complete`, data)
  }
}

export default api
