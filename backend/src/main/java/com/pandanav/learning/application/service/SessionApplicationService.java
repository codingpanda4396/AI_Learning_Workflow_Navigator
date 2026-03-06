package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.api.dto.session.MasterySummaryResponse;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.PlannedTaskResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.api.dto.session.TimelineItemResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class SessionApplicationService {

    public CreateSessionResponse createSession(CreateSessionRequest request) {
        return new CreateSessionResponse(123L);
    }

    public PlanSessionResponse planSession(Long sessionId) {
        return new PlanSessionResponse(
            sessionId,
            List.of(
                new PlannedTaskResponse(1001L, "STRUCTURE", 101L, "围绕三次握手建立结构化认知", "PENDING"),
                new PlannedTaskResponse(1002L, "UNDERSTANDING", 101L, "解释三次握手机制链路与常见误区", "PENDING"),
                new PlannedTaskResponse(1003L, "TRAINING", 101L, "输出训练题并附评分 rubric", "PENDING"),
                new PlannedTaskResponse(1004L, "REFLECTION", 101L, "总结错因并给出下一步建议", "PENDING")
            )
        );
    }

    public SessionOverviewResponse getOverview(Long sessionId) {
        return new SessionOverviewResponse(
            sessionId,
            "computer_network",
            "tcp",
            "理解 TCP 可靠传输机制并能做题",
            101L,
            "UNDERSTANDING",
            List.of(
                new TimelineItemResponse(1001L, "STRUCTURE", 101L, "SUCCEEDED"),
                new TimelineItemResponse(1002L, "UNDERSTANDING", 101L, "PENDING"),
                new TimelineItemResponse(1003L, "TRAINING", 101L, "PENDING")
            ),
            new NextTaskResponse(1002L, "UNDERSTANDING", 101L),
            List.of(
                new MasterySummaryResponse(101L, "三次握手", new BigDecimal("0.55")),
                new MasterySummaryResponse(102L, "滑动窗口", new BigDecimal("0.20"))
            )
        );
    }
}
