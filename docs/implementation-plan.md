# Customer Onboarding Backend: Scope and Implementation Plan

## Source

This plan is based on the requirements in [Customer Onboarding Assignment.pdf](./Customer%20Onboarding%20Assignment.pdf).

For implementation, read this plan together with the [Implementation Specification](./implementation-specification.md). The assignment PDF is the source of truth; either derived document may be revised when it adds unnecessary complexity or misinterprets the assignment.

## Repository Baseline

The project is greenfield: the Git repository contains planning documentation but no application code.

## Implementation Scope

Build a Java and Spring Boot backend using Maven and PostgreSQL. The application will expose exactly three public business endpoints.

### `POST /register`

- Accept the customer's full name, structured address, ISO country code, date of birth, and username.
- Validate required fields, country eligibility, and a minimum age of 18.
- Treat usernames as case-insensitively unique.
- Create one customer and one current account in a single transaction.
- Generate a valid and unique Dutch IBAN using the MOD-97 checksum algorithm.
- Generate a secure default password, return it once, and store only a strong one-way hash.
- Return:
  - `201 Created` after successful registration.
  - `400 Bad Request` for invalid input, an unsupported country, or an underage customer.
  - `400 Bad Request` when the normalized username already exists.

### `POST /login`

- Accept a username and the generated default password.
- Normalize the username consistently with registration.
- Verify the stored password hash.
- Return a short-lived signed bearer token after successful authentication.
- Return `401 Unauthorized` for invalid credentials.

### `GET /overview`

- Require a valid bearer token.
- Return the account's IBAN, account type, balance, and currency.
- Use the initial account values:
  - Account type: `CURRENT`
  - Balance: `0.00`
  - Currency: `EUR`
- Return `401 Unauthorized` when authentication is missing or invalid.

## Supporting Design Decisions

### Country eligibility

- Initially allow customers whose address country is the Netherlands (`NL`) or Belgium (`BE`).
- Store the allowed ISO country codes in external application configuration.
- Adding another country must require a configuration change rather than a code change.
- Both Dutch and Belgian residents receive a Dutch-format IBAN, as required by the assignment.

### Data model

- Store customer and account data in separate relational tables.
- Model the assignment's one-customer-to-one-account relationship with a unique foreign key.
- Add database-level unique constraints for normalized usernames and IBANs.
- Avoid unnecessary identifier-generation database calls; application-generated UUIDs are a sensible default.

### Authentication

- Use stateless signed JWT access tokens.
- Include the customer identifier in the token; additional non-sensitive claims are optional.
- Make token lifetime and signing configuration external settings.
- Validate tokens without querying the database, avoiding an additional database operation on `/overview`.

### Error responses

- Use consistent RFC 9457-style problem responses.
- Include stable application error codes and field-level validation details where applicable.
- Do not expose stack traces or persistence implementation details.

### Legacy database protection

- Admit database-backed business operations through a shared, global gate before they begin persistence work.
- Permit at most two operations per second for one application instance, failing fast before any database work starts when capacity is unavailable.
- Minimize the number of queries made by each use case.
- Do not reject a registration midway through its transaction because a later SQL statement cannot obtain a permit.
- Verify the behavior with automated tests.
- Scope this rate gate to a single application replica. A distributed limiter for multiple replicas is outside this assignment.

### Public surface

- Expose only `/register`, `/login`, and `/overview` as public business endpoints.
- Implement supporting behavior as internal application services rather than additional HTTP APIs.
- Generate OpenAPI JSON and YAML from controller, DTO, validation, and security annotations with springdoc.
- Provide Swagger UI through a documentation profile for local inspection and interactive API testing.
- Keep runtime documentation endpoints disabled in the base configuration; enable the documentation profile by default in Docker Compose for reviewers and use it to regenerate the checked-in YAML artifact.

## Out of Scope

- Identity-document verification and external KYC integrations.
- Email or SMS delivery of the generated password.
- Password changes, password recovery, or mandatory first-login password rotation.
- Refresh tokens, explicit logout, and token revocation.
- Money movement, deposits, withdrawals, or balance mutation.
- Multiple accounts per customer.
- A frontend or mobile client.
- Multi-replica deployment and distributed database throttling.
- Production cloud infrastructure or deployment.
- Publishing the repository to a remote Git host without a user-provided destination and credentials.

## Implementation Plan

### Phase checkpoint rule

Treat the numbered phases as implementation milestones. Tests should be written alongside the work in each phase rather than deferred until phase 8. After completing each phase, the implementing agent must:

1. Run the tests and validation commands relevant to that phase, including `git diff --check`.
2. Review `git status` and the complete diff for unintended or unrelated changes.
3. Fix every failure before proceeding; a phase with failing checks is not complete.
4. End the phase with a passing checkpoint commit whose message begins `phase N:`, where `N` is the phase number.
5. Record the validation commands and results for the final handoff summary.

Additional focused commits within a phase are allowed when useful, but do not include unfinished work from a later phase in the checkpoint. A validation-only phase may use `git commit --allow-empty` when a distinct checkpoint is useful.

