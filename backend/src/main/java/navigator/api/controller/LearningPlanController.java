package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.CommitLearningPlanRequest;
import navigator.api.dto.CommitPlanData;
import navigator.api.dto.PlanPreviewData;
import navigator.api.dto.PreviewLearningPlanRequest;
import navigator.application.PlanningApplicationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/learning-plans")
public class LearningPlanController {

    private final PlanningApplicationService planningService;

    public LearningPlanController(PlanningApplicationService planningService) {
        this.planningService = planningService;
    }

    @PostMapping("/preview")
    public GlobalResponse<PlanPreviewData> preview(@Valid @RequestBody PreviewLearningPlanRequest request) {
        PlanPreviewData data = planningService.preview(request.getGoalId(), request.getDiagnosisId());
        return GlobalResponse.ok(data);
    }

    @PostMapping("/commit")
    public GlobalResponse<CommitPlanData> commit(@Valid @RequestBody CommitLearningPlanRequest request) {
        CommitPlanData data = planningService.commit(request.getPlanId());
        return GlobalResponse.ok(data);
    }
}
