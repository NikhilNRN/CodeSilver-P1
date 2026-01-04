package managerAuthentication;

import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;

@Epic("Manager Authentication API")
@Feature("Login, Status, and Logout")
public class TestAuthApi {

    static RequestSpecification requestSpec;
    static ResponseSpecification responseSpec;

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost:5001/";
        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("X-Custom-Header", "Test")
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(lessThan(5000L))
                .build();
    }

    @AfterAll
    static void tearDown() {
        RestAssured.reset();
    }

    // =====================
    // AUTH STATUS TEST
    // =====================
    @Test
    @Story("Check authentication status without login")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that the authentication status returns false when no login has occurred.")
    @DisplayName("C116_01 Get Authentication Status 200")
    public void getAuthStatus() {
        given()
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(false))
                .statusCode(200);
    }

    // =====================
    // LOGIN TESTS
    // =====================
    @Test
    @Story("Valid Manager Login")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that a valid manager can log in and obtain authentication status as true.")
    @DisplayName("C116_02 Valid Manager")
    public void validManagerLogin() {
        String uname = "manager1";
        String pword = "password123";

        String requestBody = String.format("""
                {"username":"%s","password":"%s"}
                """, uname, pword);

        Response response = performLogin(requestBody);

        String jwtCookie = response.getCookie("jwt");
        checkAuthStatus(jwtCookie, true);
    }

    @ParameterizedTest
    @Story("Invalid Manager Login")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that logging in with invalid credentials or non-manager users fails.")
    @DisplayName("C116_03 Invalid Logins")
    @CsvSource({
            "employee1, password123",
            "doesnotexist, none"
    })
    public void invalidManagerLogin(String uname, String pword) {
        String requestBody = String.format("""
                {"username":"%s","password":"%s"}
                """, uname, pword);

        performLoginExpectingFailure(requestBody, 401, "Invalid credentials or user is not a manager");
    }

    @ParameterizedTest
    @Story("Null Manager Login Fields")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that login requests missing username or password return a 400 error.")
    @DisplayName("C116_04 Null Logins")
    @CsvSource({
            "{\"password\": null}",
            "{\"username\": \"person\"}"
    })
    public void nullManagerLogin(String requestBody) {
        performLoginExpectingFailure(requestBody, 400, "Username and password are required");
    }

    @ParameterizedTest
    @Story("Malformed Login Request")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that malformed login requests return a 400 error.")
    @DisplayName("C116_05 Malformed Login Request")
    @CsvSource({
            "{\"username\": 145,\"password\": \"hi\"}",
            "{\"username\": \"hi\",\"malware\": \"bad\",\"password\": \"hi\"}"
    })
    public void testLoginBadRequest(String requestBody) {
        performLoginExpectingFailure(requestBody, 400, "Invalid request format");
    }

    @Test
    @Story("Full Logout Sequence")
    @Severity(SeverityLevel.CRITICAL)
    @Description("""
            Test login followed by logout.
            Ensures that the JWT cookie is removed and authentication status becomes false.
            BUG: Currently, logout does not invalidate the JWT on the server side.
            """)
    @DisplayName("C116_06 Full Logout Sequence: BUG")
    public void fullManagerLogoutSequence() {
        String uname = "manager1";
        String pword = "password123";

        String requestBody = String.format("""
                {"username":"%s","password":"%s"}
                """, uname, pword);

        Response response = performLogin(requestBody);
        String jwtCookie = response.getCookie("jwt");

        checkAuthStatus(jwtCookie, true);

        // Logout
        given()
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(200);

        // Auth status after logout
        checkAuthStatus(jwtCookie, false);
    }

    // =====================
    // HELPER METHODS
    // =====================
    @Step("Perform login with body: {requestBody}")
    private Response performLogin(String requestBody) {
        return given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .statusCode(200)
                .extract()
                .response();
    }

    @Step("Perform login expecting failure with body: {requestBody}, status: {statusCode}, error: {errorMessage}")
    private void performLoginExpectingFailure(String requestBody, int statusCode, String errorMessage) {
        given()
                .spec(requestSpec)
                .when()
                .body(requestBody)
                .post("/api/auth/login")
                .then()
                .spec(responseSpec)
                .body("error", equalTo(errorMessage))
                .statusCode(statusCode);
    }

    @Step("Check authentication status with JWT: {jwt}, expecting authenticated={expectedAuth}")
    private void checkAuthStatus(String jwt, boolean expectedAuth) {
        given()
                .cookie("jwt", jwt)
                .when()
                .get("/api/auth/status")
                .then()
                .spec(responseSpec)
                .body("authenticated", equalTo(expectedAuth))
                .statusCode(200);
    }
}
