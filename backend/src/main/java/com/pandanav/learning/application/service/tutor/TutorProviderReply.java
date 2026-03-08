package com.pandanav.learning.application.service.tutor;

public record TutorProviderReply(
    String content,
    String provider,
    String model
) {
}
