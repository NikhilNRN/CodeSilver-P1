package com.revature.repository.integration;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.ExpenseWithUser;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for ExpenseRepository
 *
 * Tests ExpenseRepository with a REAL SQLite database.
 * Uses separate test database path from production.
 */
@Epic("Manager App")
@Feature("Expense Repository Integration")
@Tag("integration")
public class ExpenseRepositoryIntegrationTest {

    private static DatabaseConnection testDbConnection;
    private static ExpenseRepository expenseRepository;

    @BeforeAll
    @Step("Set up the test database")
    static void setUpDatabase() throws SQLException, IOException {
        testDbConnection = TestDatabaseSetup.initializeTestDatabase();
        expenseRepository = new ExpenseRepository(testDbConnection);
        Allure.step("Test database initialized successfully");
    }

    @AfterAll
    @Step("Clean up the test database")
    static void tearDownDatabase() {
        TestDatabaseSetup.cleanup();
        Allure.step("Test database cleaned up");
    }

    @Test
    @Story("Find Expense")
    @Description("Find expense by ID from real database")
    @Severity(SeverityLevel.CRITICAL)
    void testFindByIdFromRealDatabase() {
        Allure.step("Fetching expense with ID 1");
        Optional<Expense> result = expenseRepository.findById(1);

        Allure.step("Verifying that expense exists");
        assertTrue(result.isPresent(), "Expense should be found");
        assertEquals(150.00, result.get().getAmount(), 0.01);
        assertEquals("Business lunch", result.get().getDescription());
        assertEquals(1, result.get().getUserId());
    }

    @Test
    @Story("Find Pending Expenses")
    @Description("Find all pending expenses with user info from real database")
    @Severity(SeverityLevel.CRITICAL)
    void testFindPendingExpensesWithUsers() {
        Allure.step("Fetching all pending expenses with user information");
        // Act
        // Assert
        Allure.step("Verifying all returned expenses have 'pending' status");
    }

    @Test
    @Story("Find Expenses by User")
    @Description("Find all expenses for a specific user from real database")
    @Severity(SeverityLevel.NORMAL)
    void testFindExpensesByUser() {
        Allure.step("Fetching all expenses for user with ID 1");
        // Act
        // Assert
        Allure.step("Verifying all expenses belong to user 1");
    }

    @Test
    @Story("Find Expenses by Date Range")
    @Description("Find expenses by date range from real database")
    @Severity(SeverityLevel.NORMAL)
    void testFindExpensesByDateRange() {
        Allure.step("Fetching expenses between Dec 1 and Dec 10");
        // Act
        // Assert
        Allure.step("Verifying all expenses are within the date range");
    }

    @Test
    @Story("Find Expenses by Category")
    @Description("Find expenses by description/category from real database")
    @Severity(SeverityLevel.NORMAL)
    void testFindExpensesByCategory() {
        Allure.step("Fetching expenses containing 'lunch' in description");
        // Act
        // Assert
        Allure.step("Verifying all expenses contain 'lunch' in description");
    }

    @Test
    @Story("Find All Expenses")
    @Description("Find all expenses with users from real database")
    @Severity(SeverityLevel.NORMAL)
    void testFindAllExpensesWithUsers() {
        Allure.step("Fetching all expenses with user information");
        // Act
        // Assert
        Allure.step("Verifying all 7 expenses from seed data have complete information");
    }

    @Test
    @Story("Find Expense")
    @Description("Find non-existent expense returns empty")
    @Severity(SeverityLevel.MINOR)
    void testFindNonExistentExpense() {
        Allure.step("Fetching expense with non-existent ID");
        // Act
        // Assert
        Allure.step("Verifying the result is empty");
    }
}
