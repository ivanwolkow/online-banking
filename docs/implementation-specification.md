# Customer Onboarding Backend: Implementation Specification

## Handoff Instructions

This file makes the implementation plan actionable for an implementation agent. It distinguishes required outcomes from recommended defaults and example implementation choices.

- Read [implementation-plan.md](./implementation-plan.md) first, then this file.
- The assignment PDF is authoritative. This derived specification may be revised when a design choice is not required by, or adds unnecessary complexity beyond, the assignment.
- Treat the numbered plan phases as milestones, respecting their dependencies, and apply the phase checkpoint protocol after every phase. Write relevant tests alongside each phase.
- Do not add public business endpoints or out-of-scope product features. Add focused implementation or test dependencies only when they are justified by the chosen design.
- `com.example.onlinebanking` and `online-banking` are recommended package and artifact identifiers. A conventional alternative is acceptable if it is used consistently and documented.
- Prefer a simple, readable Java design. Records, ordinary classes, handwritten mapping, or a focused mapping/code-generation library are all acceptable when their use remains clear and justified.
- Generate `docs/openapi.yaml` from the annotated running application; never hand-edit the generated file.
- Do not commit the supplied assignment PDF or publish the repository.

### Phase checkpoint protocol

After each numbered phase in the implementation plan:

1. Run the phase-specific unit, integration, build, contract, or smoke checks that are available at that point, plus `git diff --check`.
2. Inspect `git status` and the complete diff. Remove accidental files and resolve unrelated or out-of-scope changes.
3. If any check fails, diagnose and fix it, then rerun the checks. Do not proceed with a known failure.
4. Stage the completed phase without including unfinished work from a later phase.
5. End the phase with a passing checkpoint commit using a concise message in the form `phase N: <completed outcome>`.
6. Verify that the checkpoint commit succeeded and that no intended phase changes remain unstaged.
7. Keep a list of the commands run and their results for the final handoff summary.

Additional focused commits within a phase are acceptable when they help recovery or review. Do not combine unfinished phases in the checkpoint or publish the repository. A validation-only phase may use `git commit --allow-empty` when a distinct checkpoint is useful.

## Technology Baseline

Use this compatibility baseline. Do not substitute preview, milestone, release-candidate, or snapshot versions. A stable patch-level substitution is acceptable when needed for availability or compatibility and is documented in the handoff.

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

Use `spring-boot-starter-parent` for dependency management and normally accept its managed transitive versions. Override a managed version only to resolve a demonstrated compatibility or security issue, and document the reason.

Expected dependency baseline; omit an unused optional dependency or add a focused dependency when the resulting design remains within scope:

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
- Distribute annotations and OpenAPI configuration as the implementation sees fit. The generated result, rather than the placement of individual annotations, is the acceptance criterion.
- Ensure the generated document contains the API title and version, complete operations and schemas, examples, all supported response codes, and a `bearerAuth` HTTP bearer/JWT security scheme applied only to `/overview`.
- Explicitly document every problem response because exception-handler responses cannot be inferred completely from controller signatures.
- Keep the generated API schemas decoupled from JPA entities; API-specific DTOs are the preferred boundary.
- Use `springdoc-openapi-starter-webmvc-ui`; it includes the API-document generation support, so do not also add `springdoc-openapi-starter-webmvc-api`.
- Keep `/v3/api-docs`, `/v3/api-docs.yaml`, and Swagger UI disabled by default. Enable them only with the `docs` Spring profile.
- Under the `docs` profile, serve Swagger UI at `/swagger-ui.html` and configure it to use `/v3/api-docs`.
- After implementation, start the application with the `docs` profile and export `/v3/api-docs.yaml` to `docs/openapi.yaml`.
- Treat `docs/openapi.yaml` as a generated, checked-in delivery artifact. Regenerate it after any API change and never edit it manually.

## Chosen External API Contract (Required)

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

