package navigator.application.llm;

/**
 * LLM 网关抽象；Sprint 3 默认使用 Mock/模板实现。
 */
public interface LlmGateway {

    /**
     * @param systemHint 系统侧约束摘要
     * @param userContent 用户输入
     * @return 助手回复正文
     */
    String generateReply(String systemHint, String userContent);

    /**
     * 脚手架 JSON 生成专用：使用更高的 max_tokens 避免 JSON 截断。
     * 默认实现委托到 {@link #generateReply}。
     */
    default String generateScaffoldReply(String systemHint, String userContent) {
        return generateReply(systemHint, userContent);
    }
}
