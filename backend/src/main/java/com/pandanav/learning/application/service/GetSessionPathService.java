package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.PathNodeResponse;
import com.pandanav.learning.api.dto.session.PathResponse;
import com.pandanav.learning.application.usecase.GetSessionPathUseCase;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GetSessionPathService implements GetSessionPathUseCase {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_NOT_STARTED = "NOT_STARTED";

    private final SessionRepository sessionRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;

    public GetSessionPathService(
        SessionRepository sessionRepository,
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
    }

    @Override
    public PathResponse execute(Long sessionId) {
        var session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new NotFoundException("Session not found."));

        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        if (nodes.isEmpty()) {
            throw new NotFoundException("No concept nodes found for chapter.");
        }

        List<Mastery> masteries = masteryRepository.findByUserIdAndChapterId(session.getUserId(), session.getChapterId());
        Map<Long, BigDecimal> masteryByNodeId = masteries.stream()
            .collect(Collectors.toMap(Mastery::getNodeId, mastery -> mastery.getMasteryValue(), (a, b) -> b));

        int currentIndex = -1;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(session.getCurrentNodeId())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            throw new NotFoundException("Current node not found for session chapter.");
        }

        List<PathNodeResponse> nodeResponses = mapNodes(nodes, masteryByNodeId, currentIndex);
        return new PathResponse(session.getId(), session.getCurrentNodeId(), nodeResponses);
    }

    private List<PathNodeResponse> mapNodes(
        List<ConceptNode> nodes,
        Map<Long, BigDecimal> masteryByNodeId,
        int currentIndex
    ) {
        return java.util.stream.IntStream.range(0, nodes.size())
            .mapToObj(index -> {
                ConceptNode node = nodes.get(index);
                String status = index < currentIndex
                    ? STATUS_COMPLETED
                    : index == currentIndex ? STATUS_IN_PROGRESS : STATUS_NOT_STARTED;
                BigDecimal masteryValue = masteryByNodeId.getOrDefault(node.getId(), BigDecimal.ZERO);
                return new PathNodeResponse(node.getId(), node.getName(), status, masteryValue);
            })
            .toList();
    }
}
