export const APP_COPY = {
  brand: 'Lumina AI',
  tutorLabel: '导师',
  loginCta: '登录后继续',
  logoutCta: '退出',
  appBarSignedInHint: '继续这轮学习',
} as const

export const AUTH_COPY = {
  eyebrow: 'AI Learning Workflow',
  heroTitle: '登录后，继续这轮学习。',
  heroSubtitle: '保留目标、进度和结果；回来时直接接着走。',
  helper: '输入账号后继续。',
  submitLogin: '登录并继续',
  submitRegister: '注册并继续',
  highlights: [
    { kicker: 'Goal', title: '继续目标', body: '回到上次选中的知识点。' },
    { kicker: 'Plan', title: '继续进度', body: '直接回到当前这一步。' },
    { kicker: 'Report', title: '继续结果', body: '这轮结果也会保留。' },
  ],
} as const

export const GOAL_COPY = {
  title: '你这轮想学什么？',
  subtitle: '选一个知识点，直接开始。',
  continueTitle: '继续上次学习',
  continueEmpty: '还没有上次进度',
  continueEmptyHint: '开始后，这里会保留最近一轮。',
  continueLoginTitle: '登录后继续上次学习',
  continueLoginHint: '登录后可直接回到这轮进度。',
  footerTitle: '当前选择',
  footerHint: '开始后直接进入下一步。',
  loginToast: '登录后继续',
} as const

export const DIAGNOSIS_COPY = {
  title: '回答 3 个问题',
  subtitle: '选最贴近的一项即可。',
  topicLabel: '本轮主题',
  intro: '答完就进入下一步。',
  submit: '继续下一步',
} as const

export const PLAN_COPY = {
  loading: '正在整理这轮路径...',
  reload: '重新加载',
  whyFirst: '为什么从这里开始',
  currentAdvice: '当前建议',
} as const

export const EXECUTION_COPY = {
  redirectTitle: '正在进入执行页',
  redirectSubtitle: '马上带你回到当前任务。',
  noTaskToast: '当前没有任务，先回到规划页。',
  noTaskEmpty: '当前没有任务',
  viewReport: '查看结果',
  loadingTask: '正在打开当前任务...',
  feedbackTitle: '当前反馈',
  transcriptUser: '你的回答',
  transcriptAssistant: '反馈',
  railTitle: '按顺序推进 4 个知识点',
  railOverview: '每次只推进当前这一个点。',
  mainActionWhy: '现在先做',
  actionChips: '这样开头',
  passHint: '写到这里就能继续',
  supportToggle: '需要时再看',
  supportTranscript: '本步记录',
  currentFocus: '当前重点',
  done: '已经做到',
  gap: '还要补',
  nextStep: '现在继续',
  taskHeaderKicker: '这一屏先做',
} as const

export const REPORT_COPY = {
  title: '本轮结果',
  subtitle: '看结果，然后决定下一步。',
  loading: '正在整理结果...',
  result: '本轮结果',
  summary: '这一轮总结',
  nextAction: '下一步',
  confirmNext: '选择下一步',
  confirmAction: '继续',
  restart: '重新开始',
  replanHint: '建议先重新规划。',
} as const
