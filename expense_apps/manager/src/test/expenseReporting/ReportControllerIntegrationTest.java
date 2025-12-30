package expenseReview;

import com.revature.api.ReportController;
import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import com.revature.repository.ExpenseRepository;
import com.revature.service.ExpenseService;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ReportControllerIntegrationTest {

    private static DatabaseConnection testDbConnection;
    private static Javalin app;
    private static int port;

    @BeforeAll
    static void setupServerAndDatabase() throws SQLException {
        // Initialize test database
        testDbConnection = new DatabaseConnection("src/test/resources/test.db");

        // Create tables
        createTables();

        // Setup repositories and services
        ExpenseRepository expenseRepo = new ExpenseRepository(testDbConnection);
        ApprovalRepository approvalRepo = new ApprovalRepository(testDbConnection);
        ExpenseService expenseService = new ExpenseService(expenseRepo, approvalRepo);
        ReportController reportController = new ReportController(expenseService);

        // Setup Javalin app with report routes
        app = Javalin.create();

        // Configure report routes
        app.get("/api/reports/expenses/csv", reportController::generateAllExpensesReport);
        app.get("/api/reports/expenses/employee/{employeeId}/csv", reportController::generateEmployeeExpensesReport);
        app.get("/api/reports/expenses/category/{category}/csv", reportController::generateCategoryExpensesReport);
        app.get("/api/reports/expenses/daterange/csv", reportController::generateDateRangeExpensesReport);
        app.get("/api/reports/expenses/pending/csv", reportController::generatePendingExpensesReport);

        // Start server
        app.start(0);
        port = app.port();

        // Configure RestAssured
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    @BeforeEach
    void setupTestData() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Clear existing data
            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");
            stmt.execute("DELETE FROM users");

            // Insert test users
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (1, 'john.doe', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (2, 'jane.smith', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (3, 'manager.bob', 'pass123', 'manager')");

            // Insert test expenses with various categories and dates
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (1, 1, 150.00, 'Travel - Conference', '2025-11-15')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (2, 1, 85.50, 'Office Supplies', '2025-12-01')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (3, 2, 300.00, 'Travel - Client Meeting', '2025-12-10')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (4, 2, 45.75, 'Meals', '2025-12-15')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (5, 1, 120.00, 'Software License', '2025-12-20')");

            // Insert approvals with different statuses
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (1, 1, 'approved', 3, 'Good', '2025-11-16 10:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (2, 2, 'pending', NULL, NULL, NULL)");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (3, 3, 'approved', 3, 'Approved', '2025-12-11 09:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (4, 4, 'denied', 3, 'Too high', '2025-12-16 14:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (5, 5, 'pending', NULL, NULL, NULL)");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        // Clean up database
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");
            stmt.execute("DELETE FROM users");
        }

        // Stop server
        if (app != null) {
            app.stop();
        }
    }

    private static void createTables() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    role TEXT NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    description TEXT NOT NULL,
                    date TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS approvals (
                    id INTEGER PRIMARY KEY,
                    expense_id INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    reviewer INTEGER,
                    comment TEXT,
                    review_date TEXT,
                    FOREIGN KEY (expense_id) REFERENCES expenses(id),
                    FOREIGN KEY (reviewer) REFERENCES users(id)
                )
            """);
        }
    }

    // ==========================================
    // GET /api/reports/expenses/csv - Tests
    // ==========================================

    @Test
    @Order(1)
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

        // Validate CSV structure
        assertThat(csv, containsString("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date"));
        assertThat(csv, containsString("john.doe"));
        assertThat(csv, containsString("jane.smith"));
        assertThat(csv, containsString("150.0"));
        assertThat(csv, containsString("300.0"));

        // Count number of data rows (should be 5 expenses + 1 header)
        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(6));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/reports/expenses/csv - CSV contains correct headers")
    void generateAllExpensesReport_hasCorrectHeaders() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();
        String headerLine = csv.split("\n")[0];

        assertThat(headerLine, equalTo("Expense ID,Employee,Amount,Description,Date,Status,Reviewer,Comment,Review Date"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/reports/expenses/csv - CSV contains all expense data")
    void generateAllExpensesReport_containsAllData() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();

        // Check for specific expense data
        assertThat(csv, containsString("Travel - Conference"));
        assertThat(csv, containsString("Office Supplies"));
        assertThat(csv, containsString("Travel - Client Meeting"));
        assertThat(csv, containsString("Meals"));
        assertThat(csv, containsString("Software License"));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/reports/expenses/csv - CSV includes approval statuses")
    void generateAllExpensesReport_includesApprovalStatuses() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();

        assertThat(csv, containsString("approved"));
        assertThat(csv, containsString("pending"));
        assertThat(csv, containsString("denied"));
    }

    // ==========================================
    // GET /api/reports/expenses/employee/{employeeId}/csv - Tests
    // ==========================================

    @Test
    @Order(5)
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns CSV for specific employee")
    void generateEmployeeExpensesReport_returnsEmployeeSpecificCSV() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/employee/1/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("employee_1_expenses_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();

        // Should contain employee 1's expenses only
        assertThat(csv, containsString("john.doe"));
        assertThat(csv, not(containsString("jane.smith")));

        // Count rows (3 expenses for employee 1 + 1 header)
        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(4));
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns error for invalid employee ID")
    void generateEmployeeExpensesReport_returns400ForInvalidId() {
        // Javalin returns 500 when path param conversion fails
        given()
                .when()
                .get("/api/reports/expenses/employee/invalid/csv")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/reports/expenses/employee/{employeeId}/csv - Returns empty CSV for employee with no expenses")
    void generateEmployeeExpensesReport_returnsEmptyForNoExpenses() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/employee/999/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .extract().response();

        String csv = response.getBody().asString();
        String[] lines = csv.split("\n");

        // Should only have header
        assertThat(lines.length, equalTo(1));
    }

    // ==========================================
    // GET /api/reports/expenses/category/{category}/csv - Tests
    // ==========================================

    @Test
    @Order(8)
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Returns CSV for specific category")
    void generateCategoryExpensesReport_returnsCategorySpecificCSV() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/category/Travel/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("category_Travel_expenses_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();

        // Should contain both Travel expenses
        assertThat(csv, containsString("Travel - Conference"));
        assertThat(csv, containsString("Travel - Client Meeting"));
        assertThat(csv, not(containsString("Office Supplies")));
        assertThat(csv, not(containsString("Meals")));

        // Count rows (2 travel expenses + 1 header)
        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(3));
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Sanitizes category name in filename")
    void generateCategoryExpensesReport_sanitizesFilename() {
        given()
                .when()
                .get("/api/reports/expenses/category/Test%20Category%20%2F%20Special/csv")
                .then()
                .statusCode(200)
                // URL encoding: %20 = space, %2F = slash
                // The actual filename will have URL-encoded characters
                .header("Content-Disposition", containsString("expenses_report.csv"));
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Returns 400 for empty category")
    void generateCategoryExpensesReport_returns400ForEmptyCategory() {
        given()
                .when()
                .get("/api/reports/expenses/category/ /csv")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(11)
    @DisplayName("GET /api/reports/expenses/category/{category}/csv - Case insensitive search")
    void generateCategoryExpensesReport_caseInsensitiveSearch() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/category/travel/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();

        // Should find Travel expenses even with lowercase
        assertThat(csv, containsString("Travel"));
        String[] lines = csv.split("\n");
        assertThat(lines.length, greaterThan(1));
    }

    // ==========================================
    // GET /api/reports/expenses/daterange/csv - Tests
    // ==========================================

    @Test
    @Order(12)
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns CSV for date range")
    void generateDateRangeExpensesReport_returnsDateRangeCSV() {
        Response response = given()
                .queryParam("startDate", "2025-12-01")
                .queryParam("endDate", "2025-12-15")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("expenses_2025-12-01_to_2025-12-15_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();

        // Should contain expenses within date range
        assertThat(csv, containsString("Office Supplies")); // 2025-12-01
        assertThat(csv, containsString("Travel - Client Meeting")); // 2025-12-10
        assertThat(csv, containsString("Meals")); // 2025-12-15

        // Should NOT contain expense outside range
        assertThat(csv, not(containsString("Travel - Conference"))); // 2025-11-15
        assertThat(csv, not(containsString("Software License"))); // 2025-12-20

        // Count rows (3 expenses in range + 1 header)
        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(4));
    }

    @Test
    @Order(13)
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 when missing startDate")
    void generateDateRangeExpensesReport_returns400WhenMissingStartDate() {
        given()
                .queryParam("endDate", "2025-12-31")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(14)
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 when missing endDate")
    void generateDateRangeExpensesReport_returns400WhenMissingEndDate() {
        given()
                .queryParam("startDate", "2025-12-01")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(15)
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns 400 for invalid date format")
    void generateDateRangeExpensesReport_returns400ForInvalidDateFormat() {
        given()
                .queryParam("startDate", "12/01/2025")
                .queryParam("endDate", "12/31/2025")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(16)
    @DisplayName("GET /api/reports/expenses/daterange/csv - Returns empty CSV when no expenses in range")
    void generateDateRangeExpensesReport_returnsEmptyForNoExpensesInRange() {
        Response response = given()
                .queryParam("startDate", "2025-01-01")
                .queryParam("endDate", "2025-01-31")
                .when()
                .get("/api/reports/expenses/daterange/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();
        String[] lines = csv.split("\n");

        // Should only have header
        assertThat(lines.length, equalTo(1));
    }

    // ==========================================
    // GET /api/reports/expenses/pending/csv - Tests
    // ==========================================

    @Test
    @Order(17)
    @DisplayName("GET /api/reports/expenses/pending/csv - Returns CSV with pending expenses only")
    void generatePendingExpensesReport_returnsPendingOnlyCSV() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/pending/csv")
                .then()
                .statusCode(200)
                .contentType("text/csv")
                .header("Content-Disposition", containsString("pending_expenses_report.csv"))
                .extract().response();

        String csv = response.getBody().asString();

        // Should contain only pending expenses
        assertThat(csv, containsString("Office Supplies")); // pending
        assertThat(csv, containsString("Software License")); // pending
        assertThat(csv, containsString("pending"));

        // Should NOT contain approved or denied
        assertThat(csv, not(containsString("Travel - Conference"))); // approved
        assertThat(csv, not(containsString("Meals"))); // denied

        // Count rows (2 pending expenses + 1 header)
        String[] lines = csv.split("\n");
        assertThat(lines.length, equalTo(3));
    }

    @Test
    @Order(18)
    @DisplayName("GET /api/reports/expenses/pending/csv - Pending expenses have no reviewer")
    void generatePendingExpensesReport_pendingHaveNoReviewer() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/pending/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();
        String[] lines = csv.split("\n");

        // Check that pending expenses have empty reviewer and comment fields
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];

            // Split by comma, but handle quoted fields properly
            String[] fields = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            // Status field should be "pending" (index 5)
            if (fields.length > 5) {
                assertThat(fields[5], equalTo("pending"));

                // Reviewer field (index 6) should be empty
                if (fields.length > 6) {
                    assertThat(fields[6], equalTo(""));
                }
            }
        }
    }

    // ==========================================
    // CSV Format Validation Tests
    // ==========================================

    @Test
    @Order(19)
    @DisplayName("CSV properly escapes commas in descriptions")
    void csvReport_escapesCommasInDescriptions() throws SQLException {
        // Add expense with comma in description
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (99, 1, 50.0, 'Test, with, commas', '2025-12-25')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status) VALUES (99, 99, 'pending')");
        }

        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();

        // Should be properly quoted
        assertThat(csv, containsString("\"Test, with, commas\""));
    }

    @Test
    @Order(20)
    @DisplayName("CSV properly escapes quotes in comments")
    void csvReport_escapesQuotesInComments() throws SQLException {
        // Add expense with quotes in comment
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (98, 1, 25.0, 'Test Expense', '2025-12-26')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) " +
                    "VALUES (98, 98, 'approved', 3, 'Said \"looks good\"', '2025-12-27 10:00:00')");
        }

        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();

        // Should be properly escaped with double quotes
        assertThat(csv, containsString("\"Said \"\"looks good\"\"\""));
    }

    @Test
    @Order(21)
    @DisplayName("CSV includes all required fields for each expense")
    void csvReport_includesAllRequiredFields() {
        Response response = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        String csv = response.getBody().asString();
        String[] lines = csv.split("\n");

        // Check each data row has correct number of fields
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];

            // Count commas (should be 8 for 9 fields)
            // Handle quoted fields properly
            int fieldCount = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1).length;
            assertThat("Line " + i + " should have 9 fields", fieldCount, equalTo(9));
        }
    }

    @Test
    @Order(22)
    @DisplayName("Multiple report endpoints work independently")
    void multipleReportEndpoints_workIndependently() {
        // Generate all expenses report
        Response allExpenses = given()
                .when()
                .get("/api/reports/expenses/csv")
                .then()
                .statusCode(200)
                .extract().response();

        // Generate pending expenses report
        Response pendingExpenses = given()
                .when()
                .get("/api/reports/expenses/pending/csv")
                .then()
                .statusCode(200)
                .extract().response();

        // They should have different content
        String allCsv = allExpenses.getBody().asString();
        String pendingCsv = pendingExpenses.getBody().asString();

        assertThat(allCsv, not(equalTo(pendingCsv)));
        assertThat(allCsv.split("\n").length, greaterThan(pendingCsv.split("\n").length));
    }
}