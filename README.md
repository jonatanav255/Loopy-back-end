# Loopy — Back End

## Run

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Start the backend (http://localhost:8080)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Requires Java 17+, Maven, and Docker.

## Rules for Claude

- After every change or phase, the application must compile and run successfully.
- If tests exist, all tests must pass before considering any work done.
- Never leave the codebase in a broken state.
- Run `mvn test` to verify before finishing any task.
