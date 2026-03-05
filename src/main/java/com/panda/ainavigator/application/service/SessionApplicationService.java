package com.panda.ainavigator.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.panda.ainavigator.api.dto.session.CreateSessionRequest;
import com.panda.ainavigator.api.dto.session.CreateSessionResponse;
import com.panda.ainavigator.api.dto.session.MasterySummaryResponse;
import com.panda.ainavigator.api.dto.session.NextTaskResponse;
import com.panda.ainavigator.api.dto.session.PlanSessionResponse;
import com.panda.ainavigator.api.dto.session.PlannedTaskResponse;
import com.panda.ainavigator.api.dto.session.SessionOverviewResponse;
import com.panda.ainavigator.api.dto.session.TimelineItemResponse;
import com.panda.ainavigator.domain.model.Stage;
import com.panda.ainavigator.domain.model.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SessionApplicationService {

    private final AtomicLong sessionIdGen = new AtomicLong(120);
    private final ObjectMapper objectMapper;

    public SessionApplicationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public CreateSessionResponse createSession(CreateSessionRequest request) {
        return new CreateSessionResponse(sessionIdGen.incrementAndGet());
    }

    public PlanSessionResponse planSession(Long sessionId) {
        return new PlanSessionResponse(
                sessionId,
                List.of(
                        new PlannedTaskResponse(1001L, Stage.STRUCTURE, 101L,
                                "Build a TCP handshake concept map", TaskStatus.PENDING),
                        new PlannedTaskResponse(1002L, Stage.UNDERSTANDING, 101L,
                                "Explain why three-way handshake is required", TaskStatus.PENDING),
                        new PlannedTaskResponse(1003L, Stage.TRAINING, 101L,
                                "Generate three training questions with rubric", TaskStatus.PENDING),
                        new PlannedTaskResponse(1004L, Stage.REFLECTION, 101L,
                                "Summarize weak points and next step", TaskStatus.PENDING)
                )
        );
    }

    public SessionOverviewResponse overview(Long sessionId) {
        return new SessionOverviewResponse(
                sessionId,
                "computer_network",
                "tcp",
                "Understand reliable transport and solve practice tasks",
                101L,
                Stage.UNDERSTANDING,
                List.of(
                        new TimelineItemResponse(1001L, Stage.STRUCTURE, 101L, TaskStatus.SUCCEEDED),
                        new TimelineItemResponse(1002L, Stage.UNDERSTANDING, 101L, TaskStatus.PENDING),
                        new TimelineItemResponse(1003L, Stage.TRAINING, 101L, TaskStatus.PENDING)
                ),
                new NextTaskResponse(1002L, Stage.UNDERSTANDING, 101L),
                List.of(
                        new MasterySummaryResponse(101L, "three_way_handshake", 0.55),
                        new MasterySummaryResponse(102L, "sliding_window", 0.20)
                )
        );
    }

    public JsonNode mockUnderstandingOutput() {
        return objectMapper.valueToTree(Map.of(
                "sections", List.of(
                        Map.of("type", "concepts", "title", "Core Concepts", "bullets", List.of("TCP is a logical connection", "SYN/ACK aligns initial sequence numbers")),
                        Map.of("type", "mechanism", "title", "Flow", "steps", List.of("Client sends SYN", "Server returns SYN+ACK", "Client returns ACK")),
                        Map.of("type", "misconceptions", "title", "Common Mistakes", "items", List.of("Two-way handshake is enough", "Handshake only checks if peer is online")),
                        Map.of("type", "summary", "title", "One Minute Summary", "text", "Three-way handshake builds consistent state and sequence alignment")
                )
        ));
    }
}