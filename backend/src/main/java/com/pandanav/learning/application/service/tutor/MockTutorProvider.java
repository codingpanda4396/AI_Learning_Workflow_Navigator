package com.pandanav.learning.application.service.tutor;

public class MockTutorProvider implements TutorProvider {

    @Override
    public TutorProviderReply generateReply(TutorProviderRequest request) {
        String question = request.userMessage() == null ? "" : request.userMessage().trim();
        if (question.isEmpty()) {
            return new TutorProviderReply(
                "你可以先描述你卡住的点，我会先用 2-3 句话解释核心概念，再给一个最小练习。",
                "mock-tutor",
                "placeholder-v1"
            );
        }

        String content;
        if (question.contains("链式法则")) {
            content = """
                链式法则可以理解为“外层求导 × 内层求导”。
                1) 先把复合函数看成 f(g(x))，先对外层 f 求导并保留 g(x)。
                2) 再乘以内层 g(x) 对 x 的导数。
                3) 你可以先练习 y=(3x+1)^5，按这个三步写一遍，我再帮你检查。
                """;
        } else {
            content = """
                我先给你一个最小学习路径：
                1) 先说出你当前理解的一句话版本；
                2) 我会指出关键漏洞；
                3) 再给你一道对应难度的小题。
                你可以先回答：你觉得这题真正考察的核心点是什么？
                """;
        }

        return new TutorProviderReply(content.trim(), "mock-tutor", "placeholder-v1");
    }
}
