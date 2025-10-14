package com.globalsearch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    /**
     * Configure OpenAPI documentation
     *
     * Swagger UI will be available at: http://localhost:8080/swagger-ui.html
     * OpenAPI JSON will be available at: http://localhost:8080/v3/api-docs
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Global Search API")
                        .version("1.0.0")
                        .description("""
                                Multi-Tenant IoT Global Search System with Elasticsearch Integration

                                This API provides comprehensive search and management capabilities for:
                                - Companies, Locations, Zones, and Sensors
                                - Reports and Dashboards
                                - User Management and Authentication
                                - Admin Dashboard and Monitoring
                                - Audit Logs and Activity Tracking

                                ## Features
                                - JWT-based authentication
                                - Role-based access control (RBAC)
                                - Multi-tenant data isolation
                                - Elasticsearch-powered search
                                - Document-level security
                                - Real-time activity monitoring
                                - Export to CSV/PDF/Excel

                                ## Security
                                All endpoints require authentication except for /api/auth/login
                                Use the JWT token in the Authorization header: Bearer <token>

                                ## Roles
                                - SUPER_ADMIN: Full system access including cross-tenant operations
                                - TENANT_ADMIN: Tenant management and administration
                                - MANAGER: Create, update, delete resources
                                - OPERATOR: Operate sensors and view data
                                - VIEWER: Read-only access
                                """)
                        .contact(new Contact()
                                .name("Global Search Team")
                                .email("support@globalsearch.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.globalsearch.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token obtained from /api/auth/login")));
    }
}
