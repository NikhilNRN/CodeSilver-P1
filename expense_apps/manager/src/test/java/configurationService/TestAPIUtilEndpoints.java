package configurationService;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;

public class TestAPIUtilEndpoints {
    private RequestSpecification requestSpec;
    private ResponseSpecification responseSpec;

    @BeforeEach
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
    }

    @AfterEach
    public void tearDown() {
        RestAssured.reset();
    }

    @DisplayName("Manager App API Health")
    @Test
    public void testAPIHealth() {
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
    }

    /*
     * Currently fails (returns 404) since this endpoint is currently not set up
     * Keep expected status code at 200 because documentation indicates that this endpoint should be working
     */
    @DisplayName("Manager App API Info")
    @Test
    // @Disabled("Expected to fail since endpoint is currently unimplemented")
    public void testAPIInfo() {
        given()
                .spec(requestSpec)
            .when()
                .get("/api")
            .then()
                .statusCode(200);
    }
}
