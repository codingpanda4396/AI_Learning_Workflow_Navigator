package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.UserSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSessionMapper extends BaseMapper<UserSessionEntity> {
}
