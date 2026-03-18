package navigator.application.llm;

import org.springframework.stereotype.Component;

/**
 * 无外部依赖的 LLM 占位实现，保证主链路可运行。
 */
@Component
public class MockLlmGateway implements LlmGateway {

    @Override
    public String generateReply(String systemHint, String userContent) {
        return "【导师】" + briefHint(systemHint) + "\n\n针对你的问题，建议先抓住一个最小场景：把概念套进一个具体例子，再对照任务目标检查是否覆盖要点。你可以继续追问一个更细的点。";
    }

    private static String briefHint(String h) {
        if (h == null || h.isBlank()) return "";
        return h.length() > 80 ? h.substring(0, 80) + "…" : h;
    }
}
