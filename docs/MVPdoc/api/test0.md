{
    "rawGoalText": "我想搞懂链表",
    "timeBudget": "WITHIN_30_MIN",
    "selfReportedLevel": "BASIC",
    "preferenceTags": [
        "CONCEPT_FIRST",
        "STEP_BY_STEP"
    ],
    "goalTypeHint": "LEARN_NEW_CONCEPT",
    "subjectHint": "数据结构",
    "topicHints": [
        "链表"
    ],
    "sourceContext": "408复习"
}
{
    "code": "OK",
    "message": "success",
    "data": {
        "goalId": "goal_001",
        "structuredGoal": {
            "rawGoalText": "我想搞懂链表",
            "normalizedGoalText": "理解链表的核心概念与基本操作",
            "goalType": "LEARN_NEW_CONCEPT",
            "subject": "数据结构",
            "topicScopeType": "SINGLE_TOPIC",
            "topics": [
                "链表"
            ],
            "intentDescription": "用户希望理解链表概念，并具备后续进入基本操作学习的准备",
            "timeBudget": "WITHIN_30_MIN",
            "urgencyLevel": "MEDIUM",
            "expectedDepth": "UNDERSTAND_AND_BASIC_USE",
            "selfReportedLevel": "BASIC",
            "preferenceTags": [
                "CONCEPT_FIRST",
                "STEP_BY_STEP"
            ],
            "constraints": [],
            "sourceContext": "408复习"
        },
        "goalContextSnapshot": {
            "structuredGoal": null,
            "requiresDiagnosis": true,
            "planningMode": "CONCEPT_CLARIFICATION",
            "entryGranularity": "SMALL",
            "strategyHints": [
                "CORE_CONCEPT_FIRST",
                "ONE_STEP_ONE_CHECK"
            ],
            "riskTags": [
                "SHALLOW_UNDERSTANDING_RISK"
            ],
            "explanationFocus": [
                "为什么先补概念",
                "为什么不直接做题"
            ],
            "createdFrom": "USER_INPUT_V1",
            "version": 1
        }
    }
}


{
    "goalId": "goal_001"
}
{
    "code": "OK",
    "message": "success",
    "data": {
        "diagnosisId": "diag_001",
        "sessionId": "learn_session_001",
        "status": "READY",
        "generationMode": "STRUCTURED",
        "questions": [
            {
                "questionId": "q_foundation",
                "dimension": "FOUNDATION",
                "type": "SINGLE_CHOICE",
                "required": true,
                "title": "你对链表基础掌握更接近哪种状态？",
                "description": "这会影响系统从概念讲解还是操作练习开始。",
                "whyAsking": "判断你是否需要先补基础概念。",
                "impactsPlanning": [
                    "ENTRY_STRATEGY",
                    "ENTRY_GRANULARITY"
                ],
                "options": [
                    {
                        "code": "BEGINNER",
                        "label": "刚开始接触",
                        "order": 1
                    },
                    {
                        "code": "BASIC",
                        "label": "学过但不太熟",
                        "order": 2
                    },
                    {
                        "code": "PROFICIENT",
                        "label": "概念基本知道",
                        "order": 3
                    }
                ]
            },
            {
                "questionId": "q_gap",
                "dimension": "GAP",
                "type": "SINGLE_CHOICE",
                "required": true,
                "title": "你现在最卡的是哪一类问题？",
                "description": "系统会据此决定是先讲概念、结构还是例题。",
                "whyAsking": "识别当前最主要的学习缺口。",
                "impactsPlanning": [
                    "RECOMMENDED_STRATEGY"
                ],
                "options": [
                    {
                        "code": "CONCEPT_GAP",
                        "label": "概念本身不清楚",
                        "order": 1
                    },
                    {
                        "code": "STRUCTURE_GAP",
                        "label": "结构关系容易混",
                        "order": 2
                    },
                    {
                        "code": "APPLICATION_GAP",
                        "label": "会概念但不会做题",
                        "order": 3
                    }
                ]
            }
        ]
    }
}

