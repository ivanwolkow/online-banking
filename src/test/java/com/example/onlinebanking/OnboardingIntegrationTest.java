package com.example.onlinebanking;

import com.example.onlinebanking.persistence.AccountRepository;
import com.example.onlinebanking.persistence.CustomerRepository;
import org.iban4j.IbanUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("docs")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.security.jwt.secret-base64=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                "app.database.rate-limit.enabled=false"
        }
)
class OnboardingIntegrationTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("postgres:18.6-alpine")
    );

    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CustomerRepository customers;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clearDatabase() {
        accounts.deleteAll();
        customers.deleteAll();
    }

    @Test
    void completesTheOnboardingFlowWithNormalizedInput() throws Exception {
        String registration = """
                {
                  "fullName": "  Ada Lovelace  ",
                  "username": "  Ada.Lovelace  ",
                  "dateOfBirth": "1990-12-10",
                  "address": {
                    "street": "  Keizersgracht  ",
                    "houseNumber": "  123A  ",
                    "postalCode": "  1015 CJ  ",
                    "city": "  Amsterdam  ",
                    "countryCode": "  nl  "
                  }
                }
                """;

        HttpResponse<String> registerResponse = post("/register", registration);
        assertThat(registerResponse.statusCode()).isEqualTo(201);
        JsonNode registered = json(registerResponse);
        assertThat(registered.path("username").asString()).isEqualTo("ada.lovelace");
        String password = registered.path("defaultPassword").asString();
        assertThat(password).isNotBlank();

        Map<String, Object> stored = jdbc.queryForMap("""
                select full_name, street, house_number, postal_code, city, country_code
                from customers
                where username = 'ada.lovelace'
                """);
        assertThat(stored)
                .containsEntry("full_name", "Ada Lovelace")
                .containsEntry("street", "Keizersgracht")
                .containsEntry("house_number", "123A")
                .containsEntry("postal_code", "1015 CJ")
                .containsEntry("city", "Amsterdam")
                .containsEntry("country_code", "NL");

        HttpResponse<String> modifiedPassword = post(
                "/login",
                loginBody("ada.lovelace", " " + password)
        );
        assertProblem(modifiedPassword, 401, "INVALID_CREDENTIALS", false);

        HttpResponse<String> loginResponse = post(
                "/login",
                loginBody("  ADA.LOVELACE  ", password)
        );
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        JsonNode loggedIn = json(loginResponse);
        assertThat(loggedIn.path("tokenType").asString()).isEqualTo("Bearer");
        assertThat(loggedIn.path("expiresIn").asLong()).isEqualTo(900);

        HttpResponse<String> overviewResponse = get(
                "/overview",
                "Bearer " + loggedIn.path("accessToken").asString()
        );
        assertThat(overviewResponse.statusCode()).isEqualTo(200);
        JsonNode overview = json(overviewResponse);
        String accountNumber = overview.path("accountNumber").asString();
        assertThat(accountNumber).matches("^NL[0-9]{2}RBNK[0-9]{10}$");
        assertThat(IbanUtil.isValid(accountNumber)).isTrue();
        assertThat(overview.path("accountType").asString()).isEqualTo("CURRENT");
        assertThat(overview.path("balance").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(overview.path("currency").asString()).isEqualTo("EUR");
    }

    @Test
    void mapsTheRealPostgreSqlUsernameConstraintByName() throws Exception {
        assertThat(post("/register", registrationBody("Ada", " Duplicate.User ", "NL")).statusCode())
                .isEqualTo(201);

        HttpResponse<String> duplicate = post(
                "/register",
                registrationBody("Grace", "duplicate.user", "BE")
        );

        assertProblem(duplicate, 400, "USERNAME_ALREADY_EXISTS", false);
        assertThat(customers.count()).isEqualTo(1);
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void validatesTrimmedValuesAndRejectsUnknownProperties() throws Exception {
        HttpResponse<String> tooShortAfterTrimming = post(
                "/register",
                registrationBody("Ada", "  ab  ", "NL")
        );
        assertProblem(tooShortAfterTrimming, 400, "VALIDATION_ERROR", true);

        String unknownProperty = registrationBody("Ada", "ada.valid", "NL")
                .replace("\n}", ",\n  \"unexpected\": true\n}");
        HttpResponse<String> unknown = post("/register", unknownProperty);
        assertProblem(unknown, 400, "MALFORMED_REQUEST", false);
    }

    @Test
    void returnsContractProblemsForDomainAndAuthenticationFailures() throws Exception {
        HttpResponse<String> underage = post(
                "/register",
                registrationBody("Young Customer", "young.customer", "NL", "2020-01-01")
        );
        assertProblem(underage, 400, "CUSTOMER_UNDERAGE", false);

        HttpResponse<String> country = post(
                "/register",
                registrationBody("Ada", "ada.usa", "US")
        );
        assertProblem(country, 400, "COUNTRY_NOT_ALLOWED", false);

        HttpResponse<String> credentials = post(
                "/login",
                loginBody("does.not.exist", "wrong-password")
        );
        assertProblem(credentials, 401, "INVALID_CREDENTIALS", false);

        assertProblem(get("/overview", null), 401, "AUTHENTICATION_REQUIRED", false);
        assertProblem(get("/overview", "Bearer invalid"), 401, "INVALID_TOKEN", false);
    }

    @Test
    void documentationEndpointsAreAvailableWithoutAuthentication() throws Exception {
        HttpResponse<String> apiDocs = get("/v3/api-docs", null);
        assertThat(apiDocs.statusCode()).isEqualTo(200);
        JsonNode contract = json(apiDocs);
        assertThat(contract.path("openapi").asString()).isEqualTo("3.1.0");
        assertJsonSuccessResponse(contract, "/register", "post", "201");
        assertJsonSuccessResponse(contract, "/login", "post", "200");
        assertJsonSuccessResponse(contract, "/overview", "get", "200");
        assertGenericInternalError(contract, "/register", "post");
        assertGenericInternalError(contract, "/login", "post");
        assertGenericInternalError(contract, "/overview", "get");

        HttpResponse<String> swaggerUi = get("/swagger-ui.html", null);
        assertThat(swaggerUi.statusCode()).isIn(200, 302);
    }

    private static void assertJsonSuccessResponse(JsonNode contract, String path, String method, String status) {
        JsonNode content = contract.path("paths")
                .path(path)
                .path(method)
                .path("responses")
                .path(status)
                .path("content");

        assertThat(content.has(MediaType.APPLICATION_JSON_VALUE)).isTrue();
    }

    private static void assertGenericInternalError(JsonNode contract, String path, String method) {
        JsonNode responses = contract.path("paths").path(path).path(method).path("responses");

        assertThat(responses.has("500")).isTrue();
        assertThat(responses.has("503")).isFalse();
        assertThat(responses.has("404")).isFalse();
        assertThat(responses.has("409")).isFalse();
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path, String authorization) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (authorization != null) {
            request.header("Authorization", authorization);
        }

        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private JsonNode json(HttpResponse<String> response) {
        return mapper.readTree(response.body());
    }

    private void assertProblem(
            HttpResponse<String> response,
            int expectedStatus,
            String expectedCode,
            boolean fieldErrorsExpected
    ) {
        assertThat(response.statusCode()).isEqualTo(expectedStatus);
        assertThat(response.headers().firstValue("Content-Type").orElse(""))
                .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

        JsonNode problem = json(response);
        assertThat(problem.path("status").asInt()).isEqualTo(expectedStatus);
        assertThat(problem.path("code").asString()).isEqualTo(expectedCode);
        assertThat(problem.path("type").asString())
                .isEqualTo("urn:problem:" + expectedCode.toLowerCase(Locale.ROOT).replace('_', '-'));
        assertThat(problem.has("errors")).isEqualTo(fieldErrorsExpected);
    }

    private static String loginBody(String username, String password) {
        return """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
    }

    private static String registrationBody(String fullName, String username, String countryCode) {
        return registrationBody(fullName, username, countryCode, "1990-12-10");
    }

    private static String registrationBody(
            String fullName,
            String username,
            String countryCode,
            String dateOfBirth
    ) {
        return """
                {
                  "fullName": "%s",
                  "username": "%s",
                  "dateOfBirth": "%s",
                  "address": {
                    "street": "Keizersgracht",
                    "houseNumber": "123A",
                    "postalCode": "1015 CJ",
                    "city": "Amsterdam",
                    "countryCode": "%s"
                  }
                }
                """.formatted(fullName, username, dateOfBirth, countryCode);
    }
}
