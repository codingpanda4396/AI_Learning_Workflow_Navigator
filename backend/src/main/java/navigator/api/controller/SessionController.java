package navigator.api.controller;

import navigator.api.GlobalResponse;
import navigator.api.dto.ConfirmNextActionRequest;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.NextActionConfirmData;
import navigator.api.dto.ReportData;
import navigator.application.ReportApplicationService;
import navigator.application.ExecutionApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final ExecutionApplicationService executionService;
    private final ReportApplicationService reportService;

    public SessionController(ExecutionApplicationService executionService, ReportApplicationService reportService) {
        this.executionService = executionService;
        this.reportService = reportService;
    }

    @GetMapping("/{sessionId}/current-task")
    public GlobalResponse<CurrentTaskData> getCurrentTask(@PathVariable String sessionId) {
        CurrentTaskData data = executionService.getCurrentTask(sessionId);
        if (data == null) return GlobalResponse.notFound("session or current task not found");
        return GlobalResponse.ok(data);
    }

    @GetMapping("/{sessionId}/report")
    public GlobalResponse<ReportData> getReport(@PathVariable String sessionId) {
        ReportData data = reportService.getReport(sessionId);
        return GlobalResponse.ok(data);
    }

    @PostMapping("/{sessionId}/next-action")
    public GlobalResponse<NextActionConfirmData> confirmNextAction(
            @PathVariable String sessionId,
            @RequestBody ConfirmNextActionRequest request) {
        NextActionConfirmData data = reportService.confirmNextAction(sessionId, request.getActionType());
        return GlobalResponse.ok(data);
    }
}
