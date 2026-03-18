package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskCheckpointResultEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskCheckpointResultMapper extends BaseMapper<TaskCheckpointResultEntity> {
}