### 1. Bootstrap the project

- Use the existing local Git repository and preserve its assignment-document ignore rule.
- Create the Maven and Spring Boot project structure targeting Java 21.
- Add Spring Web, Validation, Security, Data JPA, PostgreSQL, Flyway, JWT, and test dependencies.
- Add the Maven wrapper so the project does not depend on a preinstalled Maven version.
- Establish clear responsibility boundaries while allowing the concrete package and class layout to evolve during implementation.

### 2. Define and generate the API contract

- Treat the exact schemas in the implementation specification as the design-time contract.
- Implement request and response DTOs, validation, and controllers from that contract.
- Add the annotations and configuration needed for the generated document to cover operations, schemas, authentication, examples, and every supported status code.
- Generate `docs/openapi.yaml` from the running application after the controllers are implemented; do not maintain a second handwritten contract.
- Configure Swagger UI at `/swagger-ui.html` through the documentation profile.
- Add a contract test for the generated paths, methods, schemas, responses, and bearer security scheme.

### 3. Implement domain rules

- Implement exact age calculation using an injectable clock so the 18th-birthday boundary is testable.
- Implement configurable country eligibility using ISO country codes.
- Normalize and validate usernames consistently.
- Generate passwords with a cryptographically secure random source.
- Generate Dutch IBANs and validate their format and MOD-97 checksum.
- Define the account defaults and monetary precision.

### 4. Build persistence

- Add Flyway migrations for the customer and account tables, constraints, and indexes.
- Implement repositories or equivalent persistence adapters for registration, credential lookup, and account overview retrieval.
- Persist customer and account creation transactionally.
- Enforce uniqueness in the database to remain correct under concurrent registrations.
- Map the username constraint to a deterministic conflict response.
- Retry the rare IBAN collision with a newly generated candidate and a bounded attempt count.

### 5. Protect the legacy database

- Implement a shared persistence gate that spaces runtime database operations to the required rate.
- Apply the gate before application SQL execution so all repository paths are covered.
- Ensure retries also acquire permits.
- Add instrumentation suitable for automated verification without exposing a new public endpoint.
- Test concurrent operations to demonstrate that the database rate is not exceeded.

### 6. Implement registration

- Validate request structure and domain eligibility.
- Generate the password and IBAN.
- Create the customer and account atomically.
- Return the normalized username and generated default password.
- Implement validation, duplicate-username, and unexpected-error responses.

### 7. Implement authentication and overview

- Configure public access for registration and login and protected access for overview.
- Verify login credentials and issue a signed access token.
- Validate bearer tokens in the security filter chain without a database lookup.
- Load the authenticated customer's account overview with a focused query.
- Prevent one customer from accessing another customer's account data.

### 8. Complete automated test coverage

- Review the tests added during phases 1-7 and fill any remaining coverage gaps.

- Unit-test:
  - The day before, on, and after a customer's 18th birthday.
  - Allowed and unsupported countries.
  - Username normalization.
  - Password generation.
  - IBAN length, format, checksum, and generated-value variety.
  - Database gate scheduling.
- Integration-test:
  - Successful registration, login, and overview flow.
  - Duplicate username handling, including concurrent attempts.
  - Underage and unsupported-country registration.
  - Missing and malformed fields.
  - Invalid credentials.
  - Missing, invalid, and expired access tokens.
  - Persistence throttling under concurrent requests.

### 9. Produce delivery artifacts

- Add a Postman collection with environment variables and automated scripts for the end-to-end flow.
- Cover success, validation, duplicate, authentication, and authorization scenarios in the collection.
- Add a multi-stage Dockerfile.
- Add Docker Compose configuration for the application and PostgreSQL.
- Activate the documentation profile in Docker Compose so Swagger UI is available to reviewers by default.
- Write a README covering prerequisites, local execution, Docker execution, tests, configuration, sample requests, and design decisions.
- Document the single-replica interpretation of the legacy database constraint.

### 10. Perform final verification

- Run the complete unit and integration test suite.
- Build the production application artifact.
- Start the Docker Compose stack from a clean state and run an API smoke test.
- Regenerate `docs/openapi.yaml` from the final application and validate the generated document.
- Confirm that Swagger UI loads and can authorize with the login bearer token.
- Import and run the Postman collection against the containerized application.
- Check source formatting and dependency hygiene.
- Scan all repository content for prohibited organization references before handoff.
- Confirm that a new developer can follow the README without undocumented setup steps.

## Definition of Done

Implementation is complete when:

- All three endpoints satisfy the documented contract and functional requirements.
- The age, country, username, password, and IBAN rules are covered by automated tests.
- The overview endpoint cannot be used without successful authentication.
- Concurrent tests demonstrate that runtime database operations respect the two-per-second limit.
- The application and PostgreSQL start successfully through Docker Compose.
- The annotation-generated OpenAPI document and Postman collection cover the implemented behavior.
- Swagger UI is available in the documented local/Docker profile and disabled in the base configuration.
- The README provides repeatable local run and test instructions.
- The full automated test suite and final container smoke test pass.
