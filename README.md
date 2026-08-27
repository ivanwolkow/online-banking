# Customer Onboarding Backend

Java 21/Spring Boot backend with three business endpoints: `POST /register`, `POST /login`, and `GET /overview`.

## Run locally

Start PostgreSQL 18.6, then set the connection details if they differ from the defaults and run:

```sh
export JWT_SECRET_BASE64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
./mvnw test
./mvnw spring-boot:run
```

The development Compose stack includes PostgreSQL and enables the docs profile:

```sh
docker compose up --build
```

Open Swagger UI at http://localhost:8080/swagger-ui.html. To run without documentation endpoints, use `SPRING_PROFILES_ACTIVE=` (or omit the `docs` profile) and run the application directly.

Generate the checked-in OpenAPI artifact from a running docs-profile application:

```sh
curl -fsS http://localhost:8080/v3/api-docs.yaml -o docs/openapi.yaml
```

## Configuration and design

`application.yml` externalizes country eligibility, business clock zone, account details, JWT issuer/secret/lifetime, and the database limiter. The bundled JWT secret is for local development only and must be replaced outside it.

Each Hibernate SQL statement passes through `RateLimitedStatementInspector`. By default a single application replica starts no more than two statements per second, smoothly spaced by 500ms, and returns `503 DATABASE_BUSY` after a five-second wait. Multi-replica coordination is intentionally out of scope.

Registration normalizes usernames to lowercase, accepts NL/BE residences by default, verifies the exact 18th-birthday boundary, creates customer/account in one transaction, stores BCrypt-12 hashes only, and generates checksum-valid Dutch IBANs. JWT validation is stateless and the overview uses the subject claim only.

Import `postman/Customer-Onboarding.postman_collection.json` to exercise registration, login, and overview; it captures the generated password and bearer token automatically.
