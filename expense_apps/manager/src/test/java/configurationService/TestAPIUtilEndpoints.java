package configurationService;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
                .statusCode(200)
                .body("status", Matchers.equalTo("healthy"))
                .body("version", Matchers.equalTo("1.0.0"))
                .body("service", Matchers.equalTo("expense-manager-api"));
    }

    // Currently fails (returns 404) since this endpoint is currently not set up
    @DisplayName("Manager App API Info")
    @Test
    public void testAPIInfo() {
        given()
                .spec(requestSpec)
            .when()
                .get("/api")
            .then()
                .statusCode(200);
    }
}
