package expenseReview;

import com.revature.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ExpenseRepositoryFindPendingExpenseByUser
{

    @Mock
    private DatabaseConnection databaseConnection;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ExpenseRepository expenseRepository;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this); // Add this line
        expenseRepository = new ExpenseRepository(databaseConnection);
        when(databaseConnection.getConnection()).thenReturn(connection);

        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testFindPendingExpensesWithUsers_ReturnsOnlyPendingExpenses() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock two pending expenses
        when(resultSet.next()).thenReturn(true, true, false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            if ("id".equals(columnName)) return 1;
            if ("user_id".equals(columnName)) return 10;
            if ("approval_id".equals(columnName)) return 101;
            return 0;
        });

        when(resultSet.getDouble("amount")).thenReturn(100.0, 200.0);

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return "Expense";
                case "date": return "2024-12-01";
                case "username": return "user1";
                case "role": return "employee";
                case "status": return "pending";
                default: return null;
            }
        });

        when(resultSet.getObject("reviewer")).thenReturn(null);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify all expenses have pending status
        for (ExpenseWithUser expenseWithUser : results) {
            assertEquals("pending", expenseWithUser.getApproval().getStatus());
        }

        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindPendingExpensesWithUsers_JoinWithUsersTableCorrect() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        // Mock expense with user data
        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return 1;
                case "user_id": return 10;
                case "approval_id": return 101;
                default: return 0;
            }
        });

        when(resultSet.getDouble("amount")).thenReturn(150.0);
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return "Test Expense";
                case "date": return "2024-12-15";
                case "username": return "john.doe";
                case "role": return "employee";
                case "status": return "pending";
                default: return null;
            }
        });
        when(resultSet.getObject("reviewer")).thenReturn(null);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        assertEquals("john.doe", expenseWithUser.getUser().getUsername());
        assertEquals("employee", expenseWithUser.getUser().getRole());
        assertEquals(10, expenseWithUser.getUser().getId());

        // Verify JOIN columns were accessed
        verify(resultSet, atLeastOnce()).getString("username");
        verify(resultSet, atLeastOnce()).getString("role");
    }

    @Test
    void testFindPendingExpensesWithUsers_JoinWithApprovalsTableCorrect() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        Integer reviewerId = 50;

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return 1;
                case "user_id": return 10;
                case "approval_id": return 101;
                default: return 0;
            }
        });

        when(resultSet.getDouble("amount")).thenReturn(150.0);
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return "Test Expense";
                case "date": return "2024-12-15";
                case "username": return "john.doe";
                case "role": return "employee";
                case "status": return "pending";
                case "comment": return "Awaiting review";
                case "review_date": return "2024-12-16 10:00:00";
                default: return null;
            }
        });
        when(resultSet.getObject("reviewer")).thenReturn(reviewerId);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        Approval approval = expenseWithUser.getApproval();

        assertEquals(101, approval.getId());
        assertEquals("pending", approval.getStatus());
        assertEquals(reviewerId, approval.getReviewer());
        assertEquals("Awaiting review", approval.getComment());
        assertEquals("2024-12-16 10:00:00", approval.getReviewDate());

        // Verify approval columns were accessed
        verify(resultSet, atLeastOnce()).getInt("approval_id");
        verify(resultSet, atLeastOnce()).getString("status");
        verify(resultSet, atLeastOnce()).getObject("reviewer");
        verify(resultSet, atLeastOnce()).getString("comment");
        verify(resultSet, atLeastOnce()).getString("review_date");
    }

    @Test
    void testFindPendingExpensesWithUsers_OrderingByDateDesc() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock three expenses with different dates
        when(resultSet.next()).thenReturn(true, true, true, false);

        // Dates in descending order (most recent first)
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            if ("date".equals(columnName)) {
                // Return different dates on each call
                return null; // Will be overridden by specific mock below
            }
            switch (columnName) {
                case "description": return "Test";
                case "username": return "user";
                case "role": return "employee";
                case "status": return "pending";
                default: return null;
            }
        });

        // Mock dates specifically
        when(resultSet.getString("date")).thenReturn(
                "2024-12-20",  // Most recent
                "2024-12-15",  // Middle
                "2024-12-10"   // Oldest
        );

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            if ("id".equals(columnName)) return 1;
            if ("user_id".equals(columnName)) return 10;
            if ("approval_id".equals(columnName)) return 101;
            return 0;
        });

        when(resultSet.getDouble("amount")).thenReturn(100.0);
        when(resultSet.getObject("reviewer")).thenReturn(null);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify dates are in descending order (most recent first)
        assertEquals("2024-12-20", results.get(0).getExpense().getDate());
        assertEquals("2024-12-15", results.get(1).getExpense().getDate());
        assertEquals("2024-12-10", results.get(2).getExpense().getDate());

        // Verify the SQL contains ORDER BY e.date DESC
        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("ORDER BY e.date DESC")
        ));
    }

    @Test
    void testFindPendingExpensesWithUsers_ExpenseWithUserObjectFullyPopulated() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        // Complete data for all fields
        int expenseId = 1;
        int userId = 10;
        double amount = 250.50;
        String description = "Business lunch with client";
        String date = "2024-12-15";
        String username = "alice.johnson";
        String role = "manager";
        int approvalId = 101;
        String status = "pending";
        Integer reviewer = 25;
        String comment = "Please provide receipt";
        String reviewDate = "2024-12-16 14:30:00";

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return expenseId;
                case "user_id": return userId;
                case "approval_id": return approvalId;
                default: return 0;
            }
        });

        when(resultSet.getDouble("amount")).thenReturn(amount);

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return description;
                case "date": return date;
                case "username": return username;
                case "role": return role;
                case "status": return status;
                case "comment": return comment;
                case "review_date": return reviewDate;
                default: return null;
            }
        });

        when(resultSet.getObject("reviewer")).thenReturn(reviewer);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        Expense expense = expenseWithUser.getExpense();
        User user = expenseWithUser.getUser();
        Approval approval = expenseWithUser.getApproval();

        // Verify all fields from expenses table
        assertEquals(expenseId, expense.getId());
        assertEquals(userId, expense.getUserId());
        assertEquals(amount, expense.getAmount());
        assertEquals(description, expense.getDescription());
        assertEquals(date, expense.getDate());

        // Verify all fields from users table
        assertEquals(userId, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(role, user.getRole());

        // Verify all fields from approvals table
        assertEquals(approvalId, approval.getId());
        assertEquals(expenseId, approval.getExpenseId());
        assertEquals(status, approval.getStatus());
        assertEquals(reviewer, approval.getReviewer());
        assertEquals(comment, approval.getComment());
        assertEquals(reviewDate, approval.getReviewDate());
    }

    @Test
    void testFindPendingExpensesWithUsers_EmptyListWhenNoPendingExpenses() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No results

        // Act
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
