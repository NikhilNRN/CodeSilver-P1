package expenseHistory;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.integration.TestDatabaseSetup;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.SQLException;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API Tests for GET /api/expenses/{expenseId}
 * Tests the endpoint from the Manager application perspective
 */
@Epic("Manager App")
@Feature("Get Expense By ID API Tests")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class GetExpenseByIdApiTest
{

    private static DatabaseConnection testDbConnection;
    private static String managerJwtCookie;

    @BeforeAll
    static void setupDatabase() throws SQLException, IOException {
        testDbConnection = TestDatabaseSetup.initializeTestDatabase();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 5001;
    }

    @AfterAll
    static void tearDown() {
        TestDatabaseSetup.cleanup();
        RestAssured.reset();
    }

    @BeforeEach
    void loginAsManager() {
        String requestBody = """
                {
                    "username": "manager1",
                    "password": "admin123"
                }
                """;

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/auth/login");

        if (response.getStatusCode() == 200) {
            managerJwtCookie = response.getCookie("jwt");
        }
    }

    // Happy Path
    @Test
    @Order(1)
    @Story("Get Expense Details")
    @Description("Successfully retrieve existing expense with valid ID")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenseById_ValidId_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/1")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .body("data.expense.id", equalTo(1))
                .body("data.expense.amount", notNullValue())
                .body("data.expense.description", notNullValue())
                .body("data.expense.date", notNullValue())
                .body("data.user", notNullValue())
                .body("data.approval", notNullValue());
    }

    @Test
    @Order(2)
    @Story("Get Expense Details")
    @Description("Verify all expense fields are present in response")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpenseById_ValidId_ContainsAllFields() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/1")
                .then()
                .statusCode(200)
                .body("data.expense.user_id", notNullValue())
                .body("data.user.username", notNullValue())
                .body("data.approval.status", notNullValue());
    }

    @Test
    @Order(3)
    @Story("Get Expense Details")
    @Description("Retrieve expense with pending status")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpenseById_PendingExpense_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/1")
                .then()
                .statusCode(200)
                .body("data.approval.status", equalTo("pending"));
    }

    @Test
    @Order(4)
    @Story("Get Expense Details")
    @Description("Retrieve expense with approved status")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpenseById_ApprovedExpense_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/2")
                .then()
                .statusCode(200)
                .body("data.approval.status", equalTo("approved"))
                .body("data.approval.reviewer", notNullValue());
    }

    // Sad Path
    @Test
    @Order(5)
    @Story("Get Expense Details")
    @Description("Non-existent expense ID returns 404")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenseById_NonExistentId_Returns404() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/99999")
                .then()
                .statusCode(404)
                .body("success", equalTo(false))
                .body("error", containsString("not found"));
    }

    @Test
    @Order(6)
    @Story("Get Expense Details")
    @Description("Invalid expense ID format returns 400")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpenseById_InvalidIdFormat_Returns400() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/abc")
                .then()
                .statusCode(400)
                .body("error", containsString("Invalid expense ID"));
    }

    @Test
    @Order(7)
    @Story("Get Expense Details")
    @Description("Negative expense ID returns 404")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpenseById_NegativeId_Returns404() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/-1")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404)));
    }

    // Authentication Tests
    @Test
    @Order(8)
    @Story("Authentication")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenseById_NoAuth_Returns401() {
        given()
                .cookie("jwt", "")
                .when()
                .get("/api/expenses/1")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(9)
    @Story("Authentication")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenseById_InvalidToken_Returns401() {
        given()
                .cookie("jwt", "invalid-token-12345")
                .when()
                .get("/api/expenses/1")
                .then()
                .statusCode(401);
    }

    // Edge case tests
    @Test
    @Order(10)
    @Story("Get Expense Details")
    @Description("Expense ID zero returns 404")
    @Severity(SeverityLevel.MINOR)
    void testGetExpenseById_ZeroId_Returns404() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/0")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(11)
    @Story("Get Expense Details")
    @Description("Maximum integer expense ID handles gracefully")
    @Severity(SeverityLevel.MINOR)
    void testGetExpenseById_MaxIntId_Returns404() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/" + Integer.MAX_VALUE)
                .then()
                .statusCode(404);
    }
}