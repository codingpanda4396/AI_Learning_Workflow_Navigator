package com.pandanav.learning.application.service.tutor;

import java.util.function.Consumer;

public interface TutorProvider {

    TutorProviderReply generateReply(TutorProviderRequest request);

    default TutorProviderReply streamReply(TutorProviderRequest request, Consumer<String> onDelta) {
        TutorProviderReply reply = generateReply(request);
        if (reply != null && reply.content() != null && onDelta != null) {
            onDelta.accept(reply.content());
        }
        return reply;
    }
}
