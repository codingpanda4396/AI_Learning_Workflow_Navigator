package com.pandanav.learning.application.command;

public record StartSessionCommand(
    String userId,
    String courseId,
    String chapterId,
    String goalText
) {
}
