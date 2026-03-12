import apiClient from '@/api/client';
import type {
  CapabilityProfile,
  DiagnosisAnswer,
  DiagnosisGenerateResponse,
  DiagnosisQuestion,
  DiagnosisSubmitResponse,
} from '@/types/diagnosis';

const diagnosisApiEnabled = import.meta.env.VITE_ENABLE_DIAGNOSIS_API === 'true';

function buildMockQuestions(): DiagnosisQuestion[] {
  return [
    {
      questionId: 'foundation-level',
      dimension: 'knowledge_foundation',
      type: 'single_choice',
      title: '你觉得自己目前对这个目标相关内容的掌握程度如何？',
      description: '按你现在的真实感觉来选就可以，这不是考试。',
      options: ['刚开始接触', '学过但不太稳', '基础比较稳', '已经能独立应用'],
      required: true,
    },
    {
      questionId: 'common-difficulty',
      dimension: 'difficulty_focus',
      type: 'multiple_choice',
      title: '你现在最容易卡住的地方有哪些？',
      description: '可多选，系统会据此调整后续讲解重点。',
      options: ['概念记不牢', '公式或步骤容易混', '做题时不会下手', '会做基础题但综合题吃力'],
      required: true,
    },
    {
      questionId: 'learning-habit',
      dimension: 'learning_preference',
      type: 'text',
      title: '说说你平时更适合什么样的学习方式。',
      description: '比如更喜欢例题带着学、先看总结、边做边学，或者你每天大概能投入多久。',
      required: false,
      placeholder: '例如：我更喜欢先看一页总结，再做两三道例题，每天晚上能学 40 分钟左右。',
    },
  ];
}

function extractTextAnswer(answers: DiagnosisAnswer[], questionId: string) {
  const match = answers.find((item) => item.questionId === questionId);
  return typeof match?.value === 'string' ? match.value.trim() : '';
}

function extractListAnswer(answers: DiagnosisAnswer[], questionId: string) {
  const match = answers.find((item) => item.questionId === questionId);
  return Array.isArray(match?.value) ? match.value : [];
}

function buildMockProfile(answers: DiagnosisAnswer[]): CapabilityProfile {
  const levelAnswer = extractTextAnswer(answers, 'foundation-level');
  const difficulties = extractListAnswer(answers, 'common-difficulty');
  const habit = extractTextAnswer(answers, 'learning-habit');

  const currentLevel =
    levelAnswer === '已经能独立应用'
      ? '中上水平'
      : levelAnswer === '基础比较稳'
        ? '基础较稳'
        : levelAnswer === '学过但不太稳'
          ? '基础待巩固'
          : '起步阶段';

  const strengths =
    levelAnswer === '基础比较稳' || levelAnswer === '已经能独立应用'
      ? ['已有一定基础', '适合较快进入重点训练']
      : ['目标明确', '愿意主动补齐薄弱点'];

  const weaknesses =
    difficulties.length > 0
      ? difficulties
      : ['目前信息还不够完整，建议先完成一轮基础诊断练习'];

  const learningPreference = habit || '更适合结构清晰、一步一步推进的学习方式';
  const timeBudget = /([0-9]{1,3}\s*分钟|[0-9]{1,2}\s*小时)/.exec(habit)?.[0] || '建议每天安排 30 到 45 分钟';
  const goalOrientation = '先补基础，再逐步进入训练和巩固';

  return {
    currentLevel,
    strengths,
    weaknesses,
    learningPreference,
    timeBudget,
    goalOrientation,
    summary: `从这轮诊断来看，你目前处于“${currentLevel}”阶段。系统会优先围绕你提到的卡点安排内容，并尽量贴合你的学习节奏。`,
  };
}

export async function generateDiagnosisApi(sessionId: string): Promise<DiagnosisGenerateResponse> {
  if (!diagnosisApiEnabled) {
    return {
      diagnosisId: `mock-diagnosis-${sessionId}`,
      sessionId,
      questions: buildMockQuestions(),
    };
  }

  try {
    const { data } = await apiClient.post('/api/diagnosis/generate', { sessionId });
    return data as DiagnosisGenerateResponse;
  } catch {
    return {
      diagnosisId: `mock-diagnosis-${sessionId}`,
      sessionId,
      questions: buildMockQuestions(),
    };
  }
}

export async function submitDiagnosisApi(diagnosisId: string, answers: DiagnosisAnswer[]): Promise<DiagnosisSubmitResponse> {
  if (!diagnosisApiEnabled) {
    return {
      capabilityProfile: buildMockProfile(answers),
      nextAction: {
        type: 'PATH_PLAN',
        label: '进入个性化学习路径',
      },
    };
  }

  try {
    const { data } = await apiClient.post('/api/diagnosis/submit', {
      diagnosisId,
      answers,
    });
    return data as DiagnosisSubmitResponse;
  } catch {
    return {
      capabilityProfile: buildMockProfile(answers),
      nextAction: {
        type: 'PATH_PLAN',
        label: '进入个性化学习路径',
      },
    };
  }
}
