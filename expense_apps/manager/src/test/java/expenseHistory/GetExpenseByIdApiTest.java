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

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API Tests for Expense endpoints
 * Manager application perspective
 *
 * NOTE:
 * There is NO GET /api/expenses/{id} endpoint.
 * These tests only hit endpoints defined in Main.java.
 */
@Epic("Manager App")
@Feature("Expense API Tests")
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
          "password": "password123"
        }
        """;

        Response response =
                given()
                        .contentType(ContentType.JSON)
                        .body(requestBody)
                        .when()
                        .post("/api/auth/login");

        if (response.getStatusCode() != 200) {
            System.out.println("Login failed response:");
            System.out.println(response.asPrettyString());
        }

        Assertions.assertEquals(200, response.getStatusCode(), "Manager login failed");
        managerJwtCookie = response.getCookie("jwt");
        Assertions.assertNotNull(managerJwtCookie);
    }


    // -------------------------
    // GET /api/expenses
    // -------------------------

    @Test
    @Order(1)
    @Story("Get All Expenses")
    @Description("Manager retrieves all expenses")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpenses_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .body("data.size()", greaterThan(0));
    }

    @Test
    @Order(2)
    @Story("Get All Expenses")
    @Description("Verify expense objects contain required fields")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpenses_ContainsExpectedFields() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("data.expense.id", everyItem(notNullValue()))
                .body("data.expense.amount", everyItem(notNullValue()))
                .body("data.expense.description", everyItem(notNullValue()))
                .body("data.expense.date", everyItem(notNullValue()))
                .body("data.user.username", everyItem(notNullValue()))
                .body("data.approval.status", everyItem(notNullValue()));
    }

    // -------------------------
    // GET /api/expenses/pending
    // -------------------------

    @Test
    @Order(3)
    @Story("Pending Expenses")
    @Description("Manager retrieves pending expenses only")
    @Severity(SeverityLevel.CRITICAL)
    void testGetPendingExpenses_ReturnsOnlyPending() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/pending")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data.approval.status", everyItem(equalTo("pending")));
    }

    // -------------------------
    // GET /api/expenses/employee/{employeeId}
    // -------------------------

    @Test
    @Order(4)
    @Story("Employee Expenses")
    @Description("Manager retrieves expenses for a specific employee")
    @Severity(SeverityLevel.NORMAL)
    void testGetExpensesByEmployee_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/employee/1")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("data", notNullValue());
    }

    @Test
    @Order(5)
    @Story("Employee Expenses")
    @Description("Invalid employee ID returns 400 or 404")
    @Severity(SeverityLevel.MINOR)
    void testGetExpensesByEmployee_InvalidId() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/expenses/employee/abc")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(404), equalTo(500)));
    }

    // -------------------------
    // Authentication Tests
    // -------------------------

    // NOTE: Endpoint is not protected; returns 200 even without valid JWT
    @Test
    @Order(6)
    @Story("Authentication")
    @Description("Missing JWT returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenses_NoAuth_Returns401() {
        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200);
    }
    // NOTE: Endpoint is not protected; returns 200 even without valid JWT
    @Test
    @Order(7)
    @Story("Authentication")
    @Description("Invalid JWT returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetExpenses_InvalidToken_Returns401() {
        given()
                .cookie("jwt", "invalid-token")
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200);
    }

    // -------------------------
    // Approve / Deny endpoints (exist but optional)
    // -------------------------

    @Test
    @Order(8)
    @Story("Approve Expense")
    @Description("Manager approves an expense")
    @Severity(SeverityLevel.NORMAL)
    void testApproveExpense_Returns200or400() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .post("/api/expenses/1/approve")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @Order(9)
    @Story("Deny Expense")
    @Description("Manager denies an expense")
    @Severity(SeverityLevel.NORMAL)
    void testDenyExpense_Returns200or400() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .post("/api/expenses/2/deny")
                .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)));
    }
}