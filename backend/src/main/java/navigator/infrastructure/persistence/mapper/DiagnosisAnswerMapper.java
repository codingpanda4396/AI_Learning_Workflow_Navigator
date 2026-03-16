package navigator.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import navigator.infrastructure.persistence.entity.DiagnosisAnswerEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DiagnosisAnswerMapper extends BaseMapper<DiagnosisAnswerEntity> {
}

