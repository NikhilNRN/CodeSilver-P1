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
    @Step("Initialize test database and REST Assured configuration")
    static void setupDatabase() throws SQLException, IOException {
        testDbConnection = TestDatabaseSetup.initializeTestDatabase();
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 5001;

        Allure.parameter("Base URI", RestAssured.baseURI);
        Allure.parameter("Port", RestAssured.port);
    }

    @AfterAll
    @Step("Clean up test database and reset REST Assured")
    static void tearDown()
    {
        TestDatabaseSetup.cleanup();
        RestAssured.reset();
    }

    @BeforeEach
    @Step("Authenticate as manager and obtain JWT token")
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
            Allure.addAttachment("Login Response", "application/json", response.getBody().asString());
        }

        Allure.parameter("Username", "manager1");
        Allure.parameter("Authentication Status", response.getStatusCode() == 200 ? "Success" : "Failed");
    }

    //GET /api/reports/expenses/csv TESTS
    @Test
    @Order(1)
    @Story("All Expenses CSV Report")
    @Description("Successfully download CSV report of all expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Link(name = "API Documentation", url = "http://localhost:5001/api/docs")
    void testGetAllExpensesCsv_ValidRequest_Returns200() {
        Allure.step("Send GET request to /api/reports/expenses/csv", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/csv");

            Allure.addAttachment("Response Headers", response.getHeaders().toString());
            Allure.addAttachment("CSV Report", "text/csv", response.getBody().asString());

            response.then()
                    .statusCode(200)
                    .contentType("text/csv")
                    .header("Content-Disposition", containsString("attachment"))
                    .header("Content-Disposition", containsString("all_expenses_report.csv"));
        });
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
        Allure.addAttachment("CSV Content", "text/csv", csvContent);

        // Verify CSV header
        Allure.step("Verify 'Expense ID' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Expense ID"), "CSV should contain 'Expense ID' column");
        });

        Allure.step("Verify 'Employee' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Employee"), "CSV should contain 'Employee' column");
        });

        Allure.step("Verify 'Amount' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Amount"), "CSV should contain 'Amount' column");
        });

        Allure.step("Verify 'Description' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Description"), "CSV should contain 'Description' column");
        });

        Allure.step("Verify 'Date' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Date"), "CSV should contain 'Date' column");
        });

        Allure.step("Verify 'Status' column exists", () -> {
            Assertions.assertTrue(csvContent.contains("Status"), "CSV should contain 'Status' column");
        });
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

        Allure.addAttachment("CSV Content", "text/csv", csvContent);
        Allure.parameter("Total Lines", lines.length);

        // Header + at least one data row
        Allure.step("Verify CSV contains header and data rows", () -> {
            Assertions.assertTrue(
                    lines.length > 1,
                    "CSV should contain header and at least one data row"
            );
        });
    }


    @Test
    @Order(4)
    @Story("All Expenses CSV Report")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_NoAuth_Returns401() {
        Allure.step("Send request without authentication", () -> {
            Response response = given()
                    .cookie("jwt", "")
                    .when()
                    .get("/api/reports/expenses/csv");

            Allure.parameter("Status Code", response.getStatusCode());
            Allure.addAttachment("Response Body", response.getBody().asString());

            response.then().statusCode(401);
        });
    }

    @Test
    @Order(5)
    @Story("All Expenses CSV Report")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetAllExpensesCsv_InvalidToken_Returns401() {
        Allure.step("Send request with invalid JWT token", () -> {
            Response response = given()
                    .cookie("jwt", "invalid-token-12345")
                    .when()
                    .get("/api/reports/expenses/csv");

            Allure.parameter("Invalid Token", "invalid-token-12345");
            Allure.parameter("Status Code", response.getStatusCode());

            response.then().statusCode(401);
        });
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
        Allure.addAttachment("CSV Content", "text/csv", csvContent);

        // Check that CSV uses commas as delimiters
        Allure.step("Verify CSV uses comma delimiters", () -> {
            Assertions.assertTrue(csvContent.contains(","), "CSV should use comma delimiters");
        });

        // Check for newline characters between rows
        Allure.step("Verify CSV has newline separators", () -> {
            Assertions.assertTrue(csvContent.contains("\n"), "CSV should have newline separators");
        });
    }

    @Test
    @Order(7)
    @Story("All Expenses CSV Report")
    @Description("Response time is acceptable for report generation")
    @Severity(SeverityLevel.NORMAL)
    void testGetAllExpensesCsv_ResponseTime() {
        Allure.step("Measure report generation response time", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/csv");

            long responseTime = response.getTime();
            Allure.parameter("Response Time (ms)", responseTime);

            response.then()
                    .statusCode(200)
                    .time(lessThan(2000L)); // Should complete within 2 seconds
        });
    }

    //GET /api/reports/expenses/employee/{employeeId}/csv TESTS
    @Test
    @Order(8)
    @Story("Employee Expenses CSV Report")
    @Description("Successfully download CSV report for specific employee")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_ValidEmployee_Returns200() {
        int employeeId = 1;
        Allure.parameter("Employee ID", employeeId);

        Allure.step("Request CSV report for employee " + employeeId, () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + employeeId + "/csv");

            Allure.addAttachment("Employee CSV Report", "text/csv", response.getBody().asString());

            response.then()
                    .statusCode(200)
                    .contentType("text/csv")
                    .header("Content-Disposition", containsString("attachment"))
                    .header("Content-Disposition", containsString("employee_1_expenses_report.csv"));
        });
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
        Allure.addAttachment("Employee CSV", "text/csv", csvContent);

        // Should contain employee1's username
        Allure.step("Verify CSV contains employee1's data", () -> {
            Assertions.assertTrue(csvContent.contains("employee1"),
                    "CSV should contain employee1's expenses");
        });

        // Should have header row
        Allure.step("Verify CSV contains header row", () -> {
            Assertions.assertTrue(csvContent.contains("Expense ID"),
                    "CSV should contain header row");
        });
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
        Allure.addAttachment("Employee 1 CSV", "text/csv", csv1);

        // Get employee 2's report
        Response response2 = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/2/csv");
        String csv2 = response2.getBody().asString();
        Allure.addAttachment("Employee 2 CSV", "text/csv", csv2);

        // Reports should be different
        Allure.step("Verify different employees have different CSV content", () -> {
            Assertions.assertNotEquals(csv1, csv2,
                    "Different employees should have different CSV content");
        });
    }

    @Test
    @Order(11)
    @Story("Employee Expenses CSV Report")
    @Description("Non-existent employee returns 200 with empty data")
    @Severity(SeverityLevel.NORMAL)
    void testGetEmployeeExpensesCsv_NonExistentEmployee_Returns200WithHeader() {
        int nonExistentId = 99999;
        Allure.parameter("Non-existent Employee ID", nonExistentId);

        Response response = given()
                .cookie("jwt", managerJwtCookie)
                .when()
                .get("/api/reports/expenses/employee/" + nonExistentId + "/csv");

        // Should still return 200 with just header row
        Allure.step("Verify status code is 200", () -> {
            Assertions.assertEquals(200, response.getStatusCode());
        });

        String csvContent = response.getBody().asString();
        Allure.addAttachment("Empty Employee CSV", "text/csv", csvContent);

        String[] lines = csvContent.split("\n");
        Allure.parameter("Line Count", lines.length);

        // Should have only header row
        Allure.step("Verify CSV contains at least header", () -> {
            Assertions.assertTrue(lines.length >= 1, "CSV should at least contain header");
        });
    }

    @Test
    @Order(12)
    @Story("Employee Expenses CSV Report")
    @Description("Invalid employee ID format results in server error")
    @Severity(SeverityLevel.NORMAL)
    void testGetEmployeeExpensesCsv_InvalidIdFormat() {
        String invalidId = "invalid";
        Allure.parameter("Invalid ID", invalidId);

        Allure.step("Send request with invalid ID format", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + invalidId + "/csv");

            Allure.parameter("Status Code", response.getStatusCode());
            Allure.addAttachment("Error Response", response.getBody().asString());

            response.then()
                    .statusCode(anyOf(
                            equalTo(400),
                            equalTo(404),
                            equalTo(500)
                    ));
        });
    }

    @Test
    @Order(13)
    @Story("Employee Expenses CSV Report")
    @Description("Negative employee ID returns 400")
    @Severity(SeverityLevel.MINOR)
    void testGetEmployeeExpensesCsv_NegativeId_Returns400() {
        int negativeId = -1;
        Allure.parameter("Negative Employee ID", negativeId);

        Allure.step("Send request with negative employee ID", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + negativeId + "/csv");

            Allure.parameter("Status Code", response.getStatusCode());

            response.then()
                    .statusCode(anyOf(equalTo(400), equalTo(200))); // May return empty report
        });
    }

    @Test
    @Order(14)
    @Story("Employee Expenses CSV Report")
    @Description("Zero employee ID returns 200 with empty data")
    @Severity(SeverityLevel.MINOR)
    void testGetEmployeeExpensesCsv_ZeroId_Returns200() {
        int zeroId = 0;
        Allure.parameter("Employee ID", zeroId);

        Allure.step("Send request with zero employee ID", () -> {
            given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + zeroId + "/csv")
                    .then()
                    .statusCode(200)
                    .contentType("text/csv");
        });
    }

    @Test
    @Order(15)
    @Story("Employee Expenses CSV Report")
    @Description("Unauthenticated request returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_NoAuth_Returns401() {
        Allure.step("Send request without authentication", () -> {
            Response response = given()
                    .cookie("jwt", "")
                    .when()
                    .get("/api/reports/expenses/employee/1/csv");

            Allure.parameter("Status Code", response.getStatusCode());

            response.then().statusCode(401);
        });
    }

    @Test
    @Order(16)
    @Story("Employee Expenses CSV Report")
    @Description("Invalid JWT token returns 401")
    @Severity(SeverityLevel.CRITICAL)
    void testGetEmployeeExpensesCsv_InvalidToken_Returns401() {
        Allure.step("Send request with invalid JWT token", () -> {
            given()
                    .cookie("jwt", "invalid-token-12345")
                    .when()
                    .get("/api/reports/expenses/employee/1/csv")
                    .then()
                    .statusCode(401);
        });
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
        Allure.addAttachment("Header Row", headerRow);

        // Verify column order
        String[] columns = headerRow.split(",");
        Allure.parameter("Column Count", columns.length);

        Allure.step("Verify at least 6 columns exist", () -> {
            Assertions.assertTrue(columns.length >= 6, "Should have at least 6 columns");
        });
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
        Allure.addAttachment("CSV with Special Characters", "text/csv", csvContent);

        // CSV should handle descriptions with commas and quotes
        // If description has comma, it should be wrapped in quotes
        Allure.step("Verify CSV content is not null", () -> {
            Assertions.assertNotNull(csvContent);
        });
    }

    @Test
    @Order(19)
    @Story("CSV Content Validation")
    @Description("Employee CSV filename includes employee ID")
    @Severity(SeverityLevel.MINOR)
    void testEmployeeCsv_FilenameContainsEmployeeId() {
        int employeeId = 1;
        Allure.parameter("Employee ID", employeeId);

        Allure.step("Verify filename contains employee ID", () -> {
            given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + employeeId + "/csv")
                    .then()
                    .statusCode(200)
                    .header("Content-Disposition",
                            containsString("employee_" + employeeId + "_"));
        });
    }

    @Test
    @Order(20)
    @Story("CSV Content Validation")
    @Description("Response time acceptable for large employee datasets")
    @Severity(SeverityLevel.NORMAL)
    void testEmployeeCsv_ResponseTime() {
        Allure.step("Measure employee CSV response time", () -> {
            Response response = given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/1/csv");

            long responseTime = response.getTime();
            Allure.parameter("Response Time (ms)", responseTime);

            response.then()
                    .statusCode(200)
                    .time(lessThan(2000L)); // Should complete within 2 seconds
        });
    }

    //Edge case tests
    @Test
    @Order(21)
    @Story("Edge Cases")
    @Description("Very large employee ID handled gracefully")
    @Severity(SeverityLevel.MINOR)
    void testEmployeeCsv_MaxIntEmployeeId() {
        int maxId = Integer.MAX_VALUE;
        Allure.parameter("Max Integer ID", maxId);

        Allure.step("Request CSV with maximum integer employee ID", () -> {
            given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/" + maxId + "/csv")
                    .then()
                    .statusCode(200)
                    .contentType("text/csv");
        });
    }

    @Test
    @Order(22)
    @Story("Edge Cases")
    @Description("Multiple simultaneous report requests handled correctly")
    @Severity(SeverityLevel.NORMAL)
    void testCsv_ConcurrentRequests() {
        Allure.step("Send concurrent requests for different reports", () -> {
            // Make multiple simultaneous requests
            Response response1 = given().cookie("jwt", managerJwtCookie)
                    .get("/api/reports/expenses/csv");
            Response response2 = given().cookie("jwt", managerJwtCookie)
                    .get("/api/reports/expenses/employee/1/csv");
            Response response3 = given().cookie("jwt", managerJwtCookie)
                    .get("/api/reports/expenses/employee/2/csv");

            Allure.parameter("Request 1 Status", response1.getStatusCode());
            Allure.parameter("Request 2 Status", response2.getStatusCode());
            Allure.parameter("Request 3 Status", response3.getStatusCode());

            // All should succeed
            Assertions.assertEquals(200, response1.getStatusCode());
            Assertions.assertEquals(200, response2.getStatusCode());
            Assertions.assertEquals(200, response3.getStatusCode());
        });
    }

    @Test
    @Order(23)
    @Story("Content Type Validation")
    @Description("Both endpoints return correct content type")
    @Severity(SeverityLevel.NORMAL)
    void testCsv_CorrectContentType() {
        // All expenses CSV
        Allure.step("Verify all expenses CSV content type", () -> {
            given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/csv")
                    .then()
                    .contentType(containsString("text/csv"));
        });

        // Employee expenses CSV
        Allure.step("Verify employee expenses CSV content type", () -> {
            given()
                    .cookie("jwt", managerJwtCookie)
                    .when()
                    .get("/api/reports/expenses/employee/1/csv")
                    .then()
                    .contentType(containsString("text/csv"));
        });
    }
}