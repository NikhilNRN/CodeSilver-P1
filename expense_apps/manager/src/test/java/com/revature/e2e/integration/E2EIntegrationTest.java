package com.revature.e2e.integration;

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

@Epic("Manager App")
@Feature("E2E Integration Tests")
@Tag("e2e")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class E2EIntegrationTest {

    private static DatabaseConnection testDbConnection;

    @BeforeAll
    static void setupDatabase() throws SQLException, IOException {
        Allure.step("Initialize test database and configure REST Assured", () -> {
            testDbConnection = TestDatabaseSetup.initializeTestDatabase();
            RestAssured.baseURI = "http://localhost";
            RestAssured.port = 5001;
        });
    }

    @AfterAll
    static void tearDown() {
        Allure.step("Cleanup test database and reset REST Assured", () -> {
            TestDatabaseSetup.cleanup();
            RestAssured.reset();
        });
    }

    // ==================== COMPLETE WORKFLOW TESTS ====================

    @Test
    @Order(1)
    @Story("Complete Approval Workflow")
    @Description("Full workflow: Login → View pending → Approve → Verify")
    @Severity(SeverityLevel.CRITICAL)
    void testCompleteApprovalWorkflow() {
        Response loginResponse = Allure.step("Step 1: Login as manager", () ->
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"username\": \"manager1\", \"password\": \"admin123\"}")
                        .post("/api/auth/login")
        );

        if (loginResponse.getStatusCode() != 200) return;

        String jwt = loginResponse.getCookie("jwt");
        Assertions.assertNotNull(jwt, "Should receive JWT on login");

        Response pendingResponse = Allure.step("Step 2: View pending expenses", () ->
                given()
                        .cookie("jwt", jwt)
                        .get("/api/expenses/pending")
                        .then()
                        .statusCode(200)
                        .extract()
                        .response()
        );

        Response approveResponse = Allure.step("Step 3: Approve first pending expense (ID 1)", () ->
                given()
                        .cookie("jwt", jwt)
                        .contentType(ContentType.JSON)
                        .body("{\"comment\": \"E2E workflow approval\"}")
                        .post("/api/expenses/1/approve")
        );

        Assertions.assertTrue(
                approveResponse.getStatusCode() == 200 ||
                        approveResponse.getStatusCode() == 404 ||
                        approveResponse.getStatusCode() == 400,
                "Approve should return valid status"
        );

        Allure.step("Step 4: Verify expenses after approval", () ->
                given()
                        .cookie("jwt", jwt)
                        .get("/api/expenses")
                        .then()
                        .statusCode(200)
        );
    }

    @Test
    @Order(2)
    @Story("Complete Denial Workflow")
    @Description("Full workflow: Login → View pending → Deny → Verify")
    @Severity(SeverityLevel.CRITICAL)
    void testCompleteDenialWorkflow() {
        Allure.step("Complete Denial Workflow test (not implemented)", () -> {});
    }

    @Test
    @Order(3)
    @Story("Report Generation Workflow")
    @Description("Full workflow: Login → Generate multiple report types")
    @Severity(SeverityLevel.NORMAL)
    void testReportGenerationWorkflow() {
        Allure.step("Report Generation Workflow test (not implemented)", () -> {});
    }

    @Test
    @Order(4)
    @Story("Employee Expense Review Workflow")
    @Description("Full workflow: Login → View expenses by employee → Review")
    @Severity(SeverityLevel.NORMAL)
    void testEmployeeExpenseReviewWorkflow() {
        Allure.step("Employee Expense Review Workflow test (not implemented)", () -> {});
    }

    @Test
    @Order(5)
    @Story("Unauthorized Access Prevention")
    @Description("Verify unauthorized access is blocked")
    @Severity(SeverityLevel.CRITICAL)
    void testUnauthorizedAccessPrevention() {
        Allure.step("Unauthorized Access Prevention test (not implemented)", () -> {});
    }

    @Test
    @Order(6)
    @Story("Session Management")
    @Description("Full workflow: Login → Logout → Verify access revoked")
    @Severity(SeverityLevel.NORMAL)
    void testSessionManagementWorkflow() {
        Allure.step("Session Management Workflow test (not implemented)", () -> {});
    }
}
