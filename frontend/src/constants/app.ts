export const APP_TITLE = 'AI 学习闭环导航';

export const DEFAULT_SESSION_FORM = {
  courseId: 'computer_network',
  chapterId: 'tcp',
  goalText: '理解 TCP 可靠传输机制，并完成一轮检测与反馈。',
};

export const STORAGE_KEYS = {
  token: 'panda_nav_token',
  username: 'panda_nav_username',
  lastSessionId: 'panda_nav_last_session_id',
};

export const STAGE_LABELS: Record<string, string> = {
  STRUCTURE: '搭建结构',
  UNDERSTANDING: '理解原理',
  TRAINING: '进入检测',
  REFLECTION: '复盘巩固',
  EVALUATE: '查看反馈',
};
