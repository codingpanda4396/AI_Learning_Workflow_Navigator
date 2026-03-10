package com.pandanav.learning.infrastructure.config;

import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmProfileConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {

    private boolean enabled;
    private String provider = "openai-compatible";
    private String baseUrl;
    private String apiKey;
    private String model = "qwen3.5-plus";
    private int timeoutMs = 20000;
    private int maxRetries = 0;
    private int retryBackoffMs = 200;
    private int structureMaxOutputTokens = 120;
    private int understandingMaxOutputTokens = 160;
    private int trainingMaxOutputTokens = 220;
    private int reflectionMaxOutputTokens = 180;
    private int evaluationMaxOutputTokens = 180;
    private int practiceGenerationMaxOutputTokens = 260;
    private int conceptDecomposeMaxOutputTokens = 180;
    private int pathPlanMaxOutputTokens = 300;
    private int tutorMaxOutputTokens = 450;
    private boolean fallbackToRule = true;
    private boolean logRequest = false;
    private boolean logResponse = false;
    private Profiles profiles = new Profiles();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
    }

    public int getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(int retryBackoffMs) {
        this.retryBackoffMs = Math.max(0, retryBackoffMs);
    }

    public int getStructureMaxOutputTokens() {
        return structureMaxOutputTokens;
    }

    public void setStructureMaxOutputTokens(int structureMaxOutputTokens) {
        this.structureMaxOutputTokens = Math.max(1, structureMaxOutputTokens);
    }

    public int getUnderstandingMaxOutputTokens() {
        return understandingMaxOutputTokens;
    }

    public void setUnderstandingMaxOutputTokens(int understandingMaxOutputTokens) {
        this.understandingMaxOutputTokens = Math.max(1, understandingMaxOutputTokens);
    }

    public int getTrainingMaxOutputTokens() {
        return trainingMaxOutputTokens;
    }

    public void setTrainingMaxOutputTokens(int trainingMaxOutputTokens) {
        this.trainingMaxOutputTokens = Math.max(1, trainingMaxOutputTokens);
    }

    public int getReflectionMaxOutputTokens() {
        return reflectionMaxOutputTokens;
    }

    public void setReflectionMaxOutputTokens(int reflectionMaxOutputTokens) {
        this.reflectionMaxOutputTokens = Math.max(1, reflectionMaxOutputTokens);
    }

    public int getEvaluationMaxOutputTokens() {
        return evaluationMaxOutputTokens;
    }

    public void setEvaluationMaxOutputTokens(int evaluationMaxOutputTokens) {
        this.evaluationMaxOutputTokens = Math.max(1, evaluationMaxOutputTokens);
    }

    public int getPracticeGenerationMaxOutputTokens() {
        return practiceGenerationMaxOutputTokens;
    }

    public void setPracticeGenerationMaxOutputTokens(int practiceGenerationMaxOutputTokens) {
        this.practiceGenerationMaxOutputTokens = Math.max(1, practiceGenerationMaxOutputTokens);
    }

    public int getConceptDecomposeMaxOutputTokens() {
        return conceptDecomposeMaxOutputTokens;
    }

    public void setConceptDecomposeMaxOutputTokens(int conceptDecomposeMaxOutputTokens) {
        this.conceptDecomposeMaxOutputTokens = Math.max(1, conceptDecomposeMaxOutputTokens);
    }

    public int getPathPlanMaxOutputTokens() {
        return pathPlanMaxOutputTokens;
    }

    public void setPathPlanMaxOutputTokens(int pathPlanMaxOutputTokens) {
        this.pathPlanMaxOutputTokens = Math.max(1, pathPlanMaxOutputTokens);
    }

    public int getTutorMaxOutputTokens() {
        return tutorMaxOutputTokens;
    }

    public void setTutorMaxOutputTokens(int tutorMaxOutputTokens) {
        this.tutorMaxOutputTokens = Math.max(1, tutorMaxOutputTokens);
    }

    public boolean isFallbackToRule() {
        return fallbackToRule;
    }

    public void setFallbackToRule(boolean fallbackToRule) {
        this.fallbackToRule = fallbackToRule;
    }

    public boolean isLogRequest() {
        return logRequest;
    }

    public void setLogRequest(boolean logRequest) {
        this.logRequest = logRequest;
    }

    public boolean isLogResponse() {
        return logResponse;
    }

    public void setLogResponse(boolean logResponse) {
        this.logResponse = logResponse;
    }

    public Profiles getProfiles() {
        return profiles;
    }

    public void setProfiles(Profiles profiles) {
        this.profiles = profiles == null ? new Profiles() : profiles;
    }

    public boolean isReady() {
        return enabled
            && baseUrl != null && !baseUrl.isBlank()
            && apiKey != null && !apiKey.isBlank()
            && model != null && !model.isBlank();
    }

    public LlmProfileConfig resolveProfile(LlmInvocationProfile profile, String promptKey) {
        ProfileSettings settings = switch (profile) {
            case LIGHT_JSON_TASK -> profiles.lightJsonTask;
            case HEAVY_REASONING_TASK -> profiles.heavyReasoningTask;
            case CHAT_TASK -> profiles.chatTask;
        };
        return new LlmProfileConfig(
            profile,
            provider,
            settings.model == null || settings.model.isBlank() ? model : settings.model.trim(),
            settings.timeoutMs,
            resolveMaxOutputTokens(promptKey, settings.maxTokens),
            settings.temperature,
            settings.jsonResponse,
            settings.fallbackAllowed,
            settings.streamAllowed,
            settings.completionWarningThreshold,
            settings.toExtraParams()
        );
    }

    public Integer resolveMaxOutputTokens(String promptKey, Integer fallbackMaxTokens) {
        if (promptKey == null || promptKey.isBlank()) {
            return fallbackMaxTokens;
        }
        return switch (promptKey) {
            case "STRUCTURE" -> structureMaxOutputTokens;
            case "UNDERSTANDING" -> understandingMaxOutputTokens;
            case "TRAINING" -> trainingMaxOutputTokens;
            case "REFLECTION" -> reflectionMaxOutputTokens;
            case "EVALUATE" -> evaluationMaxOutputTokens;
            case "PRACTICE_GENERATION" -> practiceGenerationMaxOutputTokens;
            case "CONCEPT_DECOMPOSE" -> conceptDecomposeMaxOutputTokens;
            case "PATH_PLAN" -> pathPlanMaxOutputTokens;
            case "TUTOR" -> tutorMaxOutputTokens;
            default -> fallbackMaxTokens;
        };
    }

    public static class Profiles {
        private ProfileSettings lightJsonTask = new ProfileSettings("qwen3.5-plus", 10000, 260, 0.0, true, true, false, 400, false, 0);
        private ProfileSettings heavyReasoningTask = new ProfileSettings("qwen3.5-plus", 20000, 300, 0.2, true, true, false, 800, false, 0);
        private ProfileSettings chatTask = new ProfileSettings("qwen3.5-plus", 15000, 450, 0.2, false, true, true, 600, false, 0);

        public ProfileSettings getLightJsonTask() {
            return lightJsonTask;
        }

        public void setLightJsonTask(ProfileSettings lightJsonTask) {
            this.lightJsonTask = lightJsonTask == null ? this.lightJsonTask : lightJsonTask;
        }

        public ProfileSettings getHeavyReasoningTask() {
            return heavyReasoningTask;
        }

        public void setHeavyReasoningTask(ProfileSettings heavyReasoningTask) {
            this.heavyReasoningTask = heavyReasoningTask == null ? this.heavyReasoningTask : heavyReasoningTask;
        }

        public ProfileSettings getChatTask() {
            return chatTask;
        }

        public void setChatTask(ProfileSettings chatTask) {
            this.chatTask = chatTask == null ? this.chatTask : chatTask;
        }
    }

    public static class ProfileSettings {
        private String model;
        private Integer timeoutMs;
        private Integer maxTokens;
        private Double temperature;
        private boolean jsonResponse;
        private boolean fallbackAllowed;
        private boolean streamAllowed;
        private Integer completionWarningThreshold;
        private boolean disableThinking;
        private Integer thinkingBudget;

        public ProfileSettings() {
        }

        public ProfileSettings(
            String model,
            Integer timeoutMs,
            Integer maxTokens,
            Double temperature,
            boolean jsonResponse,
            boolean fallbackAllowed,
            boolean streamAllowed,
            Integer completionWarningThreshold,
            boolean disableThinking,
            Integer thinkingBudget
        ) {
            this.model = model;
            this.timeoutMs = timeoutMs;
            this.maxTokens = maxTokens;
            this.temperature = temperature;
            this.jsonResponse = jsonResponse;
            this.fallbackAllowed = fallbackAllowed;
            this.streamAllowed = streamAllowed;
            this.completionWarningThreshold = completionWarningThreshold;
            this.disableThinking = disableThinking;
            this.thinkingBudget = thinkingBudget;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Integer getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Integer timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public boolean isJsonResponse() {
            return jsonResponse;
        }

        public void setJsonResponse(boolean jsonResponse) {
            this.jsonResponse = jsonResponse;
        }

        public boolean isFallbackAllowed() {
            return fallbackAllowed;
        }

        public void setFallbackAllowed(boolean fallbackAllowed) {
            this.fallbackAllowed = fallbackAllowed;
        }

        public boolean isStreamAllowed() {
            return streamAllowed;
        }

        public void setStreamAllowed(boolean streamAllowed) {
            this.streamAllowed = streamAllowed;
        }

        public Integer getCompletionWarningThreshold() {
            return completionWarningThreshold;
        }

        public void setCompletionWarningThreshold(Integer completionWarningThreshold) {
            this.completionWarningThreshold = completionWarningThreshold;
        }

        public boolean isDisableThinking() {
            return disableThinking;
        }

        public void setDisableThinking(boolean disableThinking) {
            this.disableThinking = disableThinking;
        }

        public Integer getThinkingBudget() {
            return thinkingBudget;
        }

        public void setThinkingBudget(Integer thinkingBudget) {
            this.thinkingBudget = thinkingBudget;
        }

        Map<String, Object> toExtraParams() {
            Map<String, Object> params = new LinkedHashMap<>();
            if (disableThinking) {
                params.put("enable_thinking", false);
            }
            if (thinkingBudget != null && thinkingBudget >= 0) {
                params.put("thinking_budget", thinkingBudget);
            }
            return params;
        }
    }
}
