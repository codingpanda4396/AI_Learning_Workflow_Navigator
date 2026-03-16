package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.TaskInteractionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskInteractionMapper extends BaseMapper<TaskInteractionEntity> {
}

