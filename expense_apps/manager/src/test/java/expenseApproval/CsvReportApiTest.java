package expenseApproval;

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
import static org.junit.jupiter.api.Assertions.*;

@Epic("Manager App")
@Feature("CSV Report Generation API Tests")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CsvReportApiTest {

    private static DatabaseConnection testDbConnection;
    private static String managerJwtCookie;

    @BeforeAll
    @Step("Initialize test database and RestAssured configuration")
    static void setupDatabase() throws SQLException, IOException {
        testDbConnection = TestDatabaseSetup.initializeTestDatabase();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 5001;
        Allure.step("Test database initialized and RestAssured configured");
    }

    @AfterAll
    @Step("Clean up test database and reset RestAssured")
    static void tearDown() {
        TestDatabaseSetup.cleanup();
        RestAssured.reset();
        Allure.step("Test database cleaned and RestAssured reset");
    }

    @BeforeEach
    @Step("Login as manager to obtain JWT cookie")
    void loginAsManager() {
        String requestBody = """
                {
                    "username": "manager1",
                    "password": "password123"
                }
                """;

        Response response = Allure.step("POST /api/auth/login", () ->
                given()
                        .contentType(ContentType.JSON)
                        .body(requestBody)
                        .when()
                        .post("/api/auth/login")
        );

        if (response.getStatusCode() == 200) {
            managerJwtCookie = response.getCookie("jwt");
            Allure.step("Manager JWT obtained: " + managerJwtCookie);
        }
    }

    @Test
    @Order(1)
    @Story("All Expenses CSV Report")
    @Description("Successfully download CSV report of all expenses")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_ValidRequest_Returns200() {
        Allure.step("GET /api/reports/expenses/csv with valid JWT", () ->
                given()
                        .cookie("jwt", managerJwtCookie)
                        .when()
                        .get("/api/reports/expenses/csv")
                        .then()
                        .statusCode(200)
                        .contentType("text/csv")
                        .header("Content-Disposition", containsString("attachment"))
                        .header("Content-Disposition", containsString("all_expenses_report.csv"))
        );
    }

    @Test
    @Order(2)
    @Story("All Expenses CSV Report")
    @Description("CSV contains header row with expected columns")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_ContainsHeaderRow() {
        Allure.step("GET /api/reports/expenses/csv and verify header row", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/csv");

            String csvContent = response.getBody().asString();

            assertTrue(csvContent.contains("Expense ID"), "CSV should contain 'Expense ID' column");
            assertTrue(csvContent.contains("Employee"), "CSV should contain 'Employee' column");
            assertTrue(csvContent.contains("Amount"), "CSV should contain 'Amount' column");
            assertTrue(csvContent.contains("Description"), "CSV should contain 'Description' column");
            assertTrue(csvContent.contains("Date"), "CSV should contain 'Date' column");
            assertTrue(csvContent.contains("Status"), "CSV should contain 'Status' column");
        });
    }

    @Test
    @Order(3)
    @Story("All Expenses CSV Report")
    @Description("CSV contains data rows from seeded database")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesCsv_ContainsData() {
        Allure.step("GET /api/reports/expenses/csv and verify data rows", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/csv");

            String csvContent = response.getBody().asString();
            String[] lines = csvContent.split("\\r?\\n");

            assertTrue(lines.length > 1, "CSV should contain header and at least one data row");
        });
    }

    @Test
    @Order(4)
    @Story("All Expenses CSV Report")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_NoAuth_Returns401() {
        Allure.step("GET /api/reports/expenses/csv without JWT returns 401", () ->
                given()
                        .cookie("jwt", "")
                        .when()
                        .get("/api/reports/expenses/csv")
                        .then()
                        .statusCode(401)
        );
    }

    @Test
    @Order(5)
    @Story("All Expenses CSV Report")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_InvalidToken_Returns401() {
        Allure.step("GET /api/reports/expenses/csv with invalid JWT returns 401", () ->
                given()
                        .cookie("jwt", "invalid-token-12345")
                        .when()
                        .get("/api/reports/expenses/csv")
                        .then()
                        .statusCode(401)
        );
    }

    // Additional tests follow the same pattern...
    // Wrap the HTTP request and verification inside Allure.step()
}
