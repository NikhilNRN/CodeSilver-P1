package expenseHistory;


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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseHistoryFindAllExpensesByUser
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
        MockitoAnnotations.openMocks(this);
        expenseRepository = new ExpenseRepository(databaseConnection);
        when(databaseConnection.getConnection()).thenReturn(connection);

        // Use lenient for default setup that may not be used in all tests
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testFindAllExpensesWithUsers_ReturnsAllExpensesRegardlessOfStatus() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock expenses with different statuses
        when(resultSet.next()).thenReturn(true, true, true, false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            if ("id".equals(columnName)) return 1;
            if ("user_id".equals(columnName)) return 10;
            if ("approval_id".equals(columnName)) return 101;
            return 0;
        });

        when(resultSet.getDouble("amount")).thenReturn(100.0, 200.0, 300.0);

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return "Expense";
                case "date": return "2024-12-01";
                case "username": return "user1";
                case "role": return "employee";
                default: return null;
            }
        });

        // Different statuses for each expense
        when(resultSet.getString("status")).thenReturn(
                "pending",
                "approved",
                "rejected"
        );

        when(resultSet.getObject("reviewer")).thenReturn(1);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify all different statuses are included
        assertEquals("pending", results.get(0).getApproval().getStatus());
        assertEquals("approved", results.get(1).getApproval().getStatus());
        assertEquals("rejected", results.get(2).getApproval().getStatus());

        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindAllExpensesWithUsers_CompleteJoinsWithUsersAndApprovals() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        // Complete data from all three tables
        int expenseId = 1;
        int userId = 10;
        double amount = 250.50;
        String description = "Business lunch";
        String date = "2024-12-15";
        String username = "alice.johnson";
        String role = "manager";
        int approvalId = 101;
        String status = "approved";
        Integer reviewer = 25;
        String comment = "Approved with receipt";
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
        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        Expense expense = expenseWithUser.getExpense();
        User user = expenseWithUser.getUser();
        Approval approval = expenseWithUser.getApproval();

        // Verify expense data (from expenses table)
        assertEquals(expenseId, expense.getId());
        assertEquals(userId, expense.getUserId());
        assertEquals(amount, expense.getAmount());
        assertEquals(description, expense.getDescription());
        assertEquals(date, expense.getDate());

        // Verify user data (from users table JOIN)
        assertEquals(userId, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(role, user.getRole());

        // Verify approval data (from approvals table JOIN)
        assertEquals(approvalId, approval.getId());
        assertEquals(expenseId, approval.getExpenseId());
        assertEquals(status, approval.getStatus());
        assertEquals(reviewer, approval.getReviewer());
        assertEquals(comment, approval.getComment());
        assertEquals(reviewDate, approval.getReviewDate());

        // Verify all JOIN columns were accessed
        verify(resultSet, atLeastOnce()).getString("username");
        verify(resultSet, atLeastOnce()).getString("role");
        verify(resultSet, atLeastOnce()).getInt("approval_id");
        verify(resultSet, atLeastOnce()).getString("status");
    }

    @Test
    void testFindAllExpensesWithUsers_OrderingByDateDesc() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock three expenses with different dates
        when(resultSet.next()).thenReturn(true, true, true, false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            if ("id".equals(columnName)) return 1;
            if ("user_id".equals(columnName)) return 10;
            if ("approval_id".equals(columnName)) return 101;
            return 0;
        });

        when(resultSet.getDouble("amount")).thenReturn(100.0);

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return "Test";
                case "username": return "user";
                case "role": return "employee";
                case "status": return "approved";
                default: return null;
            }
        });

        // Dates in descending order (most recent first)
        when(resultSet.getString("date")).thenReturn(
                "2024-12-20",  // Most recent
                "2024-12-15",  // Middle
                "2024-12-10"   // Oldest
        );

        when(resultSet.getObject("reviewer")).thenReturn(1);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

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
    void testFindAllExpensesWithUsers_EmptyListWhenNoExpensesExist() throws SQLException {
        // Arrange
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No results

        // Act
        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
