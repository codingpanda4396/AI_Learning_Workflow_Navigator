package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearningSessionMapper extends BaseMapper<LearningSessionEntity> {
}

