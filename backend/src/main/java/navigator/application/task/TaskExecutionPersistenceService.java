package navigator.application.task;

import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.TaskScaffold;
import navigator.infrastructure.persistence.entity.TaskExecutionRuntimeEntity;
import navigator.infrastructure.persistence.entity.TaskMessageEntity;
import navigator.infrastructure.persistence.entity.TaskStateTransitionEntity;
import navigator.infrastructure.persistence.repository.TaskExecutionRuntimeRepository;
import navigator.infrastructure.persistence.repository.TaskMessageRepository;
import navigator.infrastructure.persistence.repository.TaskStateTransitionRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TaskExecutionPersistenceService {

    private final TaskExecutionRuntimeRepository runtimeRepository;
    private final TaskStateTransitionRepository transitionRepository;
    private final TaskMessageRepository messageRepository;
    private final JsonSerde jsonSerde;

    public TaskExecutionPersistenceService(TaskExecutionRuntimeRepository runtimeRepository,
                                          TaskStateTransitionRepository transitionRepository,
                                          TaskMessageRepository messageRepository,
                                          JsonSerde jsonSerde) {
        this.runtimeRepository = runtimeRepository;
        this.transitionRepository = transitionRepository;
        this.messageRepository = messageRepository;
        this.jsonSerde = jsonSerde;
    }

    public TaskExecutionRuntime loadRuntime(String sessionKey, String taskCode) {
        TaskExecutionRuntimeEntity entity = runtimeRepository.findBySessionKeyAndTaskCode(sessionKey, taskCode);
        if (entity == null) {
            return null;
        }
        TaskExecutionRuntime rt = new TaskExecutionRuntime();
        rt.setExploreTurnCount(entity.getExploreTurnCount() != null ? entity.getExploreTurnCount() : 0);
        rt.setCheckpointQuestion(entity.getCheckpointQuestion());
        rt.setSelfExplanationEvaluation(entity.getSelfExplanationEvaluation());
        if (entity.getCurrentState() != null) {
            try {
                rt.setState(TaskExecutionState.valueOf(entity.getCurrentState()));
            } catch (IllegalArgumentException ignored) {
                rt.setState(TaskExecutionState.INIT);
            }
        }
        TaskScaffold scaffold = jsonSerde.fromJson(entity.getScaffoldJson(), TaskScaffold.class);
        rt.setScaffold(scaffold);
        // actionHistory is stored as string array; keep safe parsing
        String[] actionNames = jsonSerde.fromJson(entity.getActionHistoryJson(), String[].class);
        if (actionNames != null) {
            for (String n : actionNames) {
                try {
                    rt.getActionHistory().add(navigator.domain.enums.LearningActionType.valueOf(n));
                } catch (Exception ignored) {
                }
            }
        }
        return rt;
    }

    public void saveRuntime(String sessionKey, String taskCode, TaskExecutionRuntime rt, Long sessionIdNumeric, Long taskIdNumeric) {
        if (sessionKey == null || taskCode == null || rt == null || rt.getScaffold() == null) {
            return;
        }
        TaskExecutionRuntimeEntity entity = new TaskExecutionRuntimeEntity();
        entity.setSessionKey(sessionKey);
        entity.setSessionId(sessionIdNumeric);
        entity.setTaskId(taskIdNumeric);
        entity.setTaskCode(taskCode);
        entity.setScaffoldId(rt.getScaffold().getScaffoldId());
        entity.setScaffoldJson(jsonSerde.toJson(rt.getScaffold()));
        entity.setCurrentState(rt.getState() != null ? rt.getState().name() : TaskExecutionState.INIT.name());
        entity.setExploreTurnCount(rt.getExploreTurnCount());
        entity.setCheckpointQuestion(rt.getCheckpointQuestion());
        entity.setSelfExplanationEvaluation(rt.getSelfExplanationEvaluation());
        entity.setActionHistoryJson(jsonSerde.toJson(rt.getActionHistory().stream().map(Enum::name).toArray(String[]::new)));
        runtimeRepository.saveOrUpdate(entity);
    }

    public void appendTransition(String sessionKey, String taskCode, TaskExecutionState from, TaskExecutionState to, String reason) {
        if (sessionKey == null || taskCode == null || from == null || to == null) {
            return;
        }
        TaskStateTransitionEntity e = new TaskStateTransitionEntity();
        e.setSessionKey(sessionKey);
        e.setTaskCode(taskCode);
        e.setFromState(from.name());
        e.setToState(to.name());
        e.setReason(reason);
        e.setCreatedAt(LocalDateTime.now());
        transitionRepository.save(e);
    }

    public void appendMessage(String sessionKey, String taskCode, String role, String content,
                              String detectedAction, TaskExecutionState stateBefore, TaskExecutionState stateAfter,
                              String fallbackMode) {
        if (sessionKey == null || taskCode == null || role == null || content == null) {
            return;
        }
        TaskMessageEntity e = new TaskMessageEntity();
        e.setSessionKey(sessionKey);
        e.setTaskCode(taskCode);
        e.setRole(role);
        e.setContent(content);
        e.setDetectedAction(detectedAction);
        e.setStateBefore(stateBefore != null ? stateBefore.name() : null);
        e.setStateAfter(stateAfter != null ? stateAfter.name() : null);
        e.setFallbackMode(fallbackMode != null ? fallbackMode : "NONE");
        e.setCreatedAt(LocalDateTime.now());
        messageRepository.save(e);
    }

    public List<TaskMessageEntity> findRecentMessagesAscending(String sessionKey, String taskCode, int limit) {
        List<TaskMessageEntity> desc = messageRepository.findRecentBySessionKeyAndTaskCode(sessionKey, taskCode, limit);
        if (desc == null || desc.isEmpty()) {
            return List.of();
        }
        List<TaskMessageEntity> asc = new ArrayList<>(desc);
        asc.sort(Comparator.comparing(TaskMessageEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return asc;
    }
}

