# Contributing to E-Commerce User Service

Thank you for considering contributing to this project! This document outlines the process for contributing to the E-Commerce User Service.

## 🚀 Quick Start

1. **Fork** the repository
2. **Clone** your fork locally
3. **Create** a new branch for your feature/fix
4. **Make** your changes
5. **Test** thoroughly
6. **Submit** a pull request

## 📋 Development Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- PostgreSQL (via Docker)

### Setup Steps
```bash
# Clone the repository
git clone https://github.com/yourusername/ecommerce-user-service.git
cd ecommerce-user-service

# Start dependencies
docker compose up -d

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test
```

## 🛠️ Code Standards

### Java Code Style
- Follow **Google Java Style Guide**
- Use **Spring Boot best practices**
- Implement **comprehensive error handling**
- Write **meaningful test cases**

### Naming Conventions
- Classes: `PascalCase`
- Methods/Variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.with.dots`

### Documentation
- **JavaDoc** for public APIs
- **README** updates for new features
- **API documentation** via OpenAPI/Swagger

## 🧪 Testing Requirements

- **Unit tests** for all service methods
- **Integration tests** for controller endpoints
- **Test coverage** minimum 80%
- **Mock external dependencies**

### Running Tests
```bash
# Unit tests
./mvnw test

# Integration tests
./mvnw verify

# Coverage report
./mvnw test
```

## 🔍 Code Review Process

1. **Self-review** your code
2. **Run all tests** locally
3. **Update documentation** if needed
4. **Create PR** with clear description
5. **Address feedback** promptly

### PR Requirements
- [ ] Tests pass
- [ ] Code follows style guide
- [ ] Documentation updated
- [ ] No breaking changes (or clearly marked)
- [ ] Meaningful commit messages

## 🐛 Bug Reports

When filing bug reports, please include:
- **Environment** details (Java version, OS)
- **Steps to reproduce** the issue
- **Expected** vs **actual** behavior
- **Logs/Screenshots** if applicable

## 💡 Feature Requests

For new features:
- **Describe** the use case
- **Explain** the proposed solution
- **Consider** backward compatibility
- **Discuss** in Issues first for major features

## 🏗️ Architecture Guidelines

### Layered Architecture
```
Controllers → Services → Repositories → Database
```

### Best Practices
- **Single Responsibility Principle**
- **Dependency Injection** via Spring
- **Event-Driven Architecture** for decoupling
- **RESTful API** design
- **Proper exception handling**

## 📚 Resources

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)

## 📞 Getting Help

- **GitHub Issues** for bug reports and feature requests
- **Discussions** for questions and general help
- **Email** [your-email@example.com] for private concerns

## 🎉 Recognition

Contributors will be:
- Added to **CONTRIBUTORS.md**
- Mentioned in **release notes**
- Given **credit** in documentation

Thank you for your contributions! 🙏