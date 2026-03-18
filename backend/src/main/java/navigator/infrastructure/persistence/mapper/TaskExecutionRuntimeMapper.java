package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskExecutionRuntimeEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskExecutionRuntimeMapper extends BaseMapper<TaskExecutionRuntimeEntity> {
}

