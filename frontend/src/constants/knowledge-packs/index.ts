import type { KnowledgePack, KnowledgePackId } from '@/types/knowledgePack'
import {
  CHOICE_TEMPLATE,
  COMPARE_TEMPLATE,
  MECHANISM_TEMPLATE,
  SEQUENCE_TEMPLATE,
} from '@/constants/knowledge-packs/templates'

const osProcessThreadPack: KnowledgePack = {
  id: 'os_process_thread',
  executionTemplate: 'CONCEPT',
  ...COMPARE_TEMPLATE,
  planning: {
    ...COMPARE_TEMPLATE.planning,
    hero: {
      title: '进程与线程：先把边界划清',
      subtitle: '资源归属、调度对象、切换开销一次讲透。',
      auxiliaryLine: 'COMPARE 模板：五维对比卡驱动四阶段。',
    },
    commonMisconceptions: ['把线程当成“轻量进程”就结束，不看共享资源风险。'],
    phaseHighlights: {
      STRUCTURE: '先建五维对比坐标',
      UNDERSTANDING: '解释每一维为何不同',
      TRAINING: '用场景做选择',
      REFLECTION: '回到浏览器多进程多线程',
    },
    optionalTips: ['把“资源归属/隔离性/通信/切换成本/故障影响”写成一行表格。'],
    steps: [
      { flowTitle: '建立对比维度', flowSubtitle: '先搭五维表', icon: 'brain', headline: '画出进程 vs 线程五维对比卡', whyFirst: '没维度就会混着背。', objectiveIntro: '你将能：', objectiveBullets: ['写出五维标签', '知道每维在比较什么'], timeLabel: '约 6 分钟', suggestedPrompt: '请先用五个维度比较进程和线程，不展开底层细节。', reflectionQuestions: ['哪一维我最容易混？'], closingLine: '先把边界写清。' },
      { flowTitle: '讲清因果', flowSubtitle: '为什么会不同', icon: 'puzzle', headline: '解释差异背后的代价与收益', whyFirst: '会背不会用通常卡在“为什么”。', objectiveIntro: '你将能：', objectiveBullets: ['解释共享与隔离的取舍'], timeLabel: '约 8 分钟', suggestedPrompt: '请解释为什么线程切换一般更轻，代价是什么。', reflectionQuestions: ['快的代价是什么？'], closingLine: '差异=取舍。' },
      { flowTitle: '场景判断', flowSubtitle: '放进真实系统', icon: 'message', headline: '用浏览器场景判断多进程+多线程', whyFirst: '场景能暴露真假理解。', objectiveIntro: '你将能：', objectiveBullets: ['说清架构选择理由'], timeLabel: '约 8 分钟', suggestedPrompt: '浏览器为什么通常是多进程且每个进程多线程？请按可靠性/性能解释。', reflectionQuestions: ['只用线程会怎样？'], closingLine: '用场景验证。' },
      { flowTitle: '快速检查', flowSubtitle: '收束误区', icon: 'check', headline: '完成反事实检查题', whyFirst: '最后一步防止回到口号。', objectiveIntro: '你将能：', objectiveBullets: ['独立判断架构选择'], timeLabel: '约 5 分钟', suggestedPrompt: '给我一道进程线程场景判断题并点评答案。', reflectionQuestions: ['我是否只会背术语？'], closingLine: '带走可复用判断句。' },
    ],
  },
  execution: {
    starterPrompts: ['给我进程/线程五维对比表', '先讲浏览器为什么多进程', '我先说理解你来纠偏'],
    scaffoldCards: [
      {
        id: 'compare-board',
        title: '先搭骨架',
        hint: '五维对比：资源、调度、切换、隔离、通信。',
        actionLabel: '开始搭骨架',
        prompt: '请基于五维对比板帮我快速区分进程与线程。',
      },
      {
        id: 'case-drill',
        title: '看概念对比',
        hint: '用场景判断差异，先抓边界。',
        actionLabel: '看概念对比',
        prompt: '给一个系统设计场景，让我判断更偏向进程还是线程并说明理由。',
      },
      {
        id: 'misconception-fix',
        title: '我先说一句',
        hint: '先暴露理解，只纠最关键误区。',
        actionLabel: '我先试着说',
        prompt: '我先讲理解，你只指出最关键的一处误区并给纠偏句。',
      },
    ],
    phaseHero: { ORIENT: '先搭对比坐标，再谈细节。', CHECK: '回答场景判断题，验证不是死记。' },
    phaseObjective: { ORIENT: '完成五维框架', EXPLORE: '讲清关键取舍', CHECK: '独立做场景判断' },
    microCheckLabels: ['我能说出五维差异', '我能解释为什么线程更轻', '我能用场景做选择'],
  },
  tutor: { focusLabel: '进程与线程', constrainedHints: ['优先对比与纠偏，避免先钻调度细节。'], suggestedQuestions: ['这个场景优先进程还是线程？依据是什么？'] },
  checkpoint: { checkpointPrompt: '浏览器为什么通常采用多进程 + 多线程？如果只保留一种会怎样？', checkpointRubric: ['提到隔离性', '提到并发与响应', '说明反事实后果'] },
}

