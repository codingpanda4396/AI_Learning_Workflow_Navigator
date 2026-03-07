package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.ErrorTag;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.policy.NextActionPolicy;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NextActionPolicyService {

    private final NextActionPolicy nextActionPolicy;

    public NextActionPolicyService(NextActionPolicy nextActionPolicy) {
        this.nextActionPolicy = nextActionPolicy;
    }

    public NextAction decide(Integer score, List<ErrorTag> errorTags) {
        if (score == null) {
            return NextAction.NOOP;
        }
        Set<ErrorTag> tags = errorTags == null ? Set.of() : new HashSet<>(errorTags);
        return nextActionPolicy.decide(score, tags);
    }
}

