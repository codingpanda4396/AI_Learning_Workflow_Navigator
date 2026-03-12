import { DEFAULT_PLAN_ADJUSTMENTS, INTENSITY_LABELS, LEARNING_MODE_LABELS, STAGE_ORDER, STAGE_LABELS } from '@/constants/learningPlan';
import type { LearningIntensity, LearningPlanPreview, LearningPlanRequest, PlanTaskPreview } from '@/types/learningPlan';

const intensityTotalMinutes: Record<LearningIntensity, number> = {
  LIGHT: 55,
  STANDARD: 85,
  INTENSIVE: 120,
};

function buildStageMinutes(totalMinutes: number) {
  return [
    Math.round(totalMinutes * 0.2),
    Math.round(totalMinutes * 0.3),
    Math.round(totalMinutes * 0.32),
    Math.max(10, totalMinutes - Math.round(totalMinutes * 0.2) - Math.round(totalMinutes * 0.3) - Math.round(totalMinutes * 0.32)),
  ];
}

function buildTaskPreviews(goalText: string, startPoint: string, totalMinutes: number): PlanTaskPreview[] {
  const minutes = buildStageMinutes(totalMinutes);
  return [
    {
      stage: 'STRUCTURE',
      stageGoal: `先把 ${goalText} 涉及的关键模块和依赖关系搭起来。`,
      learnerAction: `用 1 张简图确认 ${startPoint} 和后续知识点之间的衔接。`,
      aiSupport: 'AI 会提炼结构图、标出前置依赖，并提示你本轮不必先展开的支线。',
      estimatedMinutes: minutes[0],
    },
    {
      stage: 'UNDERSTANDING',
      stageGoal: `把 ${goalText} 的核心原理讲清楚，避免只记结论。`,
      learnerAction: '阅读解释、对照例子，并用自己的话复述关键机制。',
      aiSupport: 'AI 会根据你的诊断结果补讲最容易卡住的概念，并用更贴近当前基础的表述解释。',
      estimatedMinutes: minutes[1],
    },
    {
      stage: 'TRAINING',
      stageGoal: `把理解转成可用能力，检验你是否真的能用 ${goalText} 解决问题。`,
      learnerAction: '完成针对性练习，重点验证本轮主攻方向是否已经打通。',
      aiSupport: 'AI 会动态给出练习、即时点评你的作答，并针对薄弱点继续追问。',
      estimatedMinutes: minutes[2],
    },
    {
      stage: 'REFLECTION',
      stageGoal: '收束这一轮学习结果，明确保留项和下一步补强项。',
      learnerAction: '查看反馈摘要，确认哪些概念已经稳定，哪些还需要回补。',
      aiSupport: 'AI 会整理错因、提炼下一步建议，并把结论带回后续学习链路。',
      estimatedMinutes: minutes[3],
    },
  ];
}

