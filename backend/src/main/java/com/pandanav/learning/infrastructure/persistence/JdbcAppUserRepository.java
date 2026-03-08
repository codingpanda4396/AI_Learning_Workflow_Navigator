package com.pandanav.learning.infrastructure.persistence;

import com.pandanav.learning.domain.model.AppUser;
import com.pandanav.learning.domain.repository.AppUserRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class JdbcAppUserRepository implements AppUserRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAppUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AppUser save(AppUser user) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO app_user (username, password_hash)
                    VALUES (?, ?)
                    """,
                new String[]{"id"}
            );
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key != null) {
            user.setId(key.longValue());
        }
        return user;
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                    SELECT id, username, password_hash, created_at, last_login_at, status
                    FROM app_user
                    WHERE username = ?
                    """,
                (rs, rowNum) -> mapUser(rs),
                username
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AppUser> findById(Long userId) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                """
                    SELECT id, username, password_hash, created_at, last_login_at, status
                    FROM app_user
                    WHERE id = ?
                    """,
                (rs, rowNum) -> mapUser(rs),
                userId
            ));
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    @Override
    public void updateLastLoginAt(Long userId) {
        jdbcTemplate.update(
            """
                UPDATE app_user
                SET last_login_at = now()
                WHERE id = ?
                """,
            userId
        );
    }

    private AppUser mapUser(java.sql.ResultSet rs) throws java.sql.SQLException {
        AppUser user = new AppUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setCreatedAt(rs.getObject("created_at", java.time.OffsetDateTime.class));
        user.setLastLoginAt(rs.getObject("last_login_at", java.time.OffsetDateTime.class));
        user.setStatus(rs.getString("status"));
        return user;
    }
}
