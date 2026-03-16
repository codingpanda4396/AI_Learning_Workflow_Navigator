package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskCompletionMapper extends BaseMapper<TaskCompletionEntity> {
}

