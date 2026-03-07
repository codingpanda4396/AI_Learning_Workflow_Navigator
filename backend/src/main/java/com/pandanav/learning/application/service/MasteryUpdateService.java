package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.repository.MasteryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class MasteryUpdateService {

    private final MasteryRepository masteryRepository;

    public MasteryUpdateService(MasteryRepository masteryRepository) {
        this.masteryRepository = masteryRepository;
    }

    public MasteryUpdateResult update(String userId, Long nodeId, String nodeName, int score) {
        BigDecimal before = masteryRepository.findByUserIdAndNodeId(userId, nodeId)
            .map(Mastery::getMasteryValue)
            .orElse(BigDecimal.ZERO)
            .setScale(3, RoundingMode.HALF_UP);

        BigDecimal normalizedScore = BigDecimal.valueOf(score)
            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        BigDecimal after = before.multiply(BigDecimal.valueOf(0.7))
            .add(normalizedScore.multiply(BigDecimal.valueOf(0.3)));
        after = clamp(after).setScale(3, RoundingMode.HALF_UP);

        Mastery mastery = new Mastery();
        mastery.setUserId(userId);
        mastery.setNodeId(nodeId);
        mastery.setNodeName(nodeName);
        mastery.setMasteryValue(after);
        masteryRepository.upsert(mastery);

        BigDecimal delta = after.subtract(before).setScale(3, RoundingMode.HALF_UP);
        return new MasteryUpdateResult(before, delta, after);
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    public record MasteryUpdateResult(
        BigDecimal masteryBefore,
        BigDecimal masteryDelta,
        BigDecimal masteryAfter
    ) {
    }
}


