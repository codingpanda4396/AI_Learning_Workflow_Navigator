package navigator.application.llm;

import org.springframework.stereotype.Component;

/**
 * 无外部依赖的 LLM 占位实现，保证主链路可运行。
 */
@Component
public class MockLlmGateway implements LlmGateway {

    @Override
    @SuppressWarnings("unused")
    public String generateReply(String systemHint, String userContent) {
        // 不向用户回显 system 提示（其中含 STAGE_NAME 等内部字段），避免执行页出现「调试台」文案
        String u = userContent != null ? userContent.trim() : "";
        String hook = u.isEmpty()
                ? "先说说你现在卡在哪一小步，或贴一句你的理解。"
                : "我听到你在说：「" + truncate(u, 48) + "」。我们先抓住一个最小例子，对照任务目标看是否覆盖要点；你可以再追问一个更细的点。";
        return "我们一起看这个例子：你可以先说说你现在的理解，我帮你一起梳理。\n\n" + hook;
    }

    private static String truncate(String s, int n) {
        if (s == null) return "";
        return s.length() <= n ? s : s.substring(0, n) + "…";
    }
}
