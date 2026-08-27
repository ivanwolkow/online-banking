# Customer Onboarding Backend: Implementation Specification

## Handoff Instructions

This file turns the implementation plan into a deterministic build specification for an implementation agent.

- Read [implementation-plan.md](./implementation-plan.md) first, then this file.
- This file is authoritative when it makes the plan more precise.
- Implement the numbered plan phases in order and run the relevant tests after each phase.
- Do not add public business endpoints, frameworks, or features that are not specified.
- Use package root `com.example.onlinebanking` and artifact name `online-banking`.
- Do not use Lombok or MapStruct. Use Java records for API DTOs and ordinary classes for JPA entities.
- Generate `docs/openapi.yaml` from the annotated running application; never hand-edit the generated file.
- Do not commit the supplied assignment PDF or publish the repository.
- Do not create Git commits unless explicitly requested.

## Pinned Technology Stack

Use these versions without substituting newer preview or milestone releases:

| Component | Version or image |
| --- | --- |
| Java language level | 21 |
| Build/runtime JDK | Eclipse Temurin `21.0.12+8` |
| Build container | `eclipse-temurin:21.0.12_8-jdk` |
| Runtime container | `eclipse-temurin:21.0.12_8-jre` |
| Spring Boot parent | `4.1.1` |
| Maven Wrapper | `3.9.16` |
| PostgreSQL | `18.6` |
| PostgreSQL container | `postgres:18.6-alpine` |
| OpenAPI document | OpenAPI `3.1.0` |
| springdoc-openapi | `3.1.0` |
| Testcontainers | `2.0.5`, managed by Spring Boot |

Use `spring-boot-starter-parent` for dependency management. Do not override its managed dependency versions. Relevant managed versions include Hibernate ORM `7.4.5.Final`, Flyway `12.4.0`, PostgreSQL JDBC `42.7.13`, Spring Security `7.1.1`, and Testcontainers `2.0.5`.

Required Maven dependencies:

- `org.springframework.boot:spring-boot-starter-web`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-security`
- `org.springframework.boot:spring-boot-starter-oauth2-resource-server`
- `org.springframework.boot:spring-boot-starter-flyway`
- `org.flywaydb:flyway-database-postgresql`
- `org.postgresql:postgresql` with runtime scope
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0`
- `org.springframework.boot:spring-boot-configuration-processor` as optional
- `org.springframework.boot:spring-boot-starter-test` with test scope
- `org.springframework.security:spring-security-test` with test scope
- `org.springframework.boot:spring-boot-testcontainers` with test scope
- `org.testcontainers:testcontainers-junit-jupiter` with test scope
- `org.testcontainers:testcontainers-postgresql` with test scope

Version references:

