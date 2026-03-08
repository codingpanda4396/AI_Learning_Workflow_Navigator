package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.LlmCallLog;

public interface LlmCallLogRepository {

    void save(LlmCallLog log);
}

