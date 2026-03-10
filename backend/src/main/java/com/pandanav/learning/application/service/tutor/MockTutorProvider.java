package com.pandanav.learning.application.service.tutor;

public class MockTutorProvider implements TutorProvider {

    @Override
    public TutorProviderReply generateReply(TutorProviderRequest request) {
        String question = request.userMessage() == null ? "" : request.userMessage().trim();
        if (question.isEmpty()) {
            return new TutorProviderReply(
                "你可以先描述你卡住的步骤，我会先提一个引导问题，再给最小提示。",
                "mock-tutor",
                "placeholder-v1"
            );
        }

        String content;
        if (question.contains("链式法则")) {
            content = "先判断外层和内层函数分别是什么？写出 f(g(x)) 后，先对外层求导再乘以内层导数。";
        } else {
            content = "先说说你认为这题最关键的一步是什么？我先判断你的卡点，再给你提示。";
        }

        return new TutorProviderReply(content, "mock-tutor", "placeholder-v1");
    }
}
