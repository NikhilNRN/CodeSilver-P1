package com.revature.repository.integration;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.User;
import com.revature.repository.UserRepository;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration Tests for UserRepository
 *
 * Tests UserRepository with a REAL SQLite database.
 * Uses separate test database path from production.
 */
@Epic("Manager App")
@Feature("User Repository Integration")
@Tag("integration")
public class UserRepositoryIntegrationTest {

    private static DatabaseConnection testDbConnection;
    private static UserRepository userRepository;

    @BeforeAll
    @Step("Set up the test database")
    static void setUpDatabase() throws SQLException, IOException {
        testDbConnection = TestDatabaseSetup.initializeTestDatabase();
        userRepository = new UserRepository(testDbConnection);
        Allure.step("Test database initialized successfully");
    }

    @AfterAll
    @Step("Clean up the test database")
    static void tearDownDatabase() {
        TestDatabaseSetup.cleanup();
        Allure.step("Test database cleaned up");
    }

    @Test
    @Story("Find User")
    @Description("Find user by ID from real database")
    @Severity(SeverityLevel.CRITICAL)
    void testFindByIdFromRealDatabase() {
        Allure.step("Fetching user with ID 1");
        Optional<User> result = userRepository.findById(1);

        Allure.step("Verifying that the user exists");
        assertTrue(result.isPresent(), "User should be found");
        assertEquals("employee1", result.get().getUsername());
        assertEquals("Employee", result.get().getRole());
    }

    @Test
    @Story("Find User")
    @Description("Find user by username from real database")
    @Severity(SeverityLevel.CRITICAL)
    void testFindByUsernameFromRealDatabase() {
        Allure.step("Fetching user with username 'employee1'");
        // Act
        // Assert
        Allure.step("Verifying that the user exists");
    }

    @Test
    @Story("Find User")
    @Description("Find non-existent user returns empty")
    @Severity(SeverityLevel.NORMAL)
    void testFindNonExistentUser() {
        Allure.step("Fetching user with non-existent ID or username");
        // Act
        // Assert
        Allure.step("Verifying the result is empty");
    }

    @Test
    @Story("Find User")
    @Description("Find manager user from real database")
    @Severity(SeverityLevel.NORMAL)
    void testFindManagerUser() {
        Allure.step("Fetching manager user with ID 3");
        // Act
        // Assert
        Allure.step("Verifying the manager user exists and has correct role");
    }

    @Test
    @Story("Find User")
    @Description("Verify all seeded users exist")
    @Severity(SeverityLevel.MINOR)
    void testAllSeededUsersExist() {
        Allure.step("Verifying all 5 seeded users exist");
        // Act
        // Assert
        Allure.step("Verifying user with ID 6 does not exist");
    }
}
