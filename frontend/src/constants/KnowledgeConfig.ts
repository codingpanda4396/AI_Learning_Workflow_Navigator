import type { KnowledgePackId } from '@/types/knowledgePack'

/** 与执行机读阶段一致（与 STEP_COPY.stageLabel 对齐） */
export type KnowledgeDemoStageCode =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type DiagnosisOptionValue = 'A' | 'B' | 'C' | 'D'

export type KnowledgeDiagnosisOption = {
  label: string
  value: DiagnosisOptionValue
}

export type KnowledgeDemoConfig = {
  id: KnowledgePackId
  name: string
  subject: string
  diagnosisQuestion: {
    question: string
    options: KnowledgeDiagnosisOption[]
  }
  plan: {
    problem: string
    strategy: string
    stages: KnowledgeDemoStageCode[]
  }
  execution: Record<
    KnowledgeDemoStageCode,
    {
      /** 顶栏「当前任务」强提示 */
      currentTaskLine: string
      prompts: string[]
    }
  >
}

export const KNOWLEDGE_DEMO_CONFIGS: Record<KnowledgePackId, KnowledgeDemoConfig> = {
  os_process_thread: {
    id: 'os_process_thread',
    name: '进程与线程',
    subject: '操作系统',
    diagnosisQuestion: {
      question: '当多个程序同时运行时：',
      options: [
        { label: '我只知道它们在同时运行', value: 'A' },
        { label: '我知道有进程和线程，但不清楚区别', value: 'B' },
        { label: '我知道区别，但搞不清切换和调度', value: 'C' },
        { label: '我可以解释上下文切换过程', value: 'D' },
      ],
    },
    plan: {
      problem: '分不清进程与线程 + 不理解切换',
      strategy: '机制优先（UNDERSTANDING FIRST）',
      stages: ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'],
    },
    execution: {
      STRUCTURE: {
        currentTaskLine: '建立结构：进程和线程分别是什么、解决什么问题',
        prompts: ['进程和线程分别是什么？解决什么问题？'],
      },
      UNDERSTANDING: {
        currentTaskLine: '搞懂「线程切换时 CPU 做了什么、为何比进程快」',
        prompts: [
          '一个线程切换时，CPU 具体做了什么？',
          '为什么线程切换比进程快？',
        ],
      },
      TRAINING: {
        currentTaskLine: '用场景判断共享资源与进程/线程选择',
        prompts: [
          '如果一个程序有多个线程，它们如何共享资源？',
          '这个场景适合多线程还是多进程？',
        ],
      },
      REFLECTION: {
        currentTaskLine: '总结：进程 vs 线程的本质区别',
        prompts: ['总结：进程 vs 线程，本质区别是什么？'],
      },
    },
  },

  net_tcp_handshake: {
    id: 'net_tcp_handshake',
    name: 'TCP 三次握手',
    subject: '计算机网络',
    diagnosisQuestion: {
      question: 'TCP 三次握手：',
      options: [
        { label: '记住了 SYN/ACK', value: 'A' },
        { label: '知道三步，但不理解为什么', value: 'B' },
        { label: '理解流程，但讲不清每一步的作用', value: 'C' },
        { label: '能解释丢包/重传场景', value: 'D' },
      ],
    },
    plan: {
      problem: '流程记住但不理解',
      strategy: '机制拆解（UNDERSTANDING）',
      stages: ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'],
    },
    execution: {
      STRUCTURE: {
        currentTaskLine: '先说清：TCP 三次握手是干什么的',
        prompts: ['TCP 三次握手是干什么的？'],
      },
      UNDERSTANDING: {
        currentTaskLine: '搞懂「三次握手每一步在确认什么」',
        prompts: [
          '第一步 SYN 在确认什么？',
          '为什么不能两次握手？',
          '第三次 ACK 的意义是什么？',
        ],
      },
      TRAINING: {
        currentTaskLine: '推演异常：丢包与等待',
        prompts: [
          '如果第二次握手丢了会怎样？',
          '为什么服务器需要等待 ACK？',
        ],
      },
      REFLECTION: {
        currentTaskLine: '总结：三次握手每一步的作用',
        prompts: ['总结：三次握手每一步的作用'],
      },
    },
  },

  ds_dfs_bfs: {
    id: 'ds_dfs_bfs',
    name: 'DFS 与 BFS',
    subject: '数据结构',
    diagnosisQuestion: {
      question: 'DFS 和 BFS：',
      options: [
        { label: '名字会，但不会用', value: 'A' },
        { label: '会写代码，但不知道适用场景', value: 'B' },
        { label: '知道场景，但不会分析复杂度', value: 'C' },
        { label: '能根据问题选择策略', value: 'D' },
      ],
    },
    plan: {
      problem: '应用能力弱',
      strategy: '训练驱动（TRAINING FIRST）',
      stages: ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'],
    },
    execution: {
      STRUCTURE: {
        currentTaskLine: '建立结构：DFS 与 BFS 的核心区别',
        prompts: ['DFS 和 BFS 的核心区别是什么？'],
      },
      UNDERSTANDING: {
        currentTaskLine: '理解队列/递归与适用直觉',
        prompts: ['为什么 BFS 用队列？', 'DFS 为什么适合递归？'],
      },
      TRAINING: {
        currentTaskLine: '做题：最短路径与题型站队',
        prompts: [
          '找最短路径应该用哪个？为什么？',
          '这个问题适合 DFS 还是 BFS？',
        ],
      },
      REFLECTION: {
        currentTaskLine: '总结：什么时候用 DFS / BFS',
        prompts: ['总结：什么时候用 DFS / BFS'],
      },
    },
  },

  arch_cache_locality: {
    id: 'arch_cache_locality',
    name: '缓存与局部性',
    subject: '组成原理',
    diagnosisQuestion: {
      question: '缓存为什么能提升性能：',
      options: [
        { label: '知道有缓存', value: 'A' },
        { label: '知道局部性，但说不清', value: 'B' },
        { label: '理解时间/空间局部性', value: 'C' },
        { label: '能分析访问模式', value: 'D' },
      ],
    },
    plan: {
      problem: '直觉缺失',
      strategy: '直觉构建（UNDERSTANDING + EXAMPLE）',
      stages: ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'],
    },
    execution: {
      STRUCTURE: {
        currentTaskLine: '先说清：缓存是干什么的',
        prompts: ['缓存是干什么的？'],
      },
      UNDERSTANDING: {
        currentTaskLine: '建立局部性与访问顺序直觉',
        prompts: [
          '为什么顺序访问更快？',
          '什么是时间局部性？',
          '什么是空间局部性？',
        ],
      },
      TRAINING: {
        currentTaskLine: '判断访问模式与性能',
        prompts: [
          '这个代码访问模式有没有局部性？',
          '为什么这个程序慢？',
        ],
      },
      REFLECTION: {
        currentTaskLine: '总结：缓存为什么有效',
        prompts: ['总结：缓存为什么有效'],
      },
    },
  },
}

export function getKnowledgeDemoConfig(
  packId: string | null | undefined
): KnowledgeDemoConfig | null {
  if (!packId) return null
  const id = packId.trim() as KnowledgePackId
  return KNOWLEDGE_DEMO_CONFIGS[id] ?? null
}