export function createMockLearningPlanPreview(input: Partial<LearningPlanRequest>): LearningPlanPreview {
  const adjustments = {
    ...DEFAULT_PLAN_ADJUSTMENTS,
    ...(input.adjustments ?? {}),
  };
  const goalText = input.goalText?.trim() || '掌握当前学习目标';
  const courseId = input.courseId?.trim() || '当前课程';
  const chapterId = input.chapterId?.trim() || '当前章节';
  const foundationFirst = adjustments.prioritizeFoundation;
  const startPoint = foundationFirst ? `${chapterId} 的关键前置概念` : `${goalText} 的核心主线`;
  const bridgePoint = foundationFirst ? `${goalText} 的核心机制` : `${goalText} 的易错边界`;
  const totalMinutes = intensityTotalMinutes[adjustments.intensity];

  return {
    summary: {
      recommendedStart: startPoint,
      recommendedRhythm: adjustments.intensity,
      estimatedMinutes: totalMinutes,
      estimatedKnowledgeCount: 4,
      stageCount: STAGE_ORDER.length,
      personalizedHeadline: foundationFirst
        ? `结合你当前的基础和这次目标，系统建议你先补齐 ${chapterId} 的前置理解，再进入 ${goalText}。`
        : `你现在不是从零开始，这一轮更适合直接围绕 ${goalText} 的主线推进，再回补边界知识。`,
      personalizedSummary: `本轮会以“${startPoint} -> ${bridgePoint} -> 训练验证”的顺序展开，节奏采用${INTENSITY_LABELS[adjustments.intensity]}模式，学习方式偏向${LEARNING_MODE_LABELS[adjustments.learningMode]}。`,
    },
    reasons: [
      {
        key: 'starting-point',
        title: '起点依据',
        label: '为什么从这里开始',
        description: foundationFirst
          ? `${chapterId} 相关基础如果不先补齐，后面学习 ${goalText} 时容易只会套结论，理解链条会断。`
          : `你已经具备部分基础，直接切入 ${goalText} 的主线更能提升效率，再回补边界会更顺手。`,
      },
      {
        key: 'risk',
        title: '风险提示',
        label: '系统重点防的坑',
        description: `当前最大的风险不是“完全不会”，而是对 ${goalText} 的关键条件和适用边界掌握不稳，训练时容易在迁移应用上失分。`,
      },
      {
        key: 'priority',
        title: '本轮优先级',
        label: '为什么这样排先后',
        description: '系统把“先建立可解释的理解，再做针对训练”排在更前面，是为了避免你在练习里重复试错，却说不清自己为什么错。',
      },
      {
        key: 'strategy',
        title: '学习策略',
        label: '这轮准备怎么学',
        description: `这一轮不会把内容摊得很散，而是围绕 ${goalText} 收紧路径：先明确结构，再讲透原理，最后用训练和反思完成闭环。`,
      },
    ],
    pathNodes: [
      {
        id: 'foundation',
        name: startPoint,
        masteryStatus: foundationFirst ? 'WEAK' : 'PARTIAL',
        difficulty: 'FOUNDATION',
        reasonTags: foundationFirst ? ['推荐起点', '补前置基础'] : ['衔接主线'],
        estimatedMinutes: Math.round(totalMinutes * 0.22),
        isStartingPoint: foundationFirst,
        isPrerequisite: true,
        isFocus: foundationFirst,
      },
      {
        id: 'core',
        name: bridgePoint,
        masteryStatus: 'PARTIAL',
        difficulty: 'CORE',
        reasonTags: foundationFirst ? ['主攻方向'] : ['推荐起点', '主攻方向'],
        estimatedMinutes: Math.round(totalMinutes * 0.3),
        isStartingPoint: !foundationFirst,
        isPrerequisite: false,
        isFocus: true,
      },
      {
        id: 'application',
        name: `${goalText} 的典型应用场景`,
        masteryStatus: 'NEW',
        difficulty: 'CORE',
        reasonTags: ['训练验证', '迁移应用'],
        estimatedMinutes: Math.round(totalMinutes * 0.28),
        isStartingPoint: false,
        isPrerequisite: false,
        isFocus: true,
      },
      {
        id: 'reflection',
        name: `${goalText} 的常见误区与复盘点`,
        masteryStatus: 'NEW',
        difficulty: 'CHALLENGE',
        reasonTags: ['评估反思', '巩固闭环'],
        estimatedMinutes: Math.max(12, totalMinutes - Math.round(totalMinutes * 0.22) - Math.round(totalMinutes * 0.3) - Math.round(totalMinutes * 0.28)),
        isStartingPoint: false,
        isPrerequisite: false,
        isFocus: false,
      },
    ],
    taskPreviews: buildTaskPreviews(goalText, startPoint, totalMinutes),
    adjustments,
    goalText,
    courseId,
    chapterId,
    diagnosisSummary: `系统结合你的目标、当前章节语境和已有学习画像，判断这轮更需要先打通 ${startPoint}，再把 ${goalText} 用起来。`,
    nextStepNote: `确认后会创建本轮学习 session，并从 ${STAGE_LABELS.STRUCTURE} 阶段进入后续学习执行链路。`,
  };
}
