package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.CreateDiagnosisSessionRequest;
import navigator.api.dto.DiagnosisSessionData;
import navigator.api.dto.SubmitDiagnosisData;
import navigator.api.dto.SubmitDiagnosisRequest;
import navigator.application.DiagnosisApplicationService;
import navigator.domain.model.DiagnosisSubmission;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    private final DiagnosisApplicationService diagnosisService;

    public DiagnosisController(DiagnosisApplicationService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping("/sessions")
    public GlobalResponse<DiagnosisSessionData> createSession(@Valid @RequestBody CreateDiagnosisSessionRequest request) {
        DiagnosisSessionData data = diagnosisService.createSession(request.getGoalId());
        return GlobalResponse.ok(data);
    }

    @PostMapping("/submissions")
    public GlobalResponse<SubmitDiagnosisData> submit(@Valid @RequestBody SubmitDiagnosisRequest request) {
        DiagnosisSubmission submission = DiagnosisSubmission.builder()
                .diagnosisId(request.getDiagnosisId())
                .answers(request.getAnswers())
                .build();
        SubmitDiagnosisData data = diagnosisService.submit(submission);
        return GlobalResponse.ok(data);
    }
}
