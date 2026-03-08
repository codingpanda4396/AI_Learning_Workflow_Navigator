package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.AppUser;

import java.util.Optional;

public interface AppUserRepository {

    AppUser save(AppUser user);

    Optional<AppUser> findByUsername(String username);

    Optional<AppUser> findById(Long userId);

    void updateLastLoginAt(Long userId);
}
