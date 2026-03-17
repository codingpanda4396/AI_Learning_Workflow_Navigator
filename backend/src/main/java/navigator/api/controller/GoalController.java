package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.CreateGoalData;
import navigator.api.dto.CreateGoalRequest;
import navigator.application.GoalApplicationService;
import navigator.domain.model.LearningGoalInput;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalApplicationService goalService;

    public GoalController(GoalApplicationService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public GlobalResponse<CreateGoalData> createGoal(@Valid @RequestBody CreateGoalRequest request) {
        LearningGoalInput input = LearningGoalInput.builder()
                .rawGoalText(request.getRawGoalText())
                .timeBudget(request.getTimeBudget())
                .selfReportedLevel(request.getSelfReportedLevel())
                .preferenceTags(request.getPreferenceTags())
                .goalTypeHint(request.getGoalTypeHint())
                .subjectHint(request.getSubjectHint())
                .topicHints(request.getTopicHints())
                .sourceContext(request.getSourceContext())
                .priorityModule(request.getPriorityModule())
                .build();
        CreateGoalData data = goalService.createGoal(input);
        return GlobalResponse.ok(data);
    }
}
