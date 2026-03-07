package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.domain.policy.NextActionPolicy;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.service.DefaultTaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.service.ScoreBasedNextActionPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainPolicyConfig {

    @Bean
    public NextActionPolicy nextActionPolicy() {
        return new ScoreBasedNextActionPolicy();
    }

    @Bean
    public TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy() {
        return new DefaultTaskObjectiveTemplateStrategy();
    }
}


