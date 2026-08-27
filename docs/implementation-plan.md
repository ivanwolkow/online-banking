# Customer Onboarding Backend: Scope and Implementation Plan

## Source

This plan is based on the requirements in [Customer Onboarding Assignment.pdf](./Customer%20Onboarding%20Assignment.pdf).

## Repository Baseline

The project is currently greenfield: the workspace contains the assignment document but no application code or initialized Git repository.

## Implementation Scope

Build a Java and Spring Boot backend using Maven and PostgreSQL. The application will expose exactly three public business endpoints.

### `POST /register`

- Accept the customer's full name, structured address, ISO country code, date of birth, and username.
- Validate required fields, country eligibility, and a minimum age of 18.
- Treat usernames as case-insensitively unique.
- Create one customer and one current account in a single transaction.
- Generate a valid and unique Dutch IBAN using the MOD-97 checksum algorithm.
- Generate a secure default password, return it once, and store only its BCrypt hash.
- Return:
  - `201 Created` after successful registration.
  - `400 Bad Request` for invalid input, an unsupported country, or an underage customer.
  - `409 Conflict` when the normalized username already exists.

### `POST /login`

- Accept a username and the generated default password.
- Normalize the username consistently with registration.
- Verify the stored BCrypt password hash.
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
- Generate entity identifiers in the application to avoid database sequence lookups.

### Authentication

- Use stateless signed JWT access tokens.
- Include the customer identifier and normalized username in the token.
- Make token lifetime and signing configuration external settings.
- Validate tokens without querying the database, avoiding an additional database operation on `/overview`.

### Error responses

- Use consistent RFC 9457-style problem responses.
- Include stable application error codes and field-level validation details where applicable.
- Do not expose stack traces or persistence implementation details.

### Legacy database protection

- Enforce the database limit at the persistence boundary rather than only limiting HTTP requests.
- Route every runtime application SQL operation through a shared gate that permits at most two database operations per second.
- Minimize the number of queries made by each use case.
- Throttle both reads and writes, including retry attempts.
- Verify the behavior with concurrent automated tests.
- Scope this rate gate to a single application replica. A distributed limiter for multiple replicas is outside this assignment.

### Public surface

- Expose only `/register`, `/login`, and `/overview` as public business endpoints.
- Implement supporting behavior as internal application services rather than additional HTTP APIs.
- Provide the OpenAPI description as a checked-in YAML document without requiring a publicly exposed documentation endpoint.

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

### 1. Bootstrap the project

- Initialize the local Git repository.
- Create the Maven and Spring Boot project structure targeting Java 21.
- Add Spring Web, Validation, Security, Data JPA, PostgreSQL, Flyway, JWT, and test dependencies.
- Add the Maven wrapper so the project does not depend on a preinstalled Maven version.
- Establish package boundaries for API, application, domain, persistence, security, and configuration code.

### 2. Define the API contract

- Create an `openapi.yaml` specification before implementing controllers.
- Define registration, login, overview, and problem response schemas.
- Document authentication, validation constraints, examples, and every supported status code.
- Implement request and response DTOs that match the specification.

### 3. Implement domain rules

- Implement exact age calculation using an injectable clock so the 18th-birthday boundary is testable.
- Implement configurable country eligibility using ISO country codes.
- Normalize and validate usernames consistently.
- Generate passwords with `SecureRandom`.
- Generate Dutch IBANs and validate their format and MOD-97 checksum.
- Define the account defaults and monetary precision.

### 4. Build persistence

- Add Flyway migrations for the customer and account tables, constraints, and indexes.
- Implement repositories for registration, credential lookup, and account overview retrieval.
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

### 8. Add automated tests

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
- Write a README covering prerequisites, local execution, Docker execution, tests, configuration, sample requests, and design decisions.
- Document the single-replica interpretation of the legacy database constraint.

### 10. Perform final verification

- Run the complete unit and integration test suite.
- Build the production application artifact.
- Start the Docker Compose stack from a clean state and run an API smoke test.
- Validate the OpenAPI YAML document.
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
- The OpenAPI document and Postman collection cover the implemented behavior.
- The README provides repeatable local run and test instructions.
- The full automated test suite and final container smoke test pass.