const tcpHandshakePack: KnowledgePack = {
  id: 'net_tcp_handshake',
  executionTemplate: 'PROCESS',
  ...SEQUENCE_TEMPLATE,
  planning: { ...SEQUENCE_TEMPLATE.planning, hero: { title: 'TCP 三次握手：按时间顺序讲明白', subtitle: '每一步确认什么、为什么必须三次。', auxiliaryLine: 'SEQUENCE 模板：三步时序条。' }, optionalTips: ['只围绕 SYN / SYN-ACK / ACK 三步，不先展开拥塞控制。'], commonMisconceptions: ['只记“要三次”但说不出每次确认什么。'], steps: osProcessThreadPack.planning.steps.map((s, i)=> ({...s, flowTitle:['建立时序框架','解释每一步确认','反事实推演','检查与复盘'][i]!, headline:['画出三步时序条','解释双方状态变化','两次握手会怎样','完成握手检查题'][i]!})) },
  execution: {
    ...osProcessThreadPack.execution,
    starterPrompts: ['先画 TCP 三步时序', '解释为什么不是两次', '我复述一遍你纠错'],
    microCheckLabels: ['我能按顺序复述三步', '我能解释为什么三次', '我能说明两次握手风险'],
    scaffoldCards: [
      {
        id: 'tcp-steps',
        title: '看过程分解',
        hint: 'SYN / SYN-ACK / ACK 各确认什么。',
        actionLabel: '看过程分解',
        prompt: '请按时间顺序分解 TCP 三次握手：每一步交换什么、确认什么。',
      },
      {
        id: 'tcp-state',
        title: '看状态变化',
        hint: '双方状态如何迁移。',
        actionLabel: '看状态变化',
        prompt: '请只讲三次握手里双方状态如何变化，用最少步骤说明。',
      },
      {
        id: 'tcp-retell',
        title: '我来复述流程',
        hint: '你复述，导师纠偏。',
        actionLabel: '我来复述流程',
        prompt: '我先复述三次握手流程，你只纠正最关键的一步。我的复述是：',
      },
    ],
  },
  tutor: { focusLabel: 'TCP 三次握手', constrainedHints: ['优先时间顺序与“为什么三次”。'], suggestedQuestions: ['如果只有两次握手会出现什么问题？'] },
  checkpoint: { checkpointPrompt: '如果 TCP 只有两次握手，会带来什么具体风险？', checkpointRubric: ['能说出状态不一致风险', '用时序解释', '结论清晰'] },
}

