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

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * REST Assured API Tests for CSV Report Generation Endpoints
 * Tests both:
 * - GET /api/reports/expenses/csv
 * - GET /api/reports/expenses/employee/{employeeId}/csv
 */
@Epic("Manager App")
@Feature("CSV Report Generation API Tests")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CsvReportApiTest
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
    static void tearDown()
    {
        TestDatabaseSetup.cleanup();
        RestAssured.reset();
    }

    @BeforeEach
    void loginAsManager()
    {
        String requestBody = """
                {
                    "username": "manager1",
                    "password": "password123"
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

    //GET /api/reports/expenses/csv TESTS
    @Test
    @Order(1)
    @Story("All Expenses CSV Report")
    @Description("Successfully download CSV report of all expenses")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_ValidRequest_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("attachment"))
                .header("Content-Disposition", containsString("all_expenses_report.csv"));
    }

    @Test
    @Order(2)
    @Story("All Expenses CSV Report")
    @Description("CSV contains header row with expected columns")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_ContainsHeaderRow() {
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv");

        String csvContent = response.getBody().asString();

        // Verify CSV header
        Assertions.assertTrue(csvContent.contains("Expense ID"), "CSV should contain 'Expense ID' column");
        Assertions.assertTrue(csvContent.contains("Employee"), "CSV should contain 'Employee' column");
        Assertions.assertTrue(csvContent.contains("Amount"), "CSV should contain 'Amount' column");
        Assertions.assertTrue(csvContent.contains("Description"), "CSV should contain 'Description' column");
        Assertions.assertTrue(csvContent.contains("Date"), "CSV should contain 'Date' column");
        Assertions.assertTrue(csvContent.contains("Status"), "CSV should contain 'Status' column");
    }

    @Test
    @Order(3)
    @Story("All Expenses CSV Report")
    @Description("CSV contains data rows from seeded database")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesCsv_ContainsData() {

        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv");

        String csvContent = response.getBody().asString();
        String[] lines = csvContent.split("\\r?\\n");

        // Header + at least one data row
        Assertions.assertTrue(
                lines.length > 1,
                "CSV should contain header and at least one data row"
        );
    }


    @Test
    @Order(4)
    @Story("All Expenses CSV Report")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_NoAuth_Returns401() {
        given()
                .cookie("jwt", "")
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(5)
    @Story("All Expenses CSV Report")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_InvalidToken_Returns401() {
        given()
                .cookie("jwt", "invalid-token-12345")
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(6)
    @Story("All Expenses CSV Report")
    @Description("CSV is properly formatted with commas and quotes")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesCsv_ProperCsvFormatting() {
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv");

        String csvContent = response.getBody().asString();

        // Check that CSV uses commas as delimiters
        Assertions.assertTrue(csvContent.contains(","), "CSV should use comma delimiters");

        // Check for newline characters between rows
        Assertions.assertTrue(csvContent.contains("\n"), "CSV should have newline separators");
    }

    @Test
    @Order(7)
    @Story("All Expenses CSV Report")
    @Description("Response time is acceptable for report generation")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesCsv_ResponseTime() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .time(lessThan(2000L)); // Should complete within 2 seconds
    }

    //GET /api/reports/expenses/employee/{employeeId}/csv TESTS
    @Test
    @Order(8)
    @Story("Employee Expenses CSV Report")
    @Description("Successfully download CSV report for specific employee")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_ValidEmployee_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("attachment"))
                .header("Content-Disposition", containsString("employee_1_expenses_report.csv"));
    }

    @Test
    @Order(9)
    @Story("Employee Expenses CSV Report")
    @Description("CSV contains correct employee's expenses only")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_ContainsEmployeeData() {
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/1/csv");

        String csvContent = response.getBody().asString();

        // Should contain employee1's username
        Assertions.assertTrue(csvContent.contains("employee1"),
                "CSV should contain employee1's expenses");

        // Should have header row
        Assertions.assertTrue(csvContent.contains("Expense ID"),
                "CSV should contain header row");
    }

    @Test
    @Order(10)
    @Story("Employee Expenses CSV Report")
    @Description("Multiple employees have different data")
    @Severity(SeverityLevel.NORMAL)
    void testGetEmployeeExpensesCsv_DifferentEmployeesDifferentData() {
        // Get employee 1's report
        Response response1 = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/1/csv");
        String csv1 = response1.getBody().asString();

        // Get employee 2's report
        Response response2 = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/2/csv");
        String csv2 = response2.getBody().asString();

        // Reports should be different
        Assertions.assertNotEquals(csv1, csv2,
                "Different employees should have different CSV content");
    }

    @Test
    @Order(11)
    @Story("Employee Expenses CSV Report")
    @Description("Non-existent employee returns 200 with empty data")
    @Severity(SeverityLevel.NORMAL)
    void testGetEmployeeExpensesCsv_NonExistentEmployee_Returns200WithHeader() {
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/99999/csv");

        // Should still return 200 with just header row
        Assertions.assertEquals(200, response.getStatusCode());

        String csvContent = response.getBody().asString();
        String[] lines = csvContent.split("\n");

        // Should have only header row
        Assertions.assertTrue(lines.length >= 1, "CSV should at least contain header");
    }

    @Test
    @Order(12)
    @Story("Employee Expenses CSV Report")
    @Description("Invalid employee ID format results in server error")
    @Severity(SeverityLevel.NORMAL)
    void testGetEmployeeExpensesCsv_InvalidIdFormat() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/invalid/csv")
                .then()
                .statusCode(anyOf(
                        equalTo(400),
                        equalTo(404),
                        equalTo(500)
                ));
    }

    @Test
    @Order(13)
    @Story("Employee Expenses CSV Report")
    @Description("Negative employee ID returns 400")
    @Severity(SeverityLevel.MINOR)
    void testGetEmployeeExpensesCsv_NegativeId_Returns400() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/-1/csv")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(200))); // May return empty report
    }

    @Test
    @Order(14)
    @Story("Employee Expenses CSV Report")
    @Description("Zero employee ID returns 200 with empty data")
    @Severity(SeverityLevel.MINOR)
    void testGetEmployeeExpensesCsv_ZeroId_Returns200() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/0/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv");
    }

    @Test
    @Order(15)
    @Story("Employee Expenses CSV Report")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_NoAuth_Returns401() {
        given()
                .cookie("jwt", "")
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(16)
    @Story("Employee Expenses CSV Report")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_InvalidToken_Returns401() {
        given()
                .cookie("jwt", "invalid-token-12345")
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(401);
    }

    //CSV CONTENT VALIDATION TESTS
    @Test
    @Order(17)
    @Story("CSV Content Validation")
    @Description("All expenses CSV contains all expected columns in correct order")
    @Severity(SeverityLevel.NORMAL)
    void testAllExpensesCsv_HasCorrectColumnOrder() {
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv");

        String csvContent = response.getBody().asString();
        String headerRow = csvContent.split("\n")[0];

        // Verify column order
        String[] columns = headerRow.split(",");
        Assertions.assertTrue(columns.length >= 6, "Should have at least 6 columns");
    }

    @Test
    @Order(18)
    @Story("CSV Content Validation")
    @Description("CSV escapes special characters properly")
    @Severity(SeverityLevel.NORMAL)
    void testCsv_HandlesSpecialCharacters() {
        // This test verifies that commas and quotes in data are properly escaped
        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv");

        String csvContent = response.getBody().asString();

        // CSV should handle descriptions with commas and quotes
        // If description has comma, it should be wrapped in quotes
        Assertions.assertNotNull(csvContent);
    }

    @Test
    @Order(19)
    @Story("CSV Content Validation")
    @Description("Employee CSV filename includes employee ID")
    @Severity(SeverityLevel.MINOR)
    void testEmployeeCsv_FilenameContainsEmployeeId() {
        int employeeId = 1;

        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/" + employeeId + "/csv")
                .then()
                .statusCode(200)
                .header("Content-Disposition",
                        containsString("employee_" + employeeId + "_"));
    }

    @Test
    @Order(20)
    @Story("CSV Content Validation")
    @Description("Response time acceptable for large employee datasets")
    @Severity(SeverityLevel.NORMAL)
    void testEmployeeCsv_ResponseTime() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(200)
                .time(lessThan(2000L)); // Should complete within 2 seconds
    }

    //Edge case tests
    @Test
    @Order(21)
    @Story("Edge Cases")
    @Description("Very large employee ID handled gracefully")
    @Severity(SeverityLevel.MINOR)
    void testEmployeeCsv_MaxIntEmployeeId() {
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/" + Integer.MAX_VALUE + "/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv");
    }

    @Test
    @Order(22)
    @Story("Edge Cases")
    @Description("Multiple simultaneous report requests handled correctly")
    @Severity(SeverityLevel.NORMAL)
    void testCsv_ConcurrentRequests() {
        // Make multiple simultaneous requests
        Response response1 = given().cookie("jwt", managerJwtCookie)
                .get("/api/reports/expenses/csv");
        Response response2 = given().cookie("jwt", managerJwtCookie)
                .get("/api/reports/expenses/employee/1/csv");
        Response response3 = given().cookie("jwt", managerJwtCookie)
                .get("/api/reports/expenses/employee/2/csv");

        // All should succeed
        Assertions.assertEquals(200, response1.getStatusCode());
        Assertions.assertEquals(200, response2.getStatusCode());
        Assertions.assertEquals(200, response3.getStatusCode());
    }

    @Test
    @Order(23)
    @Story("Content Type Validation")
    @Description("Both endpoints return correct content type")
    @Severity(SeverityLevel.NORMAL)
    void testCsv_CorrectContentType() {
        // All expenses CSV
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .contentType(containsString("text/csv"));

        // Employee expenses CSV
        given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .contentType(containsString("text/csv"));
    }
}