# Loopy — Back End

## Run

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the backend (http://localhost:8080)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Requires Java 17+, Maven, and Docker.

