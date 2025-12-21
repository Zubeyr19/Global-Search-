# 🐳 Docker Setup Guide

This guide will help you run the entire Global Search application stack using Docker.

## Prerequisites

1. **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)
   - Download: https://www.docker.com/products/docker-desktop/
   - Minimum: 4GB RAM allocated to Docker
   - Recommended: 8GB RAM

2. **Docker Compose** (included with Docker Desktop)

## Quick Start

### Option 1: Run Everything with Docker Compose (RECOMMENDED)

```bash
# Start all services (MySQL, Elasticsearch, Spring Boot app)
docker-compose up -d

# View logs
docker-compose logs -f app

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

**That's it!** Your application will be available at:
- **API**: http://localhost:8080
- **Elasticsearch**: http://localhost:9200
- **MySQL**: localhost:3306

### Option 2: Run Only Infrastructure (MySQL + Elasticsearch)

If you want to run the Spring Boot app locally but use Dockerized MySQL and Elasticsearch:

```bash
# Start only MySQL and Elasticsearch
docker-compose up -d mysql elasticsearch

# Run your Spring Boot app locally with:
mvn spring-boot:run
```

## What Gets Created

### Services:
1. **mysql** - MySQL 8.3.0 database
   - Port: 3306
   - Database: `global_search_db`
   - User: `globaluser` / Password: `globalpass`
   - Root password: `rootpassword`

2. **elasticsearch** - Elasticsearch 8.13.2
   - Port: 9200 (HTTP), 9300 (Transport)
   - Single-node cluster
   - Security disabled for development

3. **app** - Your Spring Boot application
   - Port: 8080
   - Auto-connects to MySQL and Elasticsearch
   - Health check enabled

### Persistent Volumes:
- `mysql_data` - MySQL data (survives container restarts)
- `elasticsearch_data` - Elasticsearch indices (survives container restarts)

## Useful Docker Commands

### Service Management
```bash
# Start services
docker-compose up -d

# Stop services (keeps data)
docker-compose stop

# Restart a specific service
docker-compose restart app

# View service status
docker-compose ps

# View resource usage
docker stats
```

### Logs
```bash
# All logs
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f mysql
docker-compose logs -f elasticsearch

# Last 100 lines
docker-compose logs --tail=100 app
```

### Database Access
```bash
# Connect to MySQL
docker exec -it global-search-mysql mysql -u globaluser -pglobalpass global_search_db

# Run SQL file
docker exec -i global-search-mysql mysql -u globaluser -pglobalpass global_search_db < your-script.sql
```

### Elasticsearch Access
```bash
# Check cluster health
curl http://localhost:9200/_cluster/health?pretty

# List indices
curl http://localhost:9200/_cat/indices?v

# Check if data is indexed
curl http://localhost:9200/companies/_search?pretty
```

### Cleanup
```bash
# Stop and remove containers (keeps volumes)
docker-compose down

# Remove everything including volumes (CAUTION: deletes data!)
docker-compose down -v

# Remove dangling images
docker image prune

# Full cleanup (removes all unused containers, networks, images)
docker system prune -a
```

## Development Workflow

### 1. First Time Setup
```bash
# Clone the repository
git clone <your-repo>
cd Global-Search-

# Start Docker services
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps

# Check application is running
curl http://localhost:8080/actuator/health
```

### 2. Daily Development
```bash
# Start your day
docker-compose up -d

# Make code changes...

# Rebuild and restart app only
docker-compose up -d --build app

# View logs while developing
docker-compose logs -f app
```

### 3. Testing
```bash
# Run tests (they'll use Testcontainers for Elasticsearch)
mvn test

# Or run tests in Docker
docker-compose run --rm app mvn test
```

## Troubleshooting

### Services Won't Start
```bash
# Check if ports are already in use
netstat -an | findstr :8080
netstat -an | findstr :3306
netstat -an | findstr :9200

# Stop conflicting services
# Windows: Stop your local MySQL/Elasticsearch services
# Then restart Docker compose
docker-compose down
docker-compose up -d
```

### Application Can't Connect to Services
```bash
# Check service health
docker-compose ps

# All services should show "Up (healthy)"
# If not, check logs:
docker-compose logs mysql
docker-compose logs elasticsearch
```

### Out of Memory
```bash
# Increase Docker Desktop memory allocation:
# Docker Desktop → Settings → Resources → Memory → 8GB
# Then restart Docker

# Or reduce Elasticsearch memory:
# Edit docker-compose.yml, change ES_JAVA_OPTS to -Xms256m -Xmx256m
```

### Clean Slate (Nuclear Option)
```bash
# Stop everything
docker-compose down -v

# Remove all containers
docker rm -f $(docker ps -a -q)

# Remove all volumes
docker volume prune -f

# Start fresh
docker-compose up -d
```

## Environment Variables

You can override settings via environment variables:

```bash
# Custom database credentials
MYSQL_DATABASE=my_db MYSQL_USER=myuser docker-compose up -d

# Custom JWT secret
JWT_SECRET=my-super-secret-key docker-compose up -d
```

Or create a `.env` file:
```env
MYSQL_DATABASE=global_search_db
MYSQL_USER=globaluser
MYSQL_PASSWORD=globalpass
JWT_SECRET=your-secret-key-here
```

## Production Deployment

For production, modify `docker-compose.yml`:

1. Enable Elasticsearch security:
```yaml
xpack.security.enabled=true
```

2. Use secrets management (not environment variables)
3. Add reverse proxy (nginx)
4. Configure proper resource limits
5. Set up monitoring and logging
6. Use Docker Swarm or Kubernetes

## Performance Tips

### 1. Speed Up Builds
```bash
# Use BuildKit (faster Docker builds)
DOCKER_BUILDKIT=1 docker-compose build

# Or set in environment permanently (Windows)
$env:DOCKER_BUILDKIT=1
```

### 2. Multi-stage Builds
The Dockerfile uses multi-stage builds for smaller images:
- Build stage: 800MB+
- Runtime stage: ~300MB

### 3. Volume Mounts for Dev
For live reload during development, mount source code:
```yaml
# Add to app service in docker-compose.yml
volumes:
  - ./src:/app/src
```

## Next Steps

- ✅ Start services: `docker-compose up -d`
- ✅ Test API: http://localhost:8080/actuator/health
- ✅ Make changes and rebuild: `docker-compose up -d --build app`
- ✅ Run tests: `mvn test` (Testcontainers will work automatically)

## Support

If you encounter issues:
1. Check Docker Desktop is running
2. Check logs: `docker-compose logs -f`
3. Try clean start: `docker-compose down -v && docker-compose up -d`
4. Check GitHub issues

Happy Dockering! 🐳
