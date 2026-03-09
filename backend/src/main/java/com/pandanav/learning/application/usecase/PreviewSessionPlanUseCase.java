package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.PlanPreviewResponse;
import com.pandanav.learning.domain.enums.PlanMode;

public interface PreviewSessionPlanUseCase {

    PlanPreviewResponse preview(Long sessionId, PlanMode mode);
}
