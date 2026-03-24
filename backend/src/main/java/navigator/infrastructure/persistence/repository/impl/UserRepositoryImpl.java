package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import navigator.infrastructure.persistence.entity.UserEntity;
import navigator.infrastructure.persistence.mapper.UserMapper;
import navigator.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper mapper;

    public UserRepositoryImpl(UserMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserEntity save(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public UserEntity findById(Long id) {
        return id == null ? null : mapper.selectById(id);
    }

    @Override
    public UserEntity findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<UserEntity>().eq("username", username));
    }
}
