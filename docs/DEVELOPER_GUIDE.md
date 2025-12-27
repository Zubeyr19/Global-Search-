# Global Search - Developer Guide

## Table of Contents
1. [Getting Started](#getting-started)
2. [Project Structure](#project-structure)
3. [Development Environment Setup](#development-environment-setup)
4. [Coding Standards](#coding-standards)
5. [Adding New Features](#adding-new-features)
6. [Testing Guidelines](#testing-guidelines)
7. [Git Workflow](#git-workflow)
8. [Common Development Tasks](#common-development-tasks)
9. [Debugging Tips](#debugging-tips)
10. [Performance Considerations](#performance-considerations)

---

## Getting Started

### Prerequisites
- Java JDK 17 or higher
- Maven 3.9+
- MySQL 8.0
- Elasticsearch 8.13.2
- Git
- IntelliJ IDEA (recommended) or VS Code
- Postman or similar API testing tool

### Clone and Build
```bash
git clone https://github.com/Zubeyr19/Global-Search-.git
cd Global-Search-
mvn clean install
```

---

## Project Structure

```
Global-Search-/
├── src/
│   ├── main/
│   │   ├── java/com/globalsearch/
│   │   │   ├── config/              # Spring configuration classes
│   │   │   │   ├── AppConfig.java
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── CacheConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   └── ...
│   │   │   ├── controller/          # REST controllers
│   │   │   │   ├── auth/
│   │   │   │   │   └── AuthController.java
│   │   │   │   ├── search/
│   │   │   │   │   └── GlobalSearchController.java
│   │   │   │   ├── AuditLogController.java
│   │   │   │   └── ...
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/              # JPA entities
│   │   │   │   ├── User.java
│   │   │   │   ├── Company.java
│   │   │   │   ├── Sensor.java
│   │   │   │   └── ...
│   │   │   ├── document/            # Elasticsearch documents
│   │   │   │   ├── CompanyDocument.java
│   │   │   │   ├── SensorDocument.java
│   │   │   │   └── ...
│   │   │   ├── repository/          # Data access layer
│   │   │   │   ├── JPA repositories
│   │   │   │   └── search/ (Elasticsearch)
│   │   │   ├── service/             # Business logic
│   │   │   │   ├── search/
│   │   │   │   ├── auth/
│   │   │   │   └── ...
│   │   │   ├── security/            # Security components
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── ...
│   │   │   ├── util/                # Utility classes
│   │   │   │   ├── SearchUtils.java
│   │   │   │   └── InputSanitizer.java
│   │   │   ├── validation/          # Custom validators
│   │   │   │   ├── PasswordValidator.java
│   │   │   │   └── ValidPassword.java
│   │   │   └── GlobalSearchApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-{profile}.properties
│   └── test/
│       └── java/com/globalsearch/
│           ├── controller/
│           ├── service/
│           ├── security/
│           └── ...
├── docs/                            # Documentation
├── elasticsearch/                   # ES index schemas
├── bdd/                             # BDD feature files
├── uml/                             # UML diagrams
├── performance-tests/               # JMeter tests
└── pom.xml                          # Maven configuration
```

---

## Development Environment Setup

### 1. Database Setup

**MySQL:**
```sql
CREATE DATABASE global_search_db;
CREATE USER 'global_search_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON global_search_db.* TO 'global_search_user'@'localhost';
FLUSH PRIVILEGES;
```

**Elasticsearch:**
```bash
# Start Elasticsearch
cd /path/to/elasticsearch-8.13.2
bin/elasticsearch

# Verify it's running
curl http://localhost:9200
```

### 2. IDE Configuration

**IntelliJ IDEA:**
1. Import as Maven project
2. Enable annotation processing (for Lombok)
   - Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"
3. Install Lombok plugin
4. Set Java SDK to 17
5. Configure code style (use Google Java Style Guide)

**VS Code:**
1. Install Java Extension Pack
2. Install Spring Boot Extension Pack
3. Install Lombok Annotations Support
4. Configure `settings.json`:
```json
{
  "java.configuration.updateBuildConfiguration": "automatic",
  "java.jdt.ls.java.home": "/path/to/jdk-17"
}
```

### 3. Application Properties

Create `application-dev.properties` for local development:
```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/global_search_db
spring.datasource.username=global_search_user
spring.datasource.password=your_password

# Elasticsearch
spring.elasticsearch.uris=http://localhost:9200

# JWT
jwt.secret=your-dev-secret-key-here
jwt.expiration=86400000

# Logging
logging.level.com.globalsearch=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

Run with dev profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## Coding Standards

### General Principles
- Follow SOLID principles
- Write self-documenting code
- Keep methods small and focused
- Use meaningful variable names
- Avoid magic numbers

### Java Code Style
- Use Google Java Style Guide
- Maximum line length: 120 characters
- Use 4 spaces for indentation
- Place braces on same line (K&R style)
- Always use `{}` even for single-line if/for/while

### Naming Conventions
- **Classes:** PascalCase (e.g., `SearchService`)
- **Methods:** camelCase (e.g., `performSearch`)
- **Variables:** camelCase (e.g., `searchResults`)
- **Constants:** UPPER_SNAKE_CASE (e.g., `MAX_RESULTS`)
- **Packages:** lowercase (e.g., `com.globalsearch.service`)

### Example:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private static final int MAX_RESULTS_PER_PAGE = 100;

    private final CompanySearchRepository companySearchRepository;
    private final AuditLogService auditLogService;

    /**
     * Perform global search across all entity types
     *
     * @param request Search request with query and filters
     * @param user Current authenticated user
     * @return Search response with paginated results
     */
    public GlobalSearchResponse globalSearch(GlobalSearchRequest request, User user) {
        log.debug("Executing search for user: {}, query: {}", user.getUsername(), request.getQuery());

        // Validate input
        if (request.getSize() > MAX_RESULTS_PER_PAGE) {
            throw new IllegalArgumentException("Page size exceeds maximum: " + MAX_RESULTS_PER_PAGE);
        }

        // Execute search logic...
        return response;
    }
}
```

### Documentation
- Use JavaDoc for all public methods and classes
- Include `@param`, `@return`, `@throws` tags
- Document complex logic with inline comments
- Keep comments up to date with code changes

---

## Adding New Features

### 1. Adding a New Entity Type

**Step 1: Create JPA Entity**
```java
@Entity
@Table(name = "new_entity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Other fields...
}
```

**Step 2: Create Elasticsearch Document**
```java
@Document(indexName = "new_entity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEntityDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String tenantId;

    @Field(type = FieldType.Text)
    private String name;

    // Other fields...
}
```

**Step 3: Create Repositories**
```java
// JPA Repository
public interface NewEntityRepository extends JpaRepository<NewEntity, Long> {
    List<NewEntity> findByTenantId(String tenantId);
}

// Elasticsearch Repository
public interface NewEntitySearchRepository extends ElasticsearchRepository<NewEntityDocument, Long> {
    List<NewEntityDocument> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name);
}
```

**Step 4: Create Service**
```java
@Service
@RequiredArgsConstructor
public class NewEntityService {
    private final NewEntityRepository repository;
    private final NewEntitySearchRepository searchRepository;

    // CRUD methods...
}
```

**Step 5: Create Controller**
```java
@RestController
@RequestMapping("/api/new-entity")
@RequiredArgsConstructor
public class NewEntityController {
    private final NewEntityService service;

    @GetMapping
    public ResponseEntity<?> getAll() {
        // Implementation...
    }
}
```

**Step 6: Add to Global Search**
Update `SearchService` to include new entity type in global search.

### 2. Adding a New API Endpoint

**Step 1: Define Request/Response DTOs**
```java
@Data
@Builder
public class NewFeatureRequest {
    @NotBlank
    private String parameter1;

    @Min(0)
    private Integer parameter2;
}

@Data
@Builder
public class NewFeatureResponse {
    private String result;
    private Long timestamp;
}
```

**Step 2: Implement Service Logic**
```java
@Service
@RequiredArgsConstructor
public class NewFeatureService {
    public NewFeatureResponse processRequest(NewFeatureRequest request) {
        // Business logic...
        return NewFeatureResponse.builder()
                .result("Success")
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
```

**Step 3: Create Controller Endpoint**
```java
@RestController
@RequestMapping("/api/feature")
@RequiredArgsConstructor
public class NewFeatureController {

    private final NewFeatureService service;

    @PostMapping
    public ResponseEntity<NewFeatureResponse> process(
            @Valid @RequestBody NewFeatureRequest request) {

        NewFeatureResponse response = service.processRequest(request);
        return ResponseEntity.ok(response);
    }
}
```

**Step 4: Add Security Configuration**
Update `SecurityConfig.java`:
```java
.requestMatchers("/api/feature/**").hasRole("MANAGER")
```

**Step 5: Write Tests**
```java
@SpringBootTest
@AutoConfigureMockMvc
class NewFeatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "MANAGER")
    void testNewFeature() throws Exception {
        // Test implementation...
    }
}
```

---

## Testing Guidelines

### Unit Tests
- Test individual components in isolation
- Mock dependencies using Mockito
- Aim for 70-80% code coverage
- Name tests descriptively: `testMethodName_scenario_expectedResult`

```java
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private CompanySearchRepository repository;

    @InjectMocks
    private SearchService service;

    @Test
    void testSearch_validQuery_returnsResults() {
        // Arrange
        when(repository.findByTenantId(anyString())).thenReturn(mockData());

        // Act
        GlobalSearchResponse response = service.globalSearch(request, user);

        // Assert
        assertNotNull(response);
        assertEquals(10, response.getTotalResults());
    }
}
```

### Integration Tests
- Test multiple components together
- Use `@SpringBootTest` for full context
- Test with real database (H2 for tests)

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testFullSearchFlow() throws Exception {
        mockMvc.perform(post("/api/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results").exists());
    }
}
```

### Running Tests
```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=SearchServiceTest

# With coverage report
mvn clean test jacoco:report

# Skip tests
mvn install -DskipTests
```

---

## Git Workflow

### Branch Naming
- `feature/feature-name` - New features
- `bugfix/bug-description` - Bug fixes
- `hotfix/critical-fix` - Production hotfixes
- `refactor/component-name` - Code refactoring
- `docs/documentation-update` - Documentation

### Commit Messages
Follow Conventional Commits:
```
type(scope): short description

Longer description if needed

Fixes #123
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style (formatting)
- `refactor`: Code refactoring
- `test`: Adding tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(search): add fuzzy matching support

Implemented Levenshtein distance algorithm for fuzzy search.
Allows up to 2 character edits for matching.

Closes #45

---

fix(auth): prevent account lockout for valid credentials

Fixed bug where successful login after failed attempts
did not reset the failure counter.

Fixes #67
```

### Workflow
```bash
# Create feature branch
git checkout -b feature/new-feature

# Make changes and commit
git add .
git commit -m "feat(module): description"

# Push to remote
git push origin feature/new-feature

# Create pull request on GitHub

# After review and approval, merge to develop
git checkout develop
git pull origin develop
git merge --no-ff feature/new-feature
git push origin develop

# Delete feature branch
git branch -d feature/new-feature
git push origin --delete feature/new-feature
```

---

## Common Development Tasks

### Adding Database Index
```sql
CREATE INDEX idx_sensor_tenant_type
ON sensors(tenant_id, sensor_type);
```

Update entity:
```java
@Table(name = "sensors",
       indexes = @Index(name = "idx_sensor_tenant_type",
                       columnList = "tenant_id, sensor_type"))
```

### Adding Cache
```java
@Cacheable(value = "entityCache", key = "#id")
public Entity getById(Long id) {
    return repository.findById(id).orElse(null);
}

@CacheEvict(value = "entityCache", key = "#entity.id")
public void update(Entity entity) {
    repository.save(entity);
}
```

### Adding Async Method
```java
@Async
@CompletableFuture<Result> processAsync(Request request) {
    // Long-running operation...
    return CompletableFuture.completedFuture(result);
}
```

### Adding WebSocket Notification
```java
@Autowired
private SimpMessagingTemplate messagingTemplate;

public void notifyUser(Long userId, String message) {
    messagingTemplate.convertAndSend(
        "/topic/notifications/" + userId,
        new Notification(message, LocalDateTime.now())
    );
}
```

---

## Debugging Tips

### Enable Debug Logging
```properties
logging.level.com.globalsearch=DEBUG
logging.level.org.hibernate.SQL=DEBUG
logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
```

### Common Issues

**Issue: Elasticsearch not connecting**
```bash
# Check if ES is running
curl http://localhost:9200

# Check application.properties
spring.elasticsearch.uris=http://localhost:9200
```

**Issue: JWT token validation fails**
```java
// Add logging in JwtAuthenticationFilter
log.debug("Token: {}", token);
log.debug("Username from token: {}", username);
```

**Issue: Multi-tenant data leaking**
```java
// Always add tenant filter
@PreAuthorize("@securityService.canAccessTenant(#tenantId)")
public List<Entity> findByTenantId(String tenantId) {
    return repository.findByTenantId(tenantId);
}
```

### Using IntelliJ Debugger
1. Set breakpoints (click left margin)
2. Run in Debug mode (Shift+F9)
3. Use Step Over (F8), Step Into (F7)
4. Evaluate expressions (Alt+F8)
5. Watch variables (Add to Watches)

---

## Performance Considerations

### Database Queries
- Use indexes on frequently queried columns
- Avoid N+1 queries (use `@EntityGraph` or JOIN FETCH)
- Use pagination for large result sets
- Batch operations when possible

```java
// Bad - N+1 queries
List<Company> companies = companyRepository.findAll();
for (Company company : companies) {
    company.getLocations().size(); // Lazy load
}

// Good - Single query with join
@EntityGraph(attributePaths = {"locations"})
List<Company> findAll();
```

### Caching Strategy
- Cache frequently accessed, rarely changed data
- Use appropriate TTLs
- Consider cache size limits
- Implement cache warming for critical data

### Elasticsearch Optimization
- Use appropriate field types
- Limit result size
- Use filters instead of queries when possible
- Implement scroll API for large datasets

---

## Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Project Architecture](./ARCHITECTURE.md)
- [API Usage Guide](./API_USAGE_GUIDE.md)
- [Troubleshooting Guide](./TROUBLESHOOTING.md)

---

**Document Version:** 1.0
**Last Updated:** 2025-11-09
**Maintained By:** Development Team
