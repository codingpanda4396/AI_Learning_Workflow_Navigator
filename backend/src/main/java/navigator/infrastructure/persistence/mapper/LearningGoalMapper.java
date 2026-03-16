package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.LearningGoalEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningGoalMapper extends BaseMapper<LearningGoalEntity> {
}

