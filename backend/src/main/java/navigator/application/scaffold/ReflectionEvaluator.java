package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;

/**
 * DFS/BFS 反思阶段：规则校验，不生成用户答案。
 */
public interface ReflectionEvaluator {

    ValidationResult validate(StructureValidationContext ctx);
}
