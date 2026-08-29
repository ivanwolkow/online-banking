# Customer Onboarding Backend

Java 21/Spring Boot backend with three business endpoints: `POST /register`, `POST /login`, and `GET /overview`.

## Test and run locally

The automated suite uses Testcontainers, so start Docker before running:

```sh
./mvnw test
```

To run the application directly, start PostgreSQL 18.6, set the connection details if they differ from the defaults, and run:

```sh
export JWT_SECRET_BASE64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=
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

`application.yml` externalizes country eligibility, business clock zone, account details, JWT issuer/secret/lifetime, and the database limiter. `JWT_SECRET_BASE64` is required at startup and must decode to at least 32 bytes.

A global `DatabaseOperationGate` uses Guava's `RateLimiter` to admit at most two database-backed business operations per second for one application replica. It acquires a permit before registration, login, or overview starts persistence work; if capacity is unavailable, the request receives the generic `500 INTERNAL_ERROR` response. Multi-replica coordination is intentionally out of scope.

Registration normalizes usernames to lowercase, accepts NL/BE residences by default, verifies the exact 18th-birthday boundary, creates customer/account in one transaction, stores BCrypt-12 hashes only, and uses iban4j to generate checksum-valid Dutch IBANs. JWT validation is stateless and the overview uses the subject claim only.

Import `postman/Customer-Onboarding.postman_collection.json` to exercise the happy path and contract failure scenarios; it captures the generated password and bearer token automatically.
