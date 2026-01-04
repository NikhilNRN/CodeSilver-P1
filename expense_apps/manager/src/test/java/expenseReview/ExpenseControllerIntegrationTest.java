package expenseReview;

import com.revature.api.AuthenticationMiddleware;
import com.revature.api.ExpenseController;
import com.revature.repository.*;
import com.revature.service.AuthenticationService;
import com.revature.service.ExpenseService;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

@Epic("Expense Management")
@Feature("Expense Review and Approval")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExpenseControllerIntegrationTest {

    private static DatabaseConnection testDbConnection;
    private static Javalin app;
    private static int port;
    private static String managerJwtToken;

    @BeforeAll
    static void setupServerAndDatabase() throws SQLException {
        testDbConnection = new DatabaseConnection("src/test/resources/test.db");
        createTables();
        insertManagerUser();

        UserRepository userRepo = new UserRepository(testDbConnection);
        ExpenseRepository expenseRepo = new ExpenseRepository(testDbConnection);
        ApprovalRepository approvalRepo = new ApprovalRepository(testDbConnection);

        AuthenticationService authService = new AuthenticationService(userRepo);
        ExpenseService expenseService = new ExpenseService(expenseRepo, approvalRepo);

        User manager = userRepo.findById(3).orElseThrow();
        managerJwtToken = authService.createJwtToken(manager);

        ExpenseController expenseController = new ExpenseController(expenseService);
        AuthenticationMiddleware authMiddleware = new AuthenticationMiddleware(authService);

        app = Javalin.create();
        app.before("/api/expenses/*/approve", authMiddleware.validateManager());
        app.before("/api/expenses/*/deny", authMiddleware.validateManager());

        app.get("/api/expenses/pending", expenseController::getPendingExpenses);
        app.post("/api/expenses/{expenseId}/approve", expenseController::approveExpense);
        app.post("/api/expenses/{expenseId}/deny", expenseController::denyExpense);
        app.get("/api/expenses", expenseController::getAllExpenses);
        app.get("/api/expenses/employee/{employeeId}", expenseController::getExpensesByEmployee);

        app.start(0);
        port = app.port();

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @BeforeEach
    void setupTestData() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");

            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (1, 1, 100.50, 'Travel', '2025-12-01')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (2, 1, 250.75, 'Office Supplies', '2025-12-10')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (3, 2, 500.00, 'Travel', '2025-12-15')");
            stmt.execute("INSERT INTO expenses (id, user_id, amount, description, date) VALUES (4, 2, 75.25, 'Meals', '2025-12-20')");

            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (1, 1, 'pending', NULL, NULL, NULL)");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (2, 2, 'approved', 3, 'Approved', '2025-12-11 10:00:00')");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (3, 3, 'pending', NULL, NULL, NULL)");
            stmt.execute("INSERT INTO approvals (id, expense_id, status, reviewer, comment, review_date) VALUES (4, 4, 'denied', 3, 'Exceeds limit', '2025-12-21 09:00:00')");
        }
    }

    @AfterAll
    static void tearDown() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");
            stmt.execute("DELETE FROM users");
        }

        if (app != null) app.stop();
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

    private static void insertManagerUser() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (1, 'employee1', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (2, 'employee2', 'pass123', 'employee')");
            stmt.execute("INSERT INTO users (id, username, password, role) VALUES (3, 'manager1', 'pass123', 'manager')");
        } catch (SQLException e) {
            // Users already exist
        }
    }

    // ==========================================
    // GET /api/expenses/pending - Tests
    // ==========================================

    @Test
    @Order(1)
    @Epic("Expense Review")
    @Feature("Pending Expenses")
    @Story("Retrieve pending expenses for manager review")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("GET /api/expenses/pending - Returns pending expenses")
    void getPendingExpenses_returnsOnlyPendingExpenses() {
        given()
                .when()
                .get("/api/expenses/pending")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("success", equalTo(true))
                .body("data", notNullValue())
                .body("count", equalTo(2))
                .body("data.size()", equalTo(2));
    }

    @Test
    @Order(2)
    @Epic("Expense Review")
    @Feature("Pending Expenses")
    @Story("Validate structure of pending expenses data")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("GET /api/expenses/pending - Validates expense data structure")
    void getPendingExpenses_returnsCorrectDataStructure() {
        given()
                .when()
                .get("/api/expenses/pending")
                .then()
                .statusCode(200)
                .body("data[0].expense", notNullValue())
                .body("data[0].expense.id", notNullValue())
                .body("data[0].expense.userId", notNullValue())
                .body("data[0].expense.amount", notNullValue())
                .body("data[0].expense.description", notNullValue())
                .body("data[0].expense.date", notNullValue())
                .body("data[0].user", notNullValue())
                .body("data[0].user.username", notNullValue())
                .body("data[0].approval", notNullValue())
                .body("data[0].approval.status", equalTo("pending"));
    }

    @Test
    @Order(3)
    @Epic("Expense Review")
    @Feature("Pending Expenses")
    @Story("Handle empty pending expenses scenario")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/expenses/pending - Returns empty list when no pending")
    void getPendingExpenses_returnsEmptyWhenNoPending() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE approvals SET status = 'approved' WHERE status = 'pending'");
        }

        given()
                .when()
                .get("/api/expenses/pending")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("count", equalTo(0))
                .body("data", empty());
    }

    // ==========================================
    // POST /api/expenses/{expenseId}/approve - Tests
    // ==========================================

    @Test
    @Order(4)
    @Epic("Expense Approval")
    @Feature("Approve Expense")
    @Story("Manager approves an expense with JWT authentication")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /api/expenses/{expenseId}/approve - Successfully approves with JWT")
    void approveExpense_successfullyApprovesWithJwt() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("comment", "Looks good");

        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/expenses/1/approve")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("approved"));
    }

    @Test
    @Order(5)
    @Epic("Expense Approval")
    @Feature("Approve Expense")
    @Story("Fail approval if manager is not authenticated")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/expenses/{expenseId}/approve - Fails without authentication")
    void approveExpense_failsWithoutAuth() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("comment", "Looks good");

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/expenses/1/approve")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(6)
    @Epic("Expense Approval")
    @Feature("Approve Expense")
    @Story("Manager can approve without a comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/expenses/{expenseId}/approve - Approves without comment")
    void approveExpense_worksWithoutComment() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/1/approve")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @Order(7)
    @Epic("Expense Approval")
    @Feature("Approve Expense")
    @Story("Attempt to approve non-existent expense")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/expenses/{expenseId}/approve - Returns 404 for non-existent expense")
    void approveExpense_returns404ForNonExistentExpense() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/9999/approve")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    @Epic("Expense Approval")
    @Feature("Approve Expense")
    @Story("Invalid expense ID returns error")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("POST /api/expenses/{expenseId}/approve - Returns error for invalid expense ID")
    void approveExpense_returns400ForInvalidExpenseId() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/invalid/approve")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    // ==========================================
    // POST /api/expenses/{expenseId}/deny - Tests
    // ==========================================

    @Test
    @Order(9)
    @Epic("Expense Approval")
    @Feature("Deny Expense")
    @Story("Manager denies an expense with JWT authentication")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("POST /api/expenses/{expenseId}/deny - Successfully denies with JWT")
    void denyExpense_successfullyDeniesWithJwt() {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("comment", "Does not meet policy");

        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/expenses/1/deny")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("message", containsString("denied"));
    }

    @Test
    @Order(10)
    @Epic("Expense Approval")
    @Feature("Deny Expense")
    @Story("Fail denial if manager is not authenticated")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/expenses/{expenseId}/deny - Fails without authentication")
    void denyExpense_failsWithoutAuth() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/1/deny")
                .then()
                .statusCode(401);
    }

    @Test
    @Order(11)
    @Epic("Expense Approval")
    @Feature("Deny Expense")
    @Story("Manager can deny without a comment")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("POST /api/expenses/{expenseId}/deny - Denies without comment")
    void denyExpense_worksWithoutComment() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/1/deny")
                .then()
                .statusCode(200)
                .body("success", equalTo(true));
    }

    @Test
    @Order(12)
    @Epic("Expense Approval")
    @Feature("Deny Expense")
    @Story("Attempt to deny non-existent expense")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("POST /api/expenses/{expenseId}/deny - Returns 404 for non-existent expense")
    void denyExpense_returns404ForNonExistentExpense() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/9999/deny")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(13)
    @Epic("Expense Approval")
    @Feature("Deny Expense")
    @Story("Invalid expense ID returns error")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("POST /api/expenses/{expenseId}/deny - Returns error for invalid expense ID")
    void denyExpense_returns400ForInvalidExpenseId() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/expenses/invalid/deny")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    // ==========================================
    // GET /api/expenses - Tests
    // ==========================================

    @Test
    @Order(14)
    @Epic("Expense Review")
    @Feature("All Expenses")
    @Story("Retrieve all expenses")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/expenses - Returns all expenses")
    void getAllExpenses_returnsAllExpenses() {
        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("count", equalTo(4))
                .body("data.size()", equalTo(4));
    }

    @Test
    @Order(15)
    @Epic("Expense Review")
    @Feature("All Expenses")
    @Story("Validate that expenses include all statuses")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/expenses - Returns expenses with all statuses")
    void getAllExpenses_includesAllStatuses() {
        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("data.approval.status", hasItems("pending", "approved", "denied"));
    }

    @Test
    @Order(16)
    @Epic("Expense Review")
    @Feature("All Expenses")
    @Story("Return empty list when no expenses exist")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("GET /api/expenses - Returns empty list when no expenses exist")
    void getAllExpenses_returnsEmptyWhenNoExpenses() throws SQLException {
        try (Connection conn = testDbConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM approvals");
            stmt.execute("DELETE FROM expenses");
        }

        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("count", equalTo(0))
                .body("data", empty());
    }

    // ==========================================
    // GET /api/expenses/employee/{employeeId} - Tests
    // ==========================================

    @Test
    @Order(17)
    @Epic("Expense Review")
    @Feature("Employee Expenses")
    @Story("Retrieve all expenses for a specific employee")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("GET /api/expenses/employee/{employeeId} - Returns expenses for employee")
    void getExpensesByEmployee_returnsEmployeeExpenses() {
        given()
                .when()
                .get("/api/expenses/employee/1")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("employeeId", equalTo(1))
                .body("count", equalTo(2))
                .body("data.size()", equalTo(2))
                .body("data.expense.userId", everyItem(equalTo(1)));
    }

    @Test
    @Order(18)
    @Epic("Expense Review")
    @Feature("Employee Expenses")
    @Story("Return empty list when employee has no expenses")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("GET /api/expenses/employee/{employeeId} - Returns empty for no expenses")
    void getExpensesByEmployee_returnsEmptyForNoExpenses() {
        given()
                .when()
                .get("/api/expenses/employee/999")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("employeeId", equalTo(999))
                .body("count", equalTo(0))
                .body("data", empty());
    }

    @Test
    @Order(19)
    @Epic("Expense Review")
    @Feature("Employee Expenses")
    @Story("Invalid employee ID returns error")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("GET /api/expenses/employee/{employeeId} - Returns error for invalid ID")
    void getExpensesByEmployee_returns400ForInvalidId() {
        given()
                .when()
                .get("/api/expenses/employee/invalid")
                .then()
                .statusCode(anyOf(equalTo(400), equalTo(500)));
    }

    @Test
    @Order(20)
    @Epic("Expense Review")
    @Feature("Employee Expenses")
    @Story("Validate amounts for employee expenses")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("GET /api/expenses/employee/{employeeId} - Validates expense amounts")
    void getExpensesByEmployee_validatesExpenseAmounts() {
        given()
                .when()
                .get("/api/expenses/employee/1")
                .then()
                .statusCode(200)
                .body("data[0].expense.amount", isOneOf(100.5f, 250.75f));
    }

    // ==========================================
    // Database Persistence Tests
    // ==========================================

    @Test
    @Order(21)
    @Epic("Expense Approval")
    @Feature("Database Persistence")
    @Story("Approval persists correctly in the database")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Approval persists in REAL database")
    void approvalPersistsInDatabase() throws SQLException {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{\"comment\": \"Test persistence\"}")
                .when()
                .post("/api/expenses/3/approve")
                .then()
                .statusCode(200);

        try (Connection conn = testDbConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT status, comment, reviewer FROM approvals WHERE expense_id = 3")) {

            Assertions.assertTrue(rs.next(), "Approval should exist in database");
            Assertions.assertEquals("approved", rs.getString("status"));
            Assertions.assertEquals("Test persistence", rs.getString("comment"));
            Assertions.assertEquals(3, rs.getInt("reviewer"));
        }
    }

    @Test
    @Order(22)
    @Epic("Expense Approval")
    @Feature("Database Persistence")
    @Story("Denial persists correctly in the database")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("Denial persists in REAL database")
    void denialPersistsInDatabase() throws SQLException {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{\"comment\": \"Test denial persistence\"}")
                .when()
                .post("/api/expenses/3/deny")
                .then()
                .statusCode(200);

        try (Connection conn = testDbConnection.getConnection();
             var stmt = conn.createStatement();
             var rs = stmt.executeQuery("SELECT status, comment, reviewer FROM approvals WHERE expense_id = 3")) {

            Assertions.assertTrue(rs.next(), "Denial should exist in database");
            Assertions.assertEquals("denied", rs.getString("status"));
            Assertions.assertEquals("Test denial persistence", rs.getString("comment"));
            Assertions.assertEquals(3, rs.getInt("reviewer"));
        }
    }

    @Test
    @Order(23)
    @Epic("Expense Approval")
    @Feature("Database Persistence")
    @Story("Multiple operations update status correctly")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("Multiple operations update status correctly in REAL database")
    void multipleOperations_updateStatusCorrectly() {
        given()
                .cookie("jwt", managerJwtToken)
                .contentType(ContentType.JSON)
                .body("{\"comment\": \"First approval\"}")
                .when()
                .post("/api/expenses/1/approve")
                .then()
                .statusCode(200);

        given()
                .when()
                .get("/api/expenses/pending")
                .then()
                .statusCode(200)
                .body("count", equalTo(1))
                .body("data.expense.id", not(hasItem(1)));
    }

    // ==========================================
    // Edge Cases and Validation
    // ==========================================

    @Test
    @Order(24)
    @Epic("Expense Review")
    @Feature("Validation")
    @Story("Validate correct expense descriptions")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Validates correct expense descriptions")
    void validateExpenseDescriptions() {
        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("data.expense.description", hasItems("Travel", "Office Supplies", "Meals"));
    }

    @Test
    @Order(25)
    @Epic("Expense Review")
    @Feature("Validation")
    @Story("Validate format of expense dates")
    @Severity(SeverityLevel.MINOR)
    @DisplayName("Validates expense dates format")
    void validateExpenseDatesFormat() {
        given()
                .when()
                .get("/api/expenses")
                .then()
                .statusCode(200)
                .body("data[0].expense.date", matchesPattern("\\d{4}-\\d{2}-\\d{2}"));
    }
}
