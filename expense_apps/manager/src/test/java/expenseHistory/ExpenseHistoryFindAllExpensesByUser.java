package expenseHistory;

import com.revature.repository.*;
import io.qameta.allure.*;
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
@Epic("Expense Management")
@Feature("Expense Repository - Find All Expenses With Users")
public class ExpenseHistoryFindAllExpensesByUser {

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
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    @Story("Return all expenses with users regardless of status")
    @Description("Verifies that findAllExpensesWithUsers returns all expenses including pending, approved, and rejected statuses")
    @Severity(SeverityLevel.CRITICAL)
    void testFindAllExpensesWithUsers_ReturnsAllExpensesRegardlessOfStatus() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

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

        when(resultSet.getString("status")).thenReturn("pending", "approved", "rejected");
        when(resultSet.getObject("reviewer")).thenReturn(1);

        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("pending", results.get(0).getApproval().getStatus());
        assertEquals("approved", results.get(1).getApproval().getStatus());
        assertEquals("rejected", results.get(2).getApproval().getStatus());

        verify(preparedStatement).executeQuery();
    }

    @Test
    @Story("Complete JOIN between expenses, users, and approvals")
    @Description("Ensures that findAllExpensesWithUsers correctly maps expense, user, and approval fields for joined tables")
    @Severity(SeverityLevel.CRITICAL)
    void testFindAllExpensesWithUsers_CompleteJoinsWithUsersAndApprovals() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        int expenseId = 1, userId = 10, approvalId = 101;
        double amount = 250.50;
        String description = "Business lunch", date = "2024-12-15", username = "alice.johnson", role = "manager";
        String status = "approved", comment = "Approved with receipt", reviewDate = "2024-12-16 14:30:00";
        Integer reviewer = 25;

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

        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        Expense expense = expenseWithUser.getExpense();
        User user = expenseWithUser.getUser();
        Approval approval = expenseWithUser.getApproval();

        assertEquals(expenseId, expense.getId());
        assertEquals(userId, expense.getUserId());
        assertEquals(amount, expense.getAmount());
        assertEquals(description, expense.getDescription());
        assertEquals(date, expense.getDate());

        assertEquals(userId, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(role, user.getRole());

        assertEquals(approvalId, approval.getId());
        assertEquals(expenseId, approval.getExpenseId());
        assertEquals(status, approval.getStatus());
        assertEquals(reviewer, approval.getReviewer());
        assertEquals(comment, approval.getComment());
        assertEquals(reviewDate, approval.getReviewDate());

        verify(resultSet, atLeastOnce()).getString("username");
        verify(resultSet, atLeastOnce()).getString("role");
        verify(resultSet, atLeastOnce()).getInt("approval_id");
        verify(resultSet, atLeastOnce()).getString("status");
    }

    @Test
    @Story("Verify ordering of expenses by date descending")
    @Description("Ensures that findAllExpensesWithUsers returns expenses ordered by date in descending order")
    @Severity(SeverityLevel.NORMAL)
    void testFindAllExpensesWithUsers_OrderingByDateDesc() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, true, false);

        when(resultSet.getInt(anyString())).thenReturn(1, 10, 101);
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

        when(resultSet.getString("date")).thenReturn("2024-12-20", "2024-12-15", "2024-12-10");
        when(resultSet.getObject("reviewer")).thenReturn(1);

        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        assertNotNull(results);
        assertEquals(3, results.size());

        assertEquals("2024-12-20", results.get(0).getExpense().getDate());
        assertEquals("2024-12-15", results.get(1).getExpense().getDate());
        assertEquals("2024-12-10", results.get(2).getExpense().getDate());

        verify(connection).prepareStatement(argThat(sql -> sql.contains("ORDER BY e.date DESC")));
    }

    @Test
    @Story("Return empty list when no expenses exist")
    @Description("Ensures that findAllExpensesWithUsers returns an empty list if the database contains no expenses")
    @Severity(SeverityLevel.MINOR)
    void testFindAllExpensesWithUsers_EmptyListWhenNoExpensesExist() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ExpenseWithUser> results = expenseRepository.findAllExpensesWithUsers();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
