package navigator.application.scaffold;

import navigator.api.dto.scaffold.TrainingFeedback;

/**
 * TRAINING 阶段：结构化评估（不直接生成面向用户的 Tutor 长文）。
 */
public interface TrainingEvaluator {

    TrainingFeedback evaluate(StructureValidationContext ctx);
}
