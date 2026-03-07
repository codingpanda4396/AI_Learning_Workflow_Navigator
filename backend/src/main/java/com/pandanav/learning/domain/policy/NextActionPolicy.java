package com.pandanav.learning.domain.policy;

import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.ErrorTag;

import java.util.Set;

public interface NextActionPolicy {

    NextAction decide(int score, Set<ErrorTag> errorTags);
}