Generate the password with a cryptographically secure random source, provide at least 96 bits of entropy, return it only in this response, and store only a strong adaptive one-way hash. Base64 URL encoding without padding and BCrypt strength 12 are recommended defaults, not required implementations.

Registration failures:

| Status | Code | Condition |
| --- | --- | --- |
| 400 | `MALFORMED_REQUEST` | Invalid JSON or an unknown property |
| 400 | `VALIDATION_ERROR` | Structural or field validation failure |
| 400 | `CUSTOMER_UNDERAGE` | Customer has not reached age 18 |
| 400 | `COUNTRY_NOT_ALLOWED` | Normalized country is not configured as allowed |
| 400 | `USERNAME_ALREADY_EXISTS` | Normalized username unique constraint conflict |
| 500 | `INTERNAL_ERROR` | An internal failure occurred; implementation details are not exposed |

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
- Avoid redundant database access; the normal login path should need no more than one customer lookup by normalized username.
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
| 500 | `INTERNAL_ERROR` | An internal failure occurred; implementation details are not exposed |

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
- `balance`: JSON number with exact decimal semantics and two fractional digits; `BigDecimal` backed by `numeric(19,2)` is the recommended representation.
- `currency`: ISO 4217 string with the current value `EUR`.
- Avoid redundant database access; the normal overview path should retrieve the account with one query by authenticated customer identity.

Overview failures:

| Status | Code | Condition |
| --- | --- | --- |
| 401 | `AUTHENTICATION_REQUIRED` | Bearer token is absent |
| 401 | `INVALID_TOKEN` | Bearer token is malformed, expired, incorrectly signed, or has an invalid issuer |
| 500 | `INTERNAL_ERROR` | An internal failure occurred; implementation details are not exposed |

### Problem response

Return failures as `application/problem+json`. Use this shape and omit `errors` when there are no field errors. Consumers must not depend on the order of field errors.

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

Use `urn:problem:` plus the lower-kebab-case error code for `type`. Map every non-actionable internal failure, including database-capacity, account-number-generation, and missing-account failures, to `500 INTERNAL_ERROR` with a generic detail and no stack trace. Record the specific cause only in logs, metrics, or tracing.

## Authentication Outcomes and Recommended JWT Defaults

Required outcomes:

- Issue a signed, expiring, stateless bearer token after successful login.
- Put the customer identifier in `sub` and validate signature, issuer, and expiry without querying the database.
- Externalize signing material and fail startup when it is absent or too weak for the selected algorithm.
- Configure stateless security with CSRF disabled.
- Permit `/register` and `/login`; authenticate `/overview`; permit `/v3/api-docs/**`, `/swagger-ui.html`, and `/swagger-ui/**` when present; deny every other request.
- Return the specified problem JSON from both the authentication entry point and controller exception handling.

Recommended defaults:

- Use Spring Security resource-server and JOSE support rather than adding another JWT library.
- Use `HS256` with a Base64-encoded secret of at least 32 decoded bytes.
- Use issuer `online-banking` and a 15-minute lifetime.
- Include `iat` and `exp`; `username` and `jti` are optional non-sensitive claims.

For local Docker Compose, the development-only secret may be `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=`. Document that it must be replaced outside local use.

## Persistence Outcomes and Suggested Schema

Required outcomes:

- Manage the PostgreSQL schema through versioned Flyway migrations.
- Persist the customer and their single account atomically.
- Enforce normalized username uniqueness, IBAN uniqueness, and the one-account-per-customer relationship with database constraints so concurrent requests remain correct.
- Store all data required by the API, including a precise fixed-scale monetary balance.
- Distinguish a username conflict from an IBAN collision and other persistence failures without exposing database details to the client.
- Retry an IBAN collision safely with a bounded attempt count. A failed attempt must not leave partial customer or account data.
- Generate the password and its hash once per registration request so persistence retries do not change the returned credential.
- Exercise migration and persistence behavior against real PostgreSQL in the integration suite. An in-memory database may supplement fast tests but must not be the only integration database.

