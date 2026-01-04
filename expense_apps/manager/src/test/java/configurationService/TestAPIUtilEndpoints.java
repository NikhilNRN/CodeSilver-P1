package configurationService;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import io.qameta.allure.*;

import static io.restassured.RestAssured.given;

@Epic("Manager App")
@Feature("Manager API")
@Story("API Utilities")
@DisplayName("API Utility Tests")
public class TestAPIUtilEndpoints {
    // IMPORTANT: These tests require the application (Main.java) to be running

    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;

    @BeforeEach
    @Step("Set up RestAssured request and response specifications")
    public void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port    = 5001;

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("X-Custom-Header", "RestAssuredDemo")
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectContentType(ContentType.JSON)
                .expectResponseTime(Matchers.lessThan(500L))
                .build();

        Allure.step("RequestSpec and ResponseSpec initialized successfully");
    }

    @AfterEach
    @Step("Reset RestAssured configuration")
    public void tearDown() {
        RestAssured.reset();
        Allure.step("RestAssured configuration reset");
    }

    @DisplayName("Manager App API Health")
    @Test
    @Severity(SeverityLevel.MINOR)
    public void testAPIHealth() {
        Allure.step("Sending GET request to /health endpoint", () -> {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/health")
                    .then()
                    .spec(responseSpec)
                    .statusCode(200)
                    .body("status", Matchers.equalTo("healthy"))
                    .body("version", Matchers.equalTo("1.0.0"))
                    .body("service", Matchers.equalTo("expense-manager-api"));
        });
        Allure.step("/health endpoint returned expected values");
    }

    /*
     * Currently fails (returns 404) since this endpoint is currently not set up
     * Keep expected status code at 200 because documentation indicates that this endpoint should be working
     */
    @DisplayName("Manager App API Info")
    @Test
    @Severity(SeverityLevel.MINOR)
    // @Disabled("Expected to fail since endpoint is currently unimplemented")
    public void testAPIInfo() {
        Allure.step("Sending GET request to /api endpoint", () -> {
            given()
                    .spec(requestSpec)
                    .when()
                    .get("/api")
                    .then()
                    .statusCode(200);
        });
        Allure.step("/api endpoint returned expected status code 200");
    }
}
