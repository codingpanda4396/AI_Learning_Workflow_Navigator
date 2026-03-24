package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_session")
public class UserSessionEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private String tokenHash;
    private LocalDateTime expiresAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime createdAt;
}