A straightforward baseline is separate `customers` and `accounts` tables with UUID identifiers, timestamps, a unique foreign key from account to customer, and these logical fields:

- Customer: full name, normalized username, password hash, date of birth, street, house number, postal code, city, and country code.
- Account: customer reference, IBAN, account type, `numeric(19,2)` balance, and currency.

Table names, column names, constraint names, identifier-generation strategy, and equivalent compatible SQL types may vary. Application-generated UUIDs, `spring.jpa.hibernate.ddl-auto=validate`, and `spring.jpa.open-in-view=false` are recommended defaults.

To minimize legacy database load, prefer relying on the unique username constraint rather than performing a separate existence read before insertion. An alternative is acceptable if it remains race-safe, preserves the database constraint as the authority, and does not increase the successful registration path beyond the documented rate budget.

Constraint-name inspection, SQL-state handling, a transaction template, or a separate transaction method are all acceptable ways to classify and retry failures. Keep the chosen approach clear in code or the handoff, and prove atomic rollback and bounded retry behavior with tests. Five IBAN attempts is the recommended default.

## IBAN Algorithm

- Every generated account number must be unique, use country code `NL`, conform to the 18-character Dutch IBAN structure, and pass the ISO 13616 MOD-97 validation.
- Use a configurable fictional four-letter bank code; `RBNK` is the recommended default.
- A simple account component is a `SecureRandom` integer from 1 through 9,999,999,999, left-padded to 10 decimal digits. An equivalent collision-resistant generation approach is acceptable.
- Treat the four-letter bank code followed by the 10-digit account component as the BBAN.
- Calculate check digits using MOD-97:
  1. Form `BBAN + NL00`.
  2. Replace letters with `A=10` through `Z=35`.
  3. Compute the decimal value modulo 97 without converting the entire value to a fixed-width integer.
  4. Check digits are `98 - remainder`, left-padded to two digits.
  5. Final value is `NL + checkDigits + BBAN`.
- Assert in unit tests that every generated value is 18 characters, matches `^NL[0-9]{2}[A-Z]{4}[0-9]{10}$`, and produces remainder 1 when the standard IBAN validation rearrangement is evaluated modulo 97.

## Database Protection Outcomes and Implementation Options

The assignment says that the legacy database cannot handle more than two requests per second, but it does not define a database request as an individual SQL statement. The protected unit is therefore a database-backed business operation: registration, login, or account overview. This prevents a registration from being rejected after part of its transaction has already run.

Required outcomes:

- Use one shared limiter for database-backed business operations in one application instance.
- Acquire a permit before any persistence work begins; do not queue callers or reject a later statement in an already admitted transaction.
- Fail fast with a generic `500 INTERNAL_ERROR` response when no permit is immediately available; do not expose the capacity mechanism to clients.
- Keep the limiter enabled by default and make its rate externally configurable.
- Minimize queries made by each use case and treat coordination across multiple replicas as out of scope.

The implementation mechanism is deliberately open. Use an established in-memory rate-limiter library rather than reimplementing concurrency and timing behavior. Guava's `RateLimiter` with non-blocking `tryAcquire()` is a simple suitable choice. An HTTP-only limiter is acceptable only when it has the same admission semantics for every database-backed operation; documentation and requests rejected by validation should not consume database capacity.

Suggested configuration defaults:

```yaml
app:
  database:
    rate-limit:
      enabled: true
      operations-per-second: 2
```

The limiter may be disabled in integration tests unrelated to throttling to keep the suite fast. Unit tests must cover admission, fail-fast rejection, and disabled operation without relying on narrow wall-clock timing assertions.

## Recommended Application Configuration

Externalize values expected to vary by environment: the database connection, business zone, eligible countries, fictional IBAN bank code, token settings, documentation profile, and database limiter settings. Account type, currency, and initial balance may be configuration or well-named constants. Use typed, validated configuration and fail startup for missing or invalid security-critical values. Property names and file organization may vary if the same behavior remains clear and documented.

