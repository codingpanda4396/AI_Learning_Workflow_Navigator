package com.panda.ainavigator;

import com.pandanav.learning.LearningApplication;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(
    classes = LearningApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
    }
)
class AiLearningWorkflowNavigatorApplicationTests {

    @MockBean
    private Flyway flyway;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }
}
