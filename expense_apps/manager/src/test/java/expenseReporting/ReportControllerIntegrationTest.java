package expenseReporting;

import com.revature.api.ReportController;
import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import com.revature.repository.ExpenseRepository;
import com.revature.service.ExpenseService;
import io.javalin.Javalin;
import io.qameta.allure.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Epic("Expense Reporting")
@Feature("CSV Report Generation")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportControllerIntegrationTest {

    private static DatabaseConnection testDbConnection;
    private static Javalin app;
    private static int port;

    @BeforeAll
    @Step("Initialize Test Server and SQLite Database")
    static void setupServerAndDatabase() throws SQLException {
        testDbConnection = new DatabaseConnection("src/test/resources/test.db");
        createTables();

        ExpenseRepository expenseRepo = new ExpenseRepository(testDbConnection);
        ApprovalRepository approvalRepo = new ApprovalRepository(testDbConnection);
        ExpenseService expenseService = new ExpenseService(expenseRepo, approvalRepo);
        ReportController reportController = new ReportController(expenseService);

        app = Javalin.create();
        app.get("/api/reports/expenses/csv", reportController::generateAllExpensesReport);
        app.get("/api/reports/expenses/employee/{employeeId}/csv", reportController::generateEmployeeExpensesReport);
        app.get("/api/reports/expenses/category/{category}/csv", reportController::generateCategoryExpensesReport);
        app.get("/api/reports/expenses/daterange/csv", reportController::generateDateRangeExpensesReport);
        app.get("/api/reports/expenses/pending/csv", reportController::generatePendingExpensesReport);

        app.start(0);
        port = app.port();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @BeforeEach
    @Step("Reset Database and Seed Test Data")
    void setupTestData() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");
            stmt.execute("DELETE FROM users");

            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (1, 'john.doe', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (2, 'jane.smith', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (3, 'manager.bob', 'pass123', 'manager')");

            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (1, 1, 150.00, 'Travel - Conference', '2025-11-15')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (2, 1, 85.50, 'Office Supplies', '2025-12-01')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (3, 2, 300.00, 'Travel - Client Meeting', '2025-12-10')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (4, 2, 45.75, 'Meals', '2025-12-15')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (5, 1, 120.00, 'Software License', '2025-12-20')");

            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (1, 1, 'approved', 3, 'Good', '2025-11-16 10:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (2, 2, 'pending', NULL, NULL, NULL)");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (3, 3, 'approved', 3, 'Approved', '2025-12-11 09:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (4, 4, 'denied', 3, 'Too high', '2025-12-16 14:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (5, 5, 'pending', NULL, NULL, NULL)");
        }
    }

    @AfterAll
    @Step("Shut down Server")
    static void tearDown() throws SQLException {
        if (app != null) {
            app.stop();
        }
    }

    @Step("Execute Schema Creation")
    private static void createTables() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username TEXT NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS expenses (id INTEGER PRIMARY KEY, user_id INTEGER NOT NULL, amount REAL NOT NULL, description TEXT NOT NULL, date TEXT NOT NULL, FOREIGN KEY (user_id) REFERENCES users(id))");
            stmt.execute("CREATE TABLE IF NOT EXISTS approvals (id INTEGER PRIMARY KEY, expense_id INTEGER NOT NULL, status TEXT NOT NULL, reviewer INTEGER, comment TEXT, review_date TEXT, FOREIGN KEY (expense_id) REFERENCES expenses(id), FOREIGN KEY (reviewer) REFERENCES users(id))");
        }
    }

    @Step("Verify CSV contains expected content: {expectedSubstring}")
    private void verifyCsvContent(String csv, String expectedSubstring) {
        assertThat(csv, containsString(expectedSubstring));
    }

    // ==========================================
    // GET /api/reports/expenses/csv - Tests
    // ==========================================

    @Test
    @Order(1)
    @Story("General Expenses Report")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that the full expense report returns all 5 records and correct headers.")
    @DisplayName("GET /api/reports/expenses/csv - Returns CSV with all expenses")
    void generateAllExpensesReport_returnsCompleteCSV() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("all_expenses_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();
        verifyCsvContent(csv, "Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date");
        verifyCsvContent(csv, "john.doe");

        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(6));
    }

    @Test
    @Order(2)
    @Story("General Expenses Report")
    @DisplayName("GET /api/reports/expenses/csv - CSV contains correct headers")
    void generateAllExpensesReport_hasCorrectHeaders() {
        Response response = given().get("/api/reports/expenses/csv");
        String headerLine = response.getBody().asString().split("\n")[0];
        assertThat(headerLine, equalTo("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date"));
    }

    @Test
    @Order(3)
    @Story("General Expenses Report")
    @DisplayName("GET /api/reports/expenses/csv - CSV contains all expense data")
    void generateAllExpensesReport_containsAllData() {
        String csv = given().get("/api/reports/expenses/csv").asString();
        verifyCsvContent(csv, "Travel - Conference");
        verifyCsvContent(csv, "Software License");
    }

    @Test
    @Order(4)
    @Story("General Expenses Report")
    @DisplayName("GET /api/reports/expenses/csv - CSV includes approval statuses")
    void generateAllExpensesReport_includesApprovalStatuses() {
        String csv = given().get("/api/reports/expenses/csv").asString();
        verifyCsvContent(csv, "approved");
        verifyCsvContent(csv, "pending");
        verifyCsvContent(csv, "denied");
    }

    // ==========================================
    // GET /api/reports/expenses/employee/{employeeId}/csv - Tests
    // ==========================================

    @Test
    @Order(5)
    @Story("Employee Specific Report")
    @Description("Verify report filtering by Employee ID.")
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns CSV for specific employee")
    void generateEmployeeExpensesReport_returnsEmployeeSpecificCSV() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(200)
                .header("Content-Disposition", containsString("employee_1_expenses_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();
        verifyCsvContent(csv, "john.doe");
        assertThat(csv, not(containsString("jane.smith")));
    }

    @Test
    @Order(6)
    @Story("Employee Specific Report")
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns error for invalid employee ID")
    void generateEmployeeExpensesReport_returns400ForInvalidId() {
        given()
                .when()
                .get("/api/reports/expenses/employee/invalid/csv")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(7)
    @Story("Employee Specific Report")
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns empty CSV for employee with no expenses")
    void generateEmployeeExpensesReport_returnsEmptyForNoExpenses() {
        String csv = given().get("/api/reports/expenses/employee/999/csv").asString();
        assertThat(csv.split("\n").length, equalTo(1));
    }

    // ==========================================
    // GET /api/reports/expenses/category/{category}/csv - Tests
    // ==========================================

    @Test
    @Order(8)
    @Story("Category Filtered Report")
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Returns CSV for specific category")
    void generateCategoryExpensesReport_returnsCategorySpecificCSV() {
        Response response = given().get("/api/reports/expenses/category/Travel/csv");
        String csv = response.getBody().asString();
        verifyCsvContent(csv, "Travel - Conference");
        assertThat(csv, not(containsString("Office Supplies")));
    }

    @Test
    @Order(9)
    @Story("Category Filtered Report")
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Sanitizes category name in filename")
    void generateCategoryExpensesReport_sanitizesFilename() {
        given()
                .get("/api/reports/expenses/category/Test%20Category%20%2F%20Special/csv")
                .then()
                .header("Content-Disposition", containsString("expenses_report.csv"));
    }

    @Test
    @Order(10)
    @Story("Category Filtered Report")
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Returns 400 for empty category")
    void generateCategoryExpensesReport_returns400ForEmptyCategory() {
        given().get("/api/reports/expenses/category/ /csv").then().statusCode(400);
    }

    @Test
    @Order(11)
    @Story("Category Filtered Report")
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Case insensitive search")
    void generateCategoryExpensesReport_caseInsensitiveSearch() {
        String csv = given().get("/api/reports/expenses/category/travel/csv").asString();
        verifyCsvContent(csv, "Travel");
    }

    // ==========================================
    // GET /api/reports/expenses/daterange/csv - Tests
    // ==========================================

    @Test
    @Order(12)
    @Story("Date Range Filtered Report")
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns CSV for date range")
    void generateDateRangeExpensesReport_returnsDateRangeCSV() {
        Response response = given()
                .queryParam("startDate", "2025-12-01")
                .queryParam("endDate", "2025-12-15")
                .get("/api/reports/expenses/daterange/csv");

        String csv = response.getBody().asString();
        verifyCsvContent(csv, "Office Supplies");
        assertThat(csv, not(containsString("Travel - Conference")));
    }

    @Test
    @Order(13)
    @Story("Date Range Filtered Report")
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 when missing startDate")
    void generateDateRangeExpensesReport_returns400WhenMissingStartDate() {
        given().queryParam("endDate", "2025-12-31").get("/api/reports/expenses/daterange/csv").then().statusCode(400);
    }

    @Test
    @Order(14)
    @Story("Date Range Filtered Report")
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 when missing endDate")
    void generateDateRangeExpensesReport_returns400WhenMissingEndDate() {
        given().queryParam("startDate", "2025-12-01").get("/api/reports/expenses/daterange/csv").then().statusCode(400);
    }

    @Test
    @Order(15)
    @Story("Date Range Filtered Report")
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 for invalid date format")
    void generateDateRangeExpensesReport_returns400ForInvalidDateFormat() {
        given().queryParam("startDate", "12/01/2025").queryParam("endDate", "12/31/2025").get("/api/reports/expenses/daterange/csv").then().statusCode(400);
    }

    @Test
    @Order(16)
    @Story("Date Range Filtered Report")
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns empty CSV when no expenses in range")
    void generateDateRangeExpensesReport_returnsEmptyForNoExpensesInRange() {
        String csv = given().queryParam("startDate", "2025-01-01").queryParam("endDate", "2025-01-31").get("/api/reports/expenses/daterange/csv").asString();
        assertThat(csv.split("\n").length, equalTo(1));
    }

    // ==========================================
    // GET /api/reports/expenses/pending/csv - Tests
    // ==========================================

    @Test
    @Order(17)
    @Story("Pending Expenses Report")
    @DisplayName("GET /api/reports/expenses/pending/csv - Returns CSV with pending expenses only")
    void generatePendingExpensesReport_returnsPendingOnlyCSV() {
        String csv = given().get("/api/reports/expenses/pending/csv").asString();
        verifyCsvContent(csv, "Office Supplies");
        assertThat(csv, not(containsString("approved")));
    }

    @Test
    @Order(18)
    @Story("Pending Expenses Report")
    @DisplayName("GET /api/reports/expenses/pending/csv - Pending expenses have no reviewer")
    void generatePendingExpensesReport_pendingHaveNoReviewer() {
        String csv = given().get("/api/reports/expenses/pending/csv").asString();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String[] fields = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            assertThat(fields[5], equalTo("pending"));
            if (fields.length > 6) assertThat(fields[6], equalTo(""));
        }
    }

    // ==========================================
    // CSV Format Validation Tests
    // ==========================================

    @Test
    @Order(19)
    @Story("CSV Formatting & Integrity")
    @DisplayName("CSV properly escapes commas in descriptions")
    void csvReport_escapesCommasInDescriptions() throws SQLException {
        try (Connection conn = testDbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (99, 1, 50.0, 'Test, with, commas', '2025-12-25')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status) VALUES (99, 99, 'pending')");
        }
        String csv = given().get("/api/reports/expenses/csv").asString();
        verifyCsvContent(csv, "\"Test, with, commas\"");
    }

    @Test
    @Order(20)
    @Story("CSV Formatting & Integrity")
    @DisplayName("CSV properly escapes quotes in comments")
    void csvReport_escapesQuotesInComments() throws SQLException {
        try (Connection conn = testDbConnection.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (98, 1, 25.0, 'Test Expense', '2025-12-26')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (98, 98, 'approved', 3, 'Said \"looks good\"', '2025-12-27 10:00:00')");
        }
        String csv = given().get("/api/reports/expenses/csv").asString();
        verifyCsvContent(csv, "\"Said \"\"looks good\"\"\"");
    }

    @Test
    @Order(21)
    @Story("CSV Formatting & Integrity")
    @DisplayName("CSV includes all required fields for each expense")
    void csvReport_includesAllRequiredFields() {
        String csv = given().get("/api/reports/expenses/csv").asString();
        String[] lines = csv.split("\n");
        for (int i = 1; i < lines.length; i++) {
            int fieldCount = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1).length;
            assertThat(fieldCount, equalTo(9));
        }
    }

    @Test
    @Order(22)
    @Story("CSV Formatting & Integrity")
    @DisplayName("Multiple report endpoints work independently")
    void multipleReportEndpoints_workIndependently() {
        String allCsv = given().get("/api/reports/expenses/csv").asString();
        String pendingCsv = given().get("/api/reports/expenses/pending/csv").asString();
        assertThat(allCsv, not(equalTo(pendingCsv)));
    }
}