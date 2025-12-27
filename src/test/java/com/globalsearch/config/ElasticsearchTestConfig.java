package com.globalsearch.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test configuration for Elasticsearch using Testcontainers
 * This provides a real Elasticsearch instance for integration tests
 */
@TestConfiguration
public class ElasticsearchTestConfig extends ElasticsearchConfiguration {

    private static final String ELASTICSEARCH_VERSION = "8.13.2";
    private static final DockerImageName ELASTICSEARCH_IMAGE =
            DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch")
                    .withTag(ELASTICSEARCH_VERSION);

    /**
     * Shared Elasticsearch container instance
     * Static to ensure it's created only once per test run
     */
    private static final ElasticsearchContainer elasticsearchContainer;

    static {
        // Configure testcontainers for Docker socket access
        System.setProperty("testcontainers.docker.socket.override", "/var/run/docker.sock");
        System.setProperty("testcontainers.host.override", "localhost");

        // Set Docker host explicitly
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null || dockerHost.isEmpty()) {
            System.setProperty("DOCKER_HOST", "unix:///var/run/docker.sock");
        }

        // Disable Ryuk if it's causing issues
        System.setProperty("testcontainers.ryuk.disabled", "false");

        try {
            elasticsearchContainer = new ElasticsearchContainer(ELASTICSEARCH_IMAGE)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withReuse(true);
            elasticsearchContainer.start();
        } catch (Exception e) {
            System.err.println("Failed to start Elasticsearch container: " + e.getMessage());
            throw new RuntimeException("Could not start Elasticsearch test container", e);
        }
    }

    /**
     * Configure dynamic properties for Spring to connect to the containerized Elasticsearch
     */
    @DynamicPropertySource
    static void setElasticsearchProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearchContainer::getHttpHostAddress);
        registry.add("spring.data.elasticsearch.repositories.enabled", () -> "true");
        registry.add("elasticsearch.enabled", () -> "true");
    }

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo(elasticsearchContainer.getHttpHostAddress().replace("http://", ""))
                .build();
    }

    /**
     * Get the Elasticsearch container instance (useful for manual operations in tests)
     */
    public static ElasticsearchContainer getElasticsearchContainer() {
        return elasticsearchContainer;
    }
}
