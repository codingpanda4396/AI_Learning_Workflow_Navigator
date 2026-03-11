export const APP_TITLE = 'AI 个性化学习导航系统';

export const DEFAULT_SESSION_FORM = {
  courseId: 'computer_network',
  chapterId: 'tcp',
  goalText: '理解 TCP 可靠传输机制，并完成一次训练闭环',
};

export const STORAGE_KEYS = {
  token: 'panda_nav_token',
  username: 'panda_nav_username',
  lastSessionId: 'panda_nav_last_session_id',
};

export const STAGE_LABELS: Record<string, string> = {
  STRUCTURE: '结构搭建',
  UNDERSTANDING: '原理理解',
  TRAINING: '训练检测',
  REFLECTION: '反思巩固',
  EVALUATE: '结果评估',
};
