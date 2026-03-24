package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.UserEntity;

public interface UserRepository {

    UserEntity save(UserEntity entity);

    UserEntity findById(Long id);

    UserEntity findByUsername(String username);
}
