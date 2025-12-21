# 🚀 Getting Started with Global Search

Welcome! Here's the **fastest** way to get the Global Search application running.

## 🐳 Option 1: Docker (RECOMMENDED) - 2 Minutes

**Prerequisites:** Docker Desktop installed and running

### Quick Start

```bash
# Windows
docker-start.bat

# Mac/Linux
docker-compose up -d
```

That's it! The application will be available at:
- 🌐 **API**: http://localhost:8080
- 🔍 **Elasticsearch**: http://localhost:9200
- 🗄️ **MySQL**: localhost:3306

**What you get:**
- ✅ MySQL database (auto-configured)
- ✅ Elasticsearch (auto-configured)
- ✅ Spring Boot application (auto-connected)
- ✅ Sample data loaded
- ✅ All tests working with Testcontainers

**For detailed Docker instructions:** See [DOCKER-SETUP.md](DOCKER-SETUP.md)

---

## 💻 Option 2: Local Development - 15 Minutes

If you prefer running services locally:

### Prerequisites
- Java 17+
- Maven 3.9+
- MySQL 8.0+
- Elasticsearch 8.13.2

### Setup Steps

#### 1. Install Dependencies

**Windows:**
```bash
# Install MySQL
# Download from: https://dev.mysql.com/downloads/mysql/

# Install Elasticsearch
# Download from: https://www.elastic.co/downloads/elasticsearch
```

**Mac:**
```bash
brew install mysql elasticsearch
```

**Linux:**
```bash
sudo apt-get install mysql-server
# Elasticsearch: Follow official guide
```

#### 2. Configure MySQL

```bash
# Start MySQL
mysql -u root -p

# Create database
CREATE DATABASE global_search_db;
CREATE USER 'globaluser'@'localhost' IDENTIFIED BY 'globalpass';
GRANT ALL PRIVILEGES ON global_search_db.* TO 'globaluser'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

#### 3. Start Elasticsearch

```bash
# Windows
elasticsearch-8.13.2\bin\elasticsearch.bat

# Mac/Linux
elasticsearch
```

#### 4. Configure Application

Create `.env` file:
```env
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/global_search_db
SPRING_DATASOURCE_USERNAME=globaluser
SPRING_DATASOURCE_PASSWORD=globalpass
SPRING_ELASTICSEARCH_URIS=http://localhost:9200
JWT_SECRET=your-secret-key-here
```

#### 5. Run Application

```bash
# Build and run
mvn clean install
mvn spring-boot:run

# Or run with profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

---

## 🧪 Option 3: Development Mode with Docker

Run MySQL and Elasticsearch in Docker, but develop the Spring Boot app locally:

```bash
# Start only infrastructure
docker-compose -f docker-compose-dev.yml up -d

# Run app locally
mvn spring-boot:run
```

**Benefits:**
- Fast app restart (no Docker rebuild)
- Live reload with DevTools
- Easy debugging

---

## ✅ Verify Setup

### Check Services are Running

```bash
# Application health
curl http://localhost:8080/actuator/health

# Elasticsearch
curl http://localhost:9200

# MySQL
mysql -u globaluser -pglobalpass -h localhost global_search_db -e "SHOW TABLES;"
```

### Test the API

```bash
# Login (get JWT token)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Search (use token from login)
curl -X POST http://localhost:8080/api/search \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{"query":"sensor","page":0,"size":10"}'
```

---

## 📚 Next Steps

- ✅ **API Documentation**: http://localhost:8080/swagger-ui.html
- ✅ **Run Tests**: `mvn test` (Testcontainers will handle Elasticsearch)
- ✅ **Explore Codebase**: See [DESIGN-DOCUMENTATION.md](DESIGN-DOCUMENTATION.md)
- ✅ **Performance Testing**: See [PERFORMANCE_IMPLEMENTATION_SUMMARY.md](PERFORMANCE_IMPLEMENTATION_SUMMARY.md)

---

## 🐛 Troubleshooting

### Docker Issues

**Services won't start:**
```bash
# Check Docker is running
docker info

# View logs
docker-compose logs -f

# Clean restart
docker-compose down -v
docker-compose up -d
```

**Port conflicts:**
```bash
# Check what's using ports
netstat -an | findstr :8080
netstat -an | findstr :3306
netstat -an | findstr :9200

# Stop local MySQL/Elasticsearch if running
```

### Local Development Issues

**Can't connect to MySQL:**
```bash
# Verify MySQL is running
mysqladmin -u root -p status

# Check connection
mysql -u globaluser -pglobalpass -h localhost
```

**Can't connect to Elasticsearch:**
```bash
# Verify Elasticsearch is running
curl http://localhost:9200

# Check logs
tail -f elasticsearch-8.13.2/logs/elasticsearch.log
```

**Build failures:**
```bash
# Clean build
mvn clean install -DskipTests

# Update dependencies
mvn dependency:purge-local-repository
```

---

## 🎯 Recommended Setup

**For most users**: Use **Option 1 (Docker)** - it's the fastest and most reliable.

**For active development**: Use **Option 3 (Dev Mode)** - best of both worlds.

**For minimal setup**: Use **Option 1 (Docker)** then stop the app container and run locally.

---

## 📞 Need Help?

- Check [DOCKER-SETUP.md](DOCKER-SETUP.md) for detailed Docker instructions
- Review [README.md](README.md) for architecture and features
- Open an issue on GitHub

Happy coding! 🚀