const dfsBfsPack: KnowledgePack = {
  id: 'ds_dfs_bfs',
  executionTemplate: 'PROBLEM',
  ...CHOICE_TEMPLATE,
  planning: { ...CHOICE_TEMPLATE.planning, hero: { title: 'DFS vs BFS：先学会“选法”', subtitle: '看题线索 -> 搜索策略，不靠模板记忆。', auxiliaryLine: 'CHOICE 模板：算法选择地图。' }, optionalTips: ['把“最短路径/层序/回溯深搜”做成判断树。'], commonMisconceptions: ['背代码模板代替题型判断。'], steps: osProcessThreadPack.planning.steps.map((s, i)=> ({...s, flowTitle:['搭建选择地图','提炼题型线索','做题站队训练','检查与复盘'][i]!, headline:['先画 DFS/BFS 判断树','题干词如何触发选择','做三道站队小题','复盘误判原因'][i]!})) },
  execution: {
    ...osProcessThreadPack.execution,
    starterPrompts: ['给我 DFS/BFS 选择树', '给三道题让我先选法', '我先判断你只纠偏'],
    microCheckLabels: ['我能说出首选信号', '我能解释不用另一种的原因', '我能从题干抽线索'],
    scaffoldCards: [
      {
        id: 'dfsbfs-type',
        title: '先识别题型',
        hint: '最短路径 / 层序 / 回溯 等线索。',
        actionLabel: '先识别题型',
        prompt: '请帮我从题干判断更优先 DFS 还是 BFS，并指出触发线索。',
      },
      {
        id: 'dfsbfs-skel',
        title: '看解题骨架',
        hint: '步骤与依据，不代写代码。',
        actionLabel: '看解题骨架',
        prompt: '请只给 DFS/BFS 解题骨架（步骤与判断依据），不要直接给完整代码。',
      },
      {
        id: 'dfsbfs-step',
        title: '我先做一步',
        hint: '先做一步再逐步加提示。',
        actionLabel: '我先做一步',
        prompt: '我先写出第一步选法与理由，你只告诉我方向对不对。我的第一步是：',
      },
    ],
  },
  tutor: { focusLabel: 'DFS / BFS 选择', constrainedHints: ['优先选择依据与题型线索，弱化模板代码。'], suggestedQuestions: ['题干里哪个词最该触发 BFS？'] },
  checkpoint: { checkpointPrompt: '给出 3 个题型，分别判断更优先 DFS 还是 BFS，并说明线索。', checkpointRubric: ['选择正确', '线索对应题干', '理由简洁'] },
}

const cacheLocalityPack: KnowledgePack = {
  id: 'arch_cache_locality',
  executionTemplate: 'CONCEPT',
  ...MECHANISM_TEMPLATE,
  planning: { ...MECHANISM_TEMPLATE.planning, hero: { title: '缓存与局部性：建立命中直觉', subtitle: '从访问模式解释性能差异。', auxiliaryLine: 'MECHANISM 模板：访问模式对比卡。' }, optionalTips: ['只用“按行/按列遍历二维数组”先建立直觉。'], commonMisconceptions: ['直接下潜硬件细节，忽略访问模式。'], steps: osProcessThreadPack.planning.steps.map((s, i)=> ({...s, flowTitle:['搭建机制骨架','解释命中原因','访问模式实验','性能判断检查'][i]!, headline:['画出访问模式对比卡','解释空间/时间局部性','对比按行与按列','完成性能判断题'][i]!})) },
  execution: {
    ...osProcessThreadPack.execution,
    starterPrompts: ['先解释局部性直觉', '比较按行/按列遍历', '我先说命中直觉你纠错'],
    microCheckLabels: ['我能解释局部性', '我能判断访问模式快慢', '我能避免硬件细节跑偏'],
    scaffoldCards: [
      {
        id: 'cache-frame',
        title: '先搭骨架',
        hint: '是什么、解决什么问题、局部性直觉。',
        actionLabel: '开始搭骨架',
        prompt: '请用三句话建立缓存与局部性的框架：它是什么、解决什么问题、时间/空间局部性各指什么。',
      },
      {
        id: 'cache-example',
        title: '看最小例子',
        hint: '例如按行/按列遍历二维数组。',
        actionLabel: '看最小例子',
        prompt: '请用一个最小例子说明访问模式如何影响缓存命中（例如二维数组按行 vs 按列）。',
      },
      {
        id: 'cache-say',
        title: '我先试着说',
        hint: '先写一句理解，再收紧。',
        actionLabel: '我先试着说',
        prompt: '关于缓存为什么有效，我先说一句理解，你只指出最关键的问题。我的理解是：',
      },
    ],
  },
  tutor: { focusLabel: '缓存与局部性', constrainedHints: ['优先访问模式与命中直觉，避免直接下潜硬件细节。'], suggestedQuestions: ['为什么按行遍历二维数组通常更快？'] },
  checkpoint: { checkpointPrompt: '二维数组按行遍历与按列遍历哪个通常更快？请解释原因。', checkpointRubric: ['结论正确', '提到局部性与命中', '表达清晰'] },
}

export const KNOWLEDGE_PACKS: Record<KnowledgePackId, KnowledgePack> = {
  os_process_thread: osProcessThreadPack,
  net_tcp_handshake: tcpHandshakePack,
  ds_dfs_bfs: dfsBfsPack,
  arch_cache_locality: cacheLocalityPack,
}
