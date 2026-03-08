export interface ChapterOption {
  id: string
  name: string
}

export interface CourseOption {
  id: string
  name: string
  chapters: ChapterOption[]
}

export const courseOptions: CourseOption[] = [
  {
    id: 'computer_network',
    name: '计算机网络',
    chapters: [
      { id: 'tcp', name: 'TCP 协议' },
      { id: 'udp', name: 'UDP 协议' },
      { id: 'http', name: 'HTTP 基础' },
    ],
  },
  {
    id: 'operating_system',
    name: '操作系统',
    chapters: [
      { id: 'process-thread', name: '进程与线程' },
      { id: 'memory', name: '内存管理' },
      { id: 'io', name: 'I/O 与文件系统' },
    ],
  },
]
