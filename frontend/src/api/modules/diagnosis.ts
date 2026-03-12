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
      dimension: 'FOUNDATION',
      type: 'single_choice',
      required: true,
      options: ['刚开始接触', '学过但不太稳', '基础比较稳', '已经能独立应用'],
      copy: {
        sectionLabel: 'KNOWLEDGE_FOUNDATION',
        title: '你觉得自己目前对这个目标相关内容的掌握程度如何？',
        description: '按你现在的真实感觉来选就可以，这不是考试。',
        submitHint: '你的回答会帮助系统判断起点和后续安排。',
      },
    },
    {
      questionId: 'common-difficulty',
      dimension: 'DIFFICULTY_FOCUS',
      type: 'multiple_choice',
      required: true,
      options: ['概念记不牢', '公式或步骤容易混', '做题时不会下手', '会做基础题但综合题吃力'],
      copy: {
        title: '你现在最容易卡住的地方有哪些？',
        description: '可以多选，系统会据此调整后续讲解重点。',
        submitHint: '这些信息会影响系统后续的训练侧重点。',
      },
    },
    {
      questionId: 'learning-habit',
      dimension: 'LEARNING_PREFERENCE',
      type: 'text',
      required: false,
      copy: {
        title: '说说你平时更适合什么样的学习方式。',
        description: '比如喜欢先看总结、再做题，还是边学边练。',
        placeholder: '你可以简单描述自己的学习习惯、可投入时长，或你觉得最有效的学习节奏。',
        submitHint: '这会帮助系统安排更贴合你的学习路径。',
      },
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
      : ['目标明确', '愿意主动补薄弱点'];

  const weaknesses =
    difficulties.length > 0 ? difficulties : ['目前信息还不够完整，建议先完成一轮基础诊断练习'];

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
    summary: `从这轮诊断来看，你目前更适合从“${currentLevel}”出发，系统会优先围绕你提到的卡点安排内容。`,
    planExplanation: '系统接下来会优先帮你理顺基础概念，并结合典型训练逐步推进，尽量贴合你的学习节奏。',
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
