package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMessageMapper extends BaseMapper<TaskMessageEntity> {
}

