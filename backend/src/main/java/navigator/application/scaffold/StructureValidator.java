package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;

/**
 * STRUCTURE 阶段规则校验（关键词越界 + 最短表达）。
 */
public interface StructureValidator {

    ValidationResult validate(StructureValidationContext ctx);
}
