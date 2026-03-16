package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.NextActionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmNextActionRequest {
    private NextActionType actionType;
}
