package com.globalsearch.annotation;

import com.globalsearch.config.ElasticsearchTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.annotation.*;

/**
 * Composite annotation for Elasticsearch integration tests
 * Automatically configures Testcontainers Elasticsearch for tests
 *
 * Usage:
 * @ElasticsearchTest
 * class MyElasticsearchTest {
 *     // Your tests here
 * }
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(ElasticsearchTestConfig.class)
public @interface ElasticsearchTest {
}
