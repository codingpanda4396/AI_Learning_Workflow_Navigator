package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SessionTaskMapper extends BaseMapper<SessionTaskEntity> {
}