{
    "diagnosisId": "diag_001",
    "answers": [
        {
            "questionId": "q_foundation",
            "selectedOptions": [
                "BASIC"
            ]
        },
        {
            "questionId": "q_gap",
            "selectedOptions": [
                "CONCEPT_GAP"
            ]
        }
    ]
}
{
    "code": "OK",
    "message": "success",
    "data": {
        "diagnosisId": "diag_001",
        "learnerProfileSnapshot": {
            "diagnosisId": "diag_001",
            "foundationLevel": "BASIC",
            "confidenceLevel": "MEDIUM",
            "comprehensionPattern": "CONCEPT_KNOWN_BUT_UNSTABLE",
            "executionPattern": "NEEDS_GUIDED_PROGRESS",
            "blockerTags": [
                "CONCEPT_GAP"
            ],
            "riskTags": [
                "SHALLOW_UNDERSTANDING_RISK"
            ],
            "suggestedEntryStrategy": "START_FROM_CORE_DEFINITION",
            "suggestedGranularity": "SMALL",
            "suggestedFeedbackFrequency": "EACH_TASK",
            "planningHints": [
                "先建立链表节点与指针关系",
                "先做概念解释，再进入简单例子"
            ]
        },
        "diagnosisEvidenceSummary": {
            "summary": "用户对链表有初步接触，但核心概念稳定性不足，当前主要缺口在概念理解层。",
            "keyEvidence": [
                "用户自评为学过但不太熟",
                "主要困难选择为概念本身不清楚"
            ],
            "primaryGapType": "CONCEPT_GAP",
            "primaryRiskTags": [
                "SHALLOW_UNDERSTANDING_RISK"
            ],
            "explanationPoints": [
                "不建议直接进入做题",
                "更适合先做概念澄清型任务"
            ]
        }
    }
}


{
    "goalId": "goal_001",
    "diagnosisId": "diag_001"
}
{
    "code": "OK",
    "message": "success",
    "data": {
        "planId": "plan_001",
        "status": "PREVIEW_READY",
        "previewOnly": true,
        "committed": false,
        "goal": "理解链表",
        "recommendedEntry": {
            "conceptId": "linked_list_foundation",
            "title": "先建立链表节点与指针连接的基本理解",
            "estimatedMinutes": 8,
            "reason": "当前主要缺口在概念层，如果直接做题会放大理解断层。"
        },
        "recommendedStrategy": {
            "code": "CLARIFY_CORE_CONCEPT",
            "label": "先澄清核心概念，再做轻量练习",
            "reason": "适合基础不稳但已有接触的用户。"
        },
        "stages": [
            {
                "stageCode": "STAGE_1",
                "title": "概念澄清",
                "objective": "理解链表节点、指针和连接关系",
                "estimatedMinutes": 15
            },
            {
                "stageCode": "STAGE_2",
                "title": "轻量检验",
                "objective": "用自己的话解释并完成一个微练习",
                "estimatedMinutes": 10
            }
        ],
        "tasks": [
            {
                "taskId": "task_001",
                "title": "理解链表的基本结构",
                "taskType": "CONCEPT_EXPLAIN",
                "goal": "用自己的话说清链表由什么组成",
                "estimatedMinutes": 8,
                "promptScaffold": "请先解释什么是链表、节点、指针，它们之间是什么关系，并给一个最小例子。",
                "completionCriteria": [
                    "能说出节点和指针的作用",
                    "能描述节点之间如何连接"
                ],
                "evidenceToCollect": [
                    "interactionCount",
                    "userSummarySubmitted"
                ],
                "fallbackAction": "如果还是混乱，要求导师只用一个两节点例子重新解释"
            },
            {
                "taskId": "task_002",
                "title": "对比链表与顺序表",
                "taskType": "COMPARE_AND_CONNECT",
                "goal": "理解链表与数组在存储方式上的核心区别",
                "estimatedMinutes": 7,
                "promptScaffold": "请从存储结构、插入删除、访问方式三个角度对比链表和数组。",
                "completionCriteria": [
                    "至少说出两点区别",
                    "能解释为什么链表不要求连续存储"
                ],
                "evidenceToCollect": [
                    "interactionCount",
                    "userSummarySubmitted"
                ],
                "fallbackAction": "如果无法比较，先只比较是否连续存储这一点"
            },
            {
                "taskId": "task_003",
                "title": "完成一个最小自我解释",
                "taskType": "SELF_EXPLANATION",
                "goal": "用自己的话完整解释链表",
                "estimatedMinutes": 5,
                "promptScaffold": "请你不用术语堆砌，像讲给同学一样解释链表是什么。",
                "completionCriteria": [
                    "解释中包含节点、指针、连接关系",
                    "表达连贯，不只是复述定义"
                ],
                "evidenceToCollect": [
                    "userSummarySubmitted",
                    "learnerReflection"
                ],
                "fallbackAction": "先给一句模板开头，再让用户补全"
            }
        ],
        "successCriteria": [
            "能解释链表的基本组成",
            "能区分链表和数组的一个核心差异",
            "能完成最小自我解释"
        ],
        "keyEvidence": [
            "诊断显示主要缺口在概念理解",
            "当前时间预算适合 3 个小任务"
        ],
        "risks": [
            "如果跳过概念澄清，后续容易停留在术语记忆层"
        ]
    }
}


