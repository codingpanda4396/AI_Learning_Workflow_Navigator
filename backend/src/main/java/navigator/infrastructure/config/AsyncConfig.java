package navigator.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * R0003：explain 预取 / 缓存回填异步执行，避免阻塞 HTTP 线程。
 */
@Configuration
public class AsyncConfig {

    public static final String AI_TUTOR_EXECUTOR = "aiTutorExecutor";

    @Bean(name = AI_TUTOR_EXECUTOR)
    public Executor aiTutorExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("ai-tutor-");
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(200);
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setAwaitTerminationSeconds(10);
        ex.initialize();
        return ex;
    }
}
