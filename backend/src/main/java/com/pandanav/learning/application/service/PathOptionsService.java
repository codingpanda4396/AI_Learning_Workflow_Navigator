package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.GoalDiagnosisRequest;
import com.pandanav.learning.api.dto.session.PathOptionsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PathOptionsService {

    public PathOptionsResponse buildOptions(GoalDiagnosisRequest request) {
        String chapter = request.chapterId();
        String goal = request.goalText();
        List<PathOptionsResponse.PathOptionResponse> options = List.of(
            new PathOptionsResponse.PathOptionResponse(
                "steady",
                "稳扎稳打",
                "先打基础再训练，适合零基础或长期掌握。",
                "EASY",
                90,
                List.of(
                    "梳理 " + chapter + " 的核心概念和边界",
                    "理解关键机制并纠正常见误区",
                    "完成 2 轮基础训练题并复盘",
                    "输出 1 份可复用总结"
                )
            ),
            new PathOptionsResponse.PathOptionResponse(
                "exam_sprint",
                "考试冲刺",
                "以得分为导向，快速覆盖高频题型。",
                "MEDIUM",
                60,
                List.of(
                    "定位 " + chapter + " 高频考点",
                    "按题型进行分组训练",
                    "针对错题做定向强化",
                    "进行 1 次限时模拟"
                )
            ),
            new PathOptionsResponse.PathOptionResponse(
                "project_apply",
                "实战应用",
                "围绕目标构建实际产出，强调迁移能力。",
                "HARD",
                120,
                List.of(
                    "将目标拆解为可交付成果",
                    "建立 " + chapter + " 的实践场景",
                    "完成一轮实现与自测",
                    "复盘并形成改进清单"
                )
            )
        );
        if (goal.length() > 60) {
            return new PathOptionsResponse(List.of(options.get(0), options.get(2), options.get(1)));
        }
        return new PathOptionsResponse(options);
    }
}

