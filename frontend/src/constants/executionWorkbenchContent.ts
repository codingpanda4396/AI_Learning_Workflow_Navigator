import type { KnowledgeDemoStageCode } from '@/constants/KnowledgeConfig'
import type { KnowledgePackId } from '@/types/knowledgePack'
import type { TopicVisualVariant, WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

/** 单阶段工作台文案（与 KnowledgeConfig.execution 互补：偏展示，不堆进 prompt） */
export type PhaseWorkbenchCopy = {
  whatToOutput: string[]
  recommendedSteps: string[]
  avoid: string[]
  whyNow: string
  skipRisk: string
  expectedGain: string
}

export const WORKBENCH_PHASE_SEQUENCE: WorkbenchPhaseCode[] = [
  'STRUCTURE',
  'UNDERSTANDING',
  'TRAINING',
  'REFLECTION',
]

/** 四阶段通用规则（可被知识点覆盖语气，此处保持短） */
export const STAGE_RULES_BY_PHASE: Record<KnowledgeDemoStageCode, string[]> = {
  STRUCTURE: ['先看位置与边界，不急着记细节', '用一句话说清「它是什么、解决什么问题」', '允许粗糙，但必须站得住'],
  UNDERSTANDING: ['追机制与因果，不背表面定义', '能说出关键约束与常见误解', '用最小例子串起来'],
  TRAINING: ['先自己写，再对照纠错', '暴露缺口比「看起来懂」更重要', '按反馈重构表达，不堆字数'],
  REFLECTION: ['从错误里抽规律，而不是泛泛总结', '写下一条下次可执行策略', '收束成可复用的判断'],
}

export const TOPIC_DISPLAY_NAME: Record<KnowledgePackId, string> = {
  ds_dfs_bfs: 'DFS / BFS',
  net_tcp_handshake: 'TCP',
  os_process_thread: '进程与线程',
  arch_cache_locality: '缓存与一致性',
}

export const TOPIC_VISUAL_VARIANT: Record<KnowledgePackId, TopicVisualVariant> = {
  ds_dfs_bfs: 'graph',
  net_tcp_handshake: 'timeline',
  os_process_thread: 'container',
  arch_cache_locality: 'hierarchy',
}

/** 右侧「观察视角」：按知识点固定，精炼专业 */
export const TOPIC_OBSERVATION_BULLETS: Record<KnowledgePackId, string[]> = {
  ds_dfs_bfs: [
    '搜索顺序：深搜先走到底，广搜一层层扩',
    '栈 vs 队列：DFS 常用栈/递归，BFS 用队列',
    '适用题型：最短路径、层次、连通性',
  ],
  net_tcp_handshake: [
    '连接：三次握手在同步什么、确认什么',
    '可靠传输：序号、确认、重传',
    '流量/拥塞：别和「握手」混为一谈',
  ],
  os_process_thread: [
    '资源：进程是容器，线程共享进程空间',
    '调度与执行：线程多为调度单位',
    '成本：线程切换通常轻于进程切换',
  ],
  arch_cache_locality: [
    '为什么需要缓存：CPU–内存速度差',
    '局部性：时间 / 空间',
    '写策略：命中写回 vs 直写',
    '一致性：多核下为何会出现',
  ],
}

export const EXECUTION_WORKBENCH_BY_PACK: Record<
  KnowledgePackId,
  Partial<Record<KnowledgeDemoStageCode, PhaseWorkbenchCopy>>
> = {
  ds_dfs_bfs: {
    STRUCTURE: {
      whatToOutput: [
        '用一句话说明 DFS 与 BFS 各自「先扩展谁」',
        '各写一条：典型使用结构（栈/队列/递归）',
      ],
      recommendedSteps: ['先画搜索树方向，再谈实现', '用「层次 vs 深度」对照说'],
      avoid: ['不要一上来贴代码', '不要只背「一个走栈一个走队列」不解释场景'],
      whyNow: '没有结构地图，后面题型训练会散。',
      skipRisk: '场景选错策略，复杂度判断常年不稳。',
      expectedGain: '你能快速判断「该用哪种遍历直觉」。',
    },
    UNDERSTANDING: {
      whatToOutput: ['用 3～4 句描述一次 BFS/DFS 在图上的推进差异'],
      recommendedSteps: ['先想「队列弹出顺序」', '再想 DFS 回溯时栈上状态'],
      avoid: ['不要只背定义不举图', '不要混「最短」与「可行」条件'],
      whyNow: '机制不清，刷题会变成背模板。',
      skipRisk: '边界与最短路条件一换就错。',
      expectedGain: '你能解释为什么某题必须用 BFS/DFS。',
    },
    TRAINING: {
      whatToOutput: ['自选一道典型题：说明为何选 BFS 或 DFS，并写出关键判断句'],
      recommendedSteps: ['先写结论，再补一条反例或边界'],
      avoid: ['不要让 AI 代写完整题解', '不要零输出只看解析'],
      whyNow: '表达才能暴露真问题。',
      skipRisk: '以为会了，考场上仍选错策略。',
      expectedGain: '形成可复述的「题型—策略」映射。',
    },
    REFLECTION: {
      whatToOutput: ['写一条你最容易选错策略的场景 + 下次的检查动作'],
      recommendedSteps: ['先写错因，再写一条可执行策略'],
      avoid: ['不要写成空话总结', '不要只抄错题不提炼'],
      whyNow: '把错误模式固化成策略，才算过关。',
      skipRisk: '同类题反复错在同一判断点。',
      expectedGain: '带走一条你自己的「做题检查清单」。',
    },
  },

  net_tcp_handshake: {
    STRUCTURE: {
      whatToOutput: ['一句话：TCP 连接要解决什么问题', '三次握手各自在确认什么（口语化即可）'],
      recommendedSteps: ['先分清「同步」与「数据传输」', '再对齐客户端/服务器视角'],
      avoid: ['不要从报文细节开场', '不要把握手与拥塞控制搅在一起'],
      whyNow: '没有目标感，流程会变成顺口溜。',
      skipRisk: '异常场景（丢包、半连接）完全不会推。',
      expectedGain: '你能说清「每一步在确认什么」。',
    },
    UNDERSTANDING: {
      whatToOutput: ['用时间顺序写出三次交换，并标注每步消除的不确定性'],
      recommendedSteps: ['先回答「为什么不能两次」', '再回答「第三次 ACK 的意义」'],
      avoid: ['不要只列 SYN/ACK 符号', '不要忽略双向可达性'],
      whyNow: '机制是后面可靠传输与流量控制的基础。',
      skipRisk: '重传与序号一混就崩。',
      expectedGain: '你能用因果链解释握手，而不是背步骤。',
    },
    TRAINING: {
      whatToOutput: ['选一个丢包点：推演会发生什么、靠谁超时/重传'],
      recommendedSteps: ['先写现象，再写协议里谁在等'],
      avoid: ['不要长篇抄教材', '不要跳过「等待状态」'],
      whyNow: '异常推演才是区分「记住」与「理解」。',
      skipRisk: '面试一问异常就答空。',
      expectedGain: '你能用最小场景讲清等待与重传逻辑。',
    },
    REFLECTION: {
      whatToOutput: ['总结：握手与「可靠传输」各解决哪一类问题'],
      recommendedSteps: ['各写一条关键词，不要段落'],
      avoid: ['不要写成百科条目', '不要混入 UDP 对比除非题目需要'],
      whyNow: '收束概念边界，避免越学越糊。',
      skipRisk: '概念串台，论述题写一堆无关点。',
      expectedGain: '你能用极短话术讲清分工。',
    },
  },

  os_process_thread: {
    STRUCTURE: {
      whatToOutput: ['各用一句话定义进程与线程', '各写一条：它们各自解决什么问题'],
      recommendedSteps: ['先资源视角，再执行视角'],
      avoid: ['不要只背「轻量」不解释为什么轻'],
      whyNow: '角色不清，调度与通信永远对不上。',
      skipRisk: '把线程当成「小进程」乱用术语。',
      expectedGain: '你能说清资源边界与共享范围。',
    },
    UNDERSTANDING: {
      whatToOutput: ['描述一次线程切换：CPU 大致保存/恢复了什么'],
      recommendedSteps: ['对比进程切换更重在哪里'],
      avoid: ['不要只写「保存上下文」不拆解'],
      whyNow: '这是调度与性能问题的核心抓手。',
      skipRisk: '并发题只会背概念，不会分析成本。',
      expectedGain: '你能把切换成本讲成可理解的因果。',
    },
    TRAINING: {
      whatToOutput: ['给一个场景：说明为何选多线程或多进程'],
      recommendedSteps: ['先写共享需求，再写隔离需求'],
      avoid: ['不要给万能答案', '不要忽略同步开销'],
      whyNow: '场景判断是 OS 题的高频区分点。',
      skipRisk: '只会写定义，不会选型。',
      expectedGain: '你能用两条理由支撑选型。',
    },
    REFLECTION: {
      whatToOutput: ['写清：进程 vs 线程的本质区别 + 你最容易混淆的一点'],
      recommendedSteps: ['区别写一条「资源」，一条「调度」'],
      avoid: ['不要堆教材目录'],
      whyNow: '把混淆点钉死，才算真掌握。',
      skipRisk: '同一混淆反复出现。',
      expectedGain: '带走你自己的「对照句」。',
    },
  },

  arch_cache_locality: {
    STRUCTURE: {
      whatToOutput: ['一句话：缓存解决什么矛盾', '指出时间局部性与空间局部性各一条例子'],
      recommendedSteps: ['先直觉（快慢），再落到访问模式'],
      avoid: ['不要从缓存行位数开场'],
      whyNow: '没有局部性直觉，后面策略全硬背。',
      skipRisk: '性能题只会猜「缓存命中」。',
      expectedGain: '你能用访问模式解释快慢。',
    },
    UNDERSTANDING: {
      whatToOutput: ['解释：顺序访问为何通常更友好于跳跃访问'],
      recommendedSteps: ['把「预取」与空间局部性连起来'],
      avoid: ['不要只写「局部性」不举访问序列'],
      whyNow: '这是写策略与缺失分析的基础。',
      skipRisk: '写回/直写与一致性混淆。',
      expectedGain: '你能用因果解释一次缺失与写命中。',
    },
    TRAINING: {
      whatToOutput: ['给一段访问模式：判断更偏时间还是空间局部性，并说明理由'],
      recommendedSteps: ['先列访问序列，再下判断'],
      avoid: ['不要直接搜标准答案', '不要零输出'],
      whyNow: '训练把模式识别变成可讲清的判断。',
      skipRisk: '考场看到代码片段仍然懵。',
      expectedGain: '形成「看访问—下结论」的短路径。',
    },
    REFLECTION: {
      whatToOutput: ['写一条：多核下一致性风险从哪来 + 你的应对策略'],
      recommendedSteps: ['先写现象，再写一条关键词'],
      avoid: ['不要泛泛谈「同步」'],
      whyNow: '408 常考「为什么」而不是背名词。',
      skipRisk: '论述题堆术语不得分。',
      expectedGain: '你能用一句话讲清根因。',
    },
  },
}

export function getPhaseWorkbenchCopy(
  packId: KnowledgePackId | null | undefined,
  phase: KnowledgeDemoStageCode
): PhaseWorkbenchCopy | null {
  if (!packId) return null
  const row = EXECUTION_WORKBENCH_BY_PACK[packId]
  return row?.[phase] ?? null
}

export function mergePhaseCopyWithFallback(
  packId: KnowledgePackId | null | undefined,
  phase: KnowledgeDemoStageCode,
  fallback: PhaseWorkbenchCopy
): PhaseWorkbenchCopy {
  const spec = getPhaseWorkbenchCopy(packId, phase)
  if (!spec) return fallback
  return {
    whatToOutput: spec.whatToOutput.length ? spec.whatToOutput : fallback.whatToOutput,
    recommendedSteps: spec.recommendedSteps.length ? spec.recommendedSteps : fallback.recommendedSteps,
    avoid: spec.avoid.length ? spec.avoid : fallback.avoid,
    whyNow: spec.whyNow || fallback.whyNow,
    skipRisk: spec.skipRisk || fallback.skipRisk,
    expectedGain: spec.expectedGain || fallback.expectedGain,
  }
}
