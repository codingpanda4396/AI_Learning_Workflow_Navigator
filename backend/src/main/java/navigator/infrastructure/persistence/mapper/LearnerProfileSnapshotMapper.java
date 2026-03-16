package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.LearnerProfileSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LearnerProfileSnapshotMapper extends BaseMapper<LearnerProfileSnapshotEntity> {
}

