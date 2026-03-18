package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskStateTransitionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskStateTransitionMapper extends BaseMapper<TaskStateTransitionEntity> {
}