- [Spring Boot releases](https://spring.io/projects/spring-boot/)
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)
- [Apache Maven releases](https://maven.apache.org/download.cgi)
- [Eclipse Temurin 21 release](https://adoptium.net/news/2026/08/eclipse-temurin-8u502-11032-17020-21012-2504-2602-available)
- [PostgreSQL version policy](https://www.postgresql.org/support/versioning/)
- [springdoc-openapi documentation](https://springdoc.org/)

## OpenAPI Generation Rules

- The exact API contract below is the design-time source of truth before controllers exist.
- Use springdoc to derive the deliverable from implemented Spring mappings, Jakarta Validation constraints, and `io.swagger.v3.oas.annotations`.
- Add `@Tag` to controllers; add `@Operation` and complete `@ApiResponses` to every controller method; add `@Schema` descriptions, formats, constraints, and examples to DTO components.
- Define API title, version, and the `bearerAuth` HTTP bearer/JWT security scheme through an OpenAPI configuration bean.
- Add `@SecurityRequirement(name = "bearerAuth")` to `/overview` only.
- Explicitly document every problem response because exception-handler responses cannot be inferred completely from controller signatures.
- Do not annotate JPA entities or expose them as API schemas.
- Use `springdoc-openapi-starter-webmvc-ui`; it includes the API-document generation support, so do not also add `springdoc-openapi-starter-webmvc-api`.
- Keep `/v3/api-docs`, `/v3/api-docs.yaml`, and Swagger UI disabled by default. Enable them only with the `docs` Spring profile.
- Under the `docs` profile, serve Swagger UI at `/swagger-ui.html` and configure it to use `/v3/api-docs`.
- After implementation, start the application with the `docs` profile and export `/v3/api-docs.yaml` to `docs/openapi.yaml`.
- Treat `docs/openapi.yaml` as a generated, checked-in delivery artifact. Regenerate it after any API change and never edit it manually.

## Exact API Contract

All endpoints use JSON and are rooted directly at `/`; do not add an `/api` or version prefix. Unknown JSON properties must be rejected. Trim all string inputs before validation and persistence.

### Register

`POST /register`

Request body:

```json
{
  "fullName": "Ada Lovelace",
  "username": "ada.lovelace",
  "dateOfBirth": "1990-12-10",
  "address": {
    "street": "Keizersgracht",
    "houseNumber": "123A",
    "postalCode": "1015 CJ",
    "city": "Amsterdam",
    "countryCode": "NL"
  }
}
```

Validation and normalization:

| Field | Rule |
| --- | --- |
| `fullName` | Required Unicode string, trimmed length 2-100 |
| `username` | Required, trimmed length 3-50, characters `A-Z`, `a-z`, `0-9`, `.`, `_`, `-` only |
| `dateOfBirth` | Required ISO `YYYY-MM-DD`; customer must be at least 18 on the current business date |
| `address` | Required object |
| `address.street` | Required, trimmed length 1-100 |
| `address.houseNumber` | Required, trimmed length 1-20 |
| `address.postalCode` | Required, trimmed length 1-20 |
| `address.city` | Required, trimmed length 1-100 |
| `address.countryCode` | Required two ASCII letters; normalize to uppercase; must be `NL` or `BE` by default |

Normalize the username with `trim().toLowerCase(Locale.ROOT)`. Persist and return only the normalized value. Determine age using `LocalDate.now(clock)` in the configured `Europe/Amsterdam` business zone. A customer is eligible on their 18th birthday.

Success response: `201 Created`

```json
{
  "username": "ada.lovelace",
  "defaultPassword": "c29tZVJhbmRvbV8x"
}
```

The default password must contain 96 bits from `SecureRandom`, encoded with Base64 URL encoding without padding. This produces exactly 16 characters. Generate it once per registration request, return it only in this response, and store a BCrypt hash using strength 12.

Registration failures:

| Status | Code | Condition |
| --- | --- | --- |
| 400 | `MALFORMED_REQUEST` | Invalid JSON or an unknown property |
| 400 | `VALIDATION_ERROR` | Structural or field validation failure |
| 400 | `CUSTOMER_UNDERAGE` | Customer has not reached age 18 |
| 400 | `COUNTRY_NOT_ALLOWED` | Normalized country is not configured as allowed |
| 409 | `USERNAME_ALREADY_EXISTS` | Normalized username unique constraint conflict |
| 500 | `ACCOUNT_NUMBER_GENERATION_FAILED` | Five IBAN insert attempts all collide |
| 503 | `DATABASE_BUSY` | A database permit cannot be obtained within five seconds |

Do not return an access token or account number from registration.

### Login

`POST /login`

Request body:

```json
{
  "username": "ada.lovelace",
  "password": "c29tZVJhbmRvbV8x"
}
```

- `username`: required, trimmed length 3-50, normalized exactly as registration.
- `password`: required, length 1-100; do not trim or otherwise normalize it.
- Perform exactly one customer lookup by normalized username.
- Return the same failure for an unknown username and an incorrect password.

Success response: `200 OK`

```json
{
  "accessToken": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Login failures:

| Status | Code | Condition |
| --- | --- | --- |
| 400 | `MALFORMED_REQUEST` | Invalid JSON or an unknown property |
| 400 | `VALIDATION_ERROR` | Missing or invalid field |
| 401 | `INVALID_CREDENTIALS` | Unknown username or incorrect password |
| 503 | `DATABASE_BUSY` | Database permit wait exceeds five seconds |

### Account overview

`GET /overview`

Require `Authorization: Bearer <accessToken>`. Use the authenticated JWT subject as the customer UUID. Do not accept a customer identifier in the path, query, or body.

Success response: `200 OK`

```json
{
  "accountNumber": "NL91RBNK0123456789",
  "accountType": "CURRENT",
  "balance": 0.00,
  "currency": "EUR"
}
```

- `accountNumber`: exactly 18 uppercase alphanumeric characters and a checksum-valid Dutch IBAN.
- `accountType`: enum with the only current value `CURRENT`.
- `balance`: JSON number backed by `BigDecimal`, database precision 19 and scale 2.
- `currency`: ISO 4217 string with the current value `EUR`.
- Retrieve the account with one query by `customer_id`.

Overview failures:

| Status | Code | Condition |
| --- | --- | --- |
| 401 | `AUTHENTICATION_REQUIRED` | Bearer token is absent |
| 401 | `INVALID_TOKEN` | Bearer token is malformed, expired, incorrectly signed, or has an invalid issuer |
| 404 | `ACCOUNT_NOT_FOUND` | No account exists for the authenticated customer |
| 503 | `DATABASE_BUSY` | Database permit wait exceeds five seconds |

### Problem response

Return failures as `application/problem+json`. Use this exact shape; omit `errors` when there are no field errors and sort field errors by field name for deterministic tests.

```json
{
  "type": "urn:problem:validation-error",
  "title": "Request validation failed",
  "status": 400,
  "detail": "One or more fields are invalid.",
  "instance": "/register",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-08-27T12:00:00Z",
  "errors": [
    {
      "field": "dateOfBirth",
      "message": "customer must be at least 18 years old"
    }
  ]
}
```

Use `urn:problem:` plus the lower-kebab-case error code for `type`. Map unexpected exceptions to `500 INTERNAL_ERROR` with a generic detail and no stack trace.

## JWT Specification

- Use Spring Security's resource-server and JOSE support; do not add a third-party JWT library.
- Sign and verify with `HS256`.
- Read `JWT_SECRET_BASE64`, Base64-decode it, and fail startup if the decoded key is shorter than 32 bytes.
- Use issuer `online-banking` and require that issuer during validation.
- Set token lifetime to 15 minutes.
- Claims:
  - `sub`: customer UUID string
  - `username`: normalized username
  - `iss`: `online-banking`
  - `iat`: issue instant
  - `exp`: issue instant plus 15 minutes
  - `jti`: random UUID string
- Configure stateless security with CSRF disabled.
- Permit `/register` and `/login`; authenticate `/overview`; permit `/v3/api-docs/**`, `/swagger-ui.html`, and `/swagger-ui/**` when present; deny every other request.
- Write the specified problem JSON from both the authentication entry point and controller exception handling.

For local Docker Compose only, set `JWT_SECRET_BASE64` to `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`. Document that this is a development key and must be replaced outside local use.

## Database Schema and Persistence Rules

Create `src/main/resources/db/migration/V1__create_customer_and_account_tables.sql`.

`customers` columns:

| Column | Type and constraint |
| --- | --- |
| `id` | `uuid primary key` |
| `full_name` | `varchar(100) not null` |
| `username` | `varchar(50) not null`, constraint `uk_customers_username` unique |
| `password_hash` | `varchar(60) not null` |
| `date_of_birth` | `date not null` |
| `street` | `varchar(100) not null` |
| `house_number` | `varchar(20) not null` |
| `postal_code` | `varchar(20) not null` |
| `city` | `varchar(100) not null` |
| `country_code` | `varchar(2) not null` |
| `created_at` | `timestamp with time zone not null` |

`accounts` columns:

| Column | Type and constraint |
| --- | --- |
| `id` | `uuid primary key` |
| `customer_id` | `uuid not null`, foreign key to `customers(id)`, constraint `uk_accounts_customer` unique |
| `iban` | `char(18) not null`, constraint `uk_accounts_iban` unique |
| `account_type` | `varchar(20) not null`, check value is `CURRENT` |
| `balance` | `numeric(19,2) not null`, check value is at least zero |
| `currency` | `char(3) not null`, check value is `EUR` |
| `created_at` | `timestamp with time zone not null` |

Generate all UUIDs in Java with `UUID.randomUUID()`. Set `spring.jpa.hibernate.ddl-auto=validate` and `spring.jpa.open-in-view=false`. Use Flyway as the only schema-creation mechanism. Do not use H2; integration tests must use PostgreSQL through Testcontainers.

Registration must not issue a username-existence query. Attempt the insert and inspect the PostgreSQL constraint name through the root `PSQLException`:

- `uk_customers_username`: return `USERNAME_ALREADY_EXISTS` without retry.
- `uk_accounts_iban`: roll back the whole attempt and retry with a new IBAN.
- Any other constraint: rethrow for the generic internal-error handler.

Generate the password and its hash once outside the retry loop. Allow five transactional persistence attempts. Each attempt must run through a separate Spring bean method annotated `@Transactional(propagation = REQUIRES_NEW)` so a failed attempt cannot poison the next transaction.

## IBAN Algorithm

- Country code: `NL`.
- Fictional four-letter bank code: `RBNK`.
- Account component: a `SecureRandom` integer from 1 through 9,999,999,999, left-padded to 10 decimal digits.
- BBAN: `RBNK` followed by the 10-digit account component.
- Calculate check digits using ISO 13616 MOD-97:
  1. Form `BBAN + NL00`.
  2. Replace letters with `A=10` through `Z=35`.
  3. Compute the decimal value modulo 97 without converting the entire value to a fixed-width integer.
  4. Check digits are `98 - remainder`, left-padded to two digits.
  5. Final value is `NL + checkDigits + BBAN`.
- Assert in unit tests that every generated value is 18 characters, matches `^NL[0-9]{2}RBNK[0-9]{10}$`, and produces remainder 1 when the standard IBAN validation rearrangement is evaluated modulo 97.

## Database Rate-Limiter Design

The protected unit is each runtime SQL statement started by Hibernate, not each HTTP request or transaction. Flyway startup statements and JDBC metadata calls are outside the runtime limit.

Implement `DatabaseStatementRateLimiter` and install a singleton Hibernate `StatementInspector` through `HibernatePropertiesCustomizer`. Every application persistence path must use JPA/Hibernate; do not add direct `JdbcTemplate` or raw JDBC paths that bypass the inspector.

Limiter behavior:

- Maximum rate: two SQL statement starts per second per application instance.
- Minimum interval between permit completions: 500,000,000 nanoseconds.
- Maximum total permit wait: five seconds.
- Use monotonic `System.nanoTime()`, never wall-clock time, for scheduling.
- Use one fair `ReentrantLock(true)`.
- A caller records a five-second monotonic deadline, then uses timed `tryLock` for the remaining duration.
- While holding the lock, calculate the earliest permitted completion as `lastPermitNanos + 500,000,000`.
- Sleep while still holding the lock until that instant, but never beyond the caller's original deadline.
- Immediately before releasing the lock, set `lastPermitNanos` from the monotonic ticker and return.
- Holding the lock during the wait prevents delayed threads from overtaking one another and producing a burst.
- If the lock or scheduled permit cannot be obtained before the deadline, throw `DatabaseBusyException`.
- If interrupted, restore the interrupt flag and throw `DatabaseBusyException`.
- Map that exception, including when wrapped by Hibernate, to `503 DATABASE_BUSY` and `Retry-After: 1`.

Inject small `Ticker` and `Sleeper` interfaces into the limiter so unit tests can use deterministic fakes. Production implementations use `System.nanoTime()` and `LockSupport.parkNanos`, checking interruption after each park.

Set these properties:

```yaml
app:
  database:
    rate-limit:
      enabled: true
      operations-per-second: 2
      max-wait: 5s
```

Keep the limiter enabled by default. Disable it for general integration tests to keep the suite fast, but add one dedicated integration test with the real limiter enabled. Start four concurrent repository operations, record permit completion times, and assert adjacent completions are at least 450 milliseconds apart and the first-to-fourth duration is at least 1.45 seconds. Unit tests with a fake ticker must assert the exact 500-millisecond schedule and five-second rejection behavior.

## Application Configuration

Use `application.yml` with these effective defaults:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/online_banking}
    username: ${DB_USERNAME:online_banking}
    password: ${DB_PASSWORD:online_banking}
  jackson:
    deserialization:
      fail-on-unknown-properties: true
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate

springdoc:
  api-docs:
    enabled: false
    version: OPENAPI_3_1
  swagger-ui:
    enabled: false

app:
  clock-zone: Europe/Amsterdam
  registration:
    minimum-age: 18
    allowed-countries: [NL, BE]
  account:
    iban-country-code: NL
    iban-bank-code: RBNK
    type: CURRENT
    currency: EUR
    initial-balance: 0.00
  security:
    jwt:
      issuer: online-banking
      ttl: 15m
      secret-base64: ${JWT_SECRET_BASE64}
  database:
    rate-limit:
      enabled: true
      operations-per-second: 2
      max-wait: 5s
```

Add `application-docs.yml` containing:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

Bind `app.*` settings with validated `@ConfigurationProperties` records. Provide a `Clock` bean using the configured zone. Configure `compose.yaml` with `SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-docs}` so reviewers get the documentation profile by default while still being able to override it.

## Required Project Structure

```text
.
├── .mvn/wrapper/
├── docs/
│   ├── implementation-plan.md
│   ├── implementation-specification.md
│   └── openapi.yaml
├── postman/
│   ├── customer-onboarding.postman_collection.json
│   └── local.postman_environment.json
├── src/main/java/com/example/onlinebanking/
│   ├── OnlineBankingApplication.java
│   ├── api/
│   │   ├── RegistrationController.java
│   │   ├── LoginController.java
│   │   ├── OverviewController.java
│   │   ├── dto/
│   │   └── error/
│   ├── application/
│   │   ├── RegistrationService.java
│   │   ├── RegistrationPersistenceService.java
│   │   ├── LoginService.java
│   │   └── OverviewService.java
│   ├── config/
│   ├── domain/
│   ├── persistence/
│   │   ├── entity/
│   │   └── repository/
│   ├── ratelimit/
│   └── security/
├── src/main/resources/
│   ├── application.yml
│   ├── application-docs.yml
│   └── db/migration/V1__create_customer_and_account_tables.sql
├── src/test/java/com/example/onlinebanking/
├── .gitignore
├── compose.yaml
├── Dockerfile
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

Controllers perform HTTP mapping only. Application services coordinate use cases. Domain code owns age, country, password, and IBAN rules. Persistence code owns JPA entities and repositories. Security code owns password encoding and JWT behavior. Rate-limit code owns statement scheduling and Hibernate integration.

## Test and Delivery Rules

- Unit-test all domain boundary cases without Spring where possible.
- Use `@SpringBootTest` and Testcontainers `postgres:18.6-alpine` for persistence and end-to-end integration tests.
- Use Spring Security test support for authentication failures and protected endpoint tests.
- Do not mock repositories in end-to-end API tests.
- Add `OpenApiContractIntegrationTest` with the `docs` profile. Assert that the generated contract contains exactly `POST /register`, `POST /login`, and `GET /overview` as business operations, includes every specified response status and schema, defines `bearerAuth`, applies it only to `/overview`, emits OpenAPI 3.1 YAML successfully, and makes `/swagger-ui.html` resolvable.
- The Postman collection must create a unique username, store `defaultPassword`, log in, store `accessToken`, and call `/overview` automatically.
- The Dockerfile must be multi-stage, build with the pinned JDK image, and run as a non-root user with the pinned JRE image.
- `compose.yaml` must start PostgreSQL and the application, wait for PostgreSQL health, persist data in a named volume mounted at `/var/lib/postgresql` for PostgreSQL 18, expose the application on port 8080, and activate the `docs` profile by default.
- The README must give exact commands for `./mvnw test`, `./mvnw spring-boot:run`, `docker compose up --build`, opening `http://localhost:8080/swagger-ui.html`, regenerating `docs/openapi.yaml` from `/v3/api-docs.yaml`, and running without the `docs` profile.
- Before handoff, run `./mvnw verify`, regenerate and validate `docs/openapi.yaml`, verify Swagger UI and bearer authorization, run the Docker smoke flow, run `git diff --check`, and scan repository content for prohibited organization references.