The following is a suggested `application.yml` baseline:

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
```

One straightforward way to enable documentation only through the `docs` profile is an `application-docs.yml` containing:

```yaml
springdoc:
  api-docs:
    enabled: true
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
```

Prefer binding related settings through validated `@ConfigurationProperties`; records or ordinary classes are both suitable. Provide an injectable `Clock` based on the configured business zone. Docker Compose must make the documentation profile active by default for reviewers while allowing it to be overridden; the exact environment-variable expression is an implementation detail.

## Project Structure Guidelines

Use the standard Maven source layout. `com.example.onlinebanking` is the suggested package root, not a requirement. The following are architectural guardrails, not requirements for exact package names, class names, or nesting:

- Controllers handle HTTP mapping and delegate use-case work; they do not access repositories directly.
- API request and response DTOs are separate from JPA entities. Controllers never return persistence entities.
- Business rules such as age eligibility, allowed countries, username normalization, password generation, and IBAN generation remain independently unit-testable.
- Persistence concerns, Spring Security/JWT concerns, and database rate-limiting concerns do not leak into controller logic.
- Transaction boundaries and the IBAN retry mechanism remain explicit and testable, regardless of the chosen class layout.
- The implementing agent may merge, split, or rename internal packages and classes when that produces a simpler design.
- Meaningful deviations from the responsibility boundaries should be explained briefly in the README or final handoff; ordinary naming differences need no justification.

These delivery locations remain fixed because other artifacts and instructions refer to them:

- Maven build files and wrapper at the repository root.
- Application sources under the Maven `src/main/java/` source root.
- Tests under the Maven `src/test/java/` source root.
- Flyway migrations under `src/main/resources/db/migration/`.
- Base and documentation-profile configuration under `src/main/resources/`.
- Generated API contract at `docs/openapi.yaml`.
- Postman collection and environment under `postman/`.
- `README.md`, `Dockerfile`, and `compose.yaml` at the repository root.

## Test and Delivery Rules

- Use an appropriate mix of plain unit tests, focused Spring tests, and full integration tests. Keep domain boundary tests independent of Spring where practical.
- Exercise migrations, constraints, transactional behavior, and end-to-end persistence against a real compatible PostgreSQL instance. Testcontainers with the baseline PostgreSQL image is the recommended default, not a required test class or annotation style.
- Cover authentication failures and protected endpoint behavior. Spring Security test support is recommended.
- Mocks are acceptable in isolated unit or slice tests, but end-to-end API tests must exercise real application persistence rather than mocked repositories.
- Add an automated OpenAPI contract check under the documentation profile. Regardless of test class or assertion-library choice, verify that the generated contract contains exactly `POST /register`, `POST /login`, and `GET /overview` as business operations, includes every specified response status and schema, defines `bearerAuth`, applies it only to `/overview`, emits OpenAPI 3.1 YAML successfully, and makes `/swagger-ui.html` resolvable.
- The Postman collection must create a unique username, store `defaultPassword`, log in, store `accessToken`, and call `/overview` automatically.
- The Dockerfile must be multi-stage, use compatible Java 21 build and runtime images from the technology baseline, and run as a non-root user.
- `compose.yaml` must start PostgreSQL and the application, wait for PostgreSQL health, persist database data in a named volume at the image's documented data directory, expose the application on port 8080, and activate the `docs` profile by default.
- The README must give exact commands for `./mvnw test`, `./mvnw spring-boot:run`, `docker compose up --build`, opening `http://localhost:8080/swagger-ui.html`, regenerating `docs/openapi.yaml` from `/v3/api-docs.yaml`, and running without the `docs` profile.
- Before handoff, run `./mvnw verify`, regenerate and validate `docs/openapi.yaml`, verify Swagger UI and bearer authorization, run the Docker smoke flow, run `git diff --check`, and scan repository content for prohibited organization references.
