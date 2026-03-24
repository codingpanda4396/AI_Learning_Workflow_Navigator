package navigator.api;

import navigator.api.auth.AuthInterceptor;
import navigator.infrastructure.config.AsyncConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(
            AuthInterceptor authInterceptor,
            @Qualifier(AsyncConfig.AI_TUTOR_EXECUTOR) java.util.concurrent.Executor aiTutorExecutor) {
        AsyncTaskExecutor mvcAsyncExecutor = adaptAsyncExecutor(aiTutorExecutor);
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(authInterceptor).addPathPatterns("/api/**");
            }

            @Override
            public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
                configurer.setTaskExecutor(mvcAsyncExecutor);
            }
        };
    }

    private static AsyncTaskExecutor adaptAsyncExecutor(java.util.concurrent.Executor executor) {
        if (executor instanceof AsyncTaskExecutor asyncTaskExecutor) {
            return asyncTaskExecutor;
        }
        ThreadPoolTaskExecutor adapter = new ThreadPoolTaskExecutor();
        adapter.setThreadNamePrefix("mvc-sse-");
        adapter.setCorePoolSize(2);
        adapter.setMaxPoolSize(8);
        adapter.setQueueCapacity(200);
        adapter.setWaitForTasksToCompleteOnShutdown(true);
        adapter.setAwaitTerminationSeconds(10);
        adapter.initialize();
        return adapter;
    }
}