{
    "planId": "plan_001"
}
{
    "code": "OK",
    "message": "success",
    "data": {
        "sessionId": "learn_session_001",
        "planId": "plan_001",
        "taskSequence": [
            "task_001",
            "task_002",
            "task_003"
        ],
        "currentTaskId": "task_001",
        "status": "IN_PROGRESS"
    }
}

{
    "code": "OK",
    "message": "success",
    "data": {
        "sessionId": "learn_session_001",
        "currentTask": {
            "taskId": "task_001",
            "title": "理解链表的基本结构",
            "taskType": "CONCEPT_EXPLAIN",
            "goal": "用自己的话说清链表由什么组成",
            "whyThisTask": "这是当前最关键的基础点，后续所有操作都建立在这里。",
            "estimatedMinutes": 8,
            "promptScaffold": "请先解释什么是链表、节点、指针，它们之间是什么关系，并给一个最小例子。",
            "completionCriteria": [
                "能说出节点和指针的作用",
                "能描述节点之间如何连接"
            ],
            "fallbackAction": "如果还是混乱，要求导师只用一个两节点例子重新解释"
        },
        "progress": {
            "currentIndex": 1,
            "totalTasks": 3
        }
    }
}

{
    "code": "OK",
    "message": "success",
    "data": {
        "learningReport": {
            "sessionId": "learn_session_001",
            "resultStatus": "PARTIALLY_ACHIEVED",
            "goalReview": "本轮目标是理解链表的基本概念与结构。",
            "completedProgress": [
                "已能解释链表由节点和指针构成",
                "已初步理解链表与数组的存储差异"
            ],
            "unresolvedIssues": [
                "对链表操作场景的理解还不稳定"
            ],
            "evidenceSummary": [
                "3 个任务中完成了 3 个",
                "完成了自我解释",
                "互动次数表明用户有主动澄清行为"
            ],
            "summaryText": "本轮已经完成概念澄清，但距离稳定应用还差一步，建议继续做轻量巩固。",
            "nextAction": {
                "actionType": "REINFORCE",
                "reason": "概念已建立，但应用层仍需巩固。",
                "nextEntryPoint": "链表的基本操作与简单题",
                "adjustmentSignals": [
                    "进入轻量练习",
                    "保留概念回顾检查点"
                ],
                "requiresReplan": false
            }
        },
        "nextActionDecision": {
            "actionType": "REINFORCE",
            "reason": "概念已建立，但应用层仍需巩固。",
            "nextEntryPoint": "链表的基本操作与简单题",
            "adjustmentSignals": [
                "进入轻量练习",
                "保留概念回顾检查点"
            ],
            "requiresReplan": false
        }
    }
}

{
    "code": "OK",
    "message": "success",
    "data": {
        "sessionId": "learn_session_001",
        "acceptedAction": "REINFORCE",
        "requiresReplan": false,
        "nextHint": "下一轮建议进入链表基本操作与简单练习。"
    }
}
