package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiagnosisSessionMapper extends BaseMapper<DiagnosisSessionEntity> {
}

