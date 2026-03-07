package com.pandanav.learning.application.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NextActionPolicyService {

    public NextAction decide(Integer score, List<String> errorTags) {
        if (score == null) {
            return NextAction.NOOP;
        }
        if (score < 60) {
            return NextAction.INSERT_REMEDIAL_UNDERSTANDING;
        }
        if (score < 80) {
            return NextAction.INSERT_TRAINING_VARIANTS;
        }
        if (score < 90) {
            return NextAction.INSERT_TRAINING_REINFORCEMENT;
        }
        if (score <= 100) {
            return NextAction.ADVANCE_TO_NEXT_NODE;
        }
        return NextAction.NOOP;
    }

    public enum NextAction {
        INSERT_REMEDIAL_UNDERSTANDING,
        INSERT_TRAINING_VARIANTS,
        INSERT_TRAINING_REINFORCEMENT,
        ADVANCE_TO_NEXT_NODE,
        NOOP
    }
}
