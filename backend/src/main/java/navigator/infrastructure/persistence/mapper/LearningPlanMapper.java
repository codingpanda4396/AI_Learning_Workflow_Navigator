package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningPlanMapper extends BaseMapper<LearningPlanEntity> {
}

