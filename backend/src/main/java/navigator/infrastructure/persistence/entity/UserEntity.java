package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class UserEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
