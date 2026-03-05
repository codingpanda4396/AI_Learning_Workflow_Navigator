package com.pandanav.learning.api.controller;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DbDebugController {

    private final Flyway flyway;
    private final JdbcTemplate jdbcTemplate;

    public DbDebugController(Flyway flyway, JdbcTemplate jdbcTemplate) {
        this.flyway = flyway;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/db")
    public Map<String, Object> db() {
        Integer selectOne = jdbcTemplate.queryForObject("select 1", Integer.class);

        List<Map<String, Object>> migrations = Arrays.stream(flyway.info().all())
            .map(this::toMigrationInfo)
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("db", "ok");
        result.put("select1", selectOne);
        result.put("migrationCount", migrations.size());
        result.put("migrations", migrations);
        return result;
    }

    private Map<String, Object> toMigrationInfo(MigrationInfo info) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("version", info.getVersion() == null ? null : info.getVersion().getVersion());
        map.put("description", info.getDescription());
        map.put("script", info.getScript());
        map.put("state", info.getState().getDisplayName());
        return map;
    }
}
