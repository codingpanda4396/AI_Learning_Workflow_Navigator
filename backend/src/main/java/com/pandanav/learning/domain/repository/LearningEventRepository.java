package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LearningEvent;

public interface LearningEventRepository {

    LearningEvent save(LearningEvent event);
}
