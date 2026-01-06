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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import io.qameta.allure.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management")
@Feature("Expense Repository - Find Pending Expenses By User")
public class ExpenseRepositoryFindPendingExpenseByUser {

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
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that only pending expenses are returned for users.")
    void testFindPendingExpensesWithUsers_ReturnsOnlyPendingExpenses() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
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

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        assertEquals(2, results.size());

        for (ExpenseWithUser expenseWithUser : results) {
            assertEquals("pending", expenseWithUser.getApproval().getStatus());
        }

        verify(preparedStatement).executeQuery();
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that the JOIN with the Users table populates user details correctly.")
    void testFindPendingExpensesWithUsers_JoinWithUsersTableCorrect() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

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

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser expenseWithUser = results.get(0);
        assertEquals("john.doe", expenseWithUser.getUser().getUsername());
        assertEquals("employee", expenseWithUser.getUser().getRole());
        assertEquals(10, expenseWithUser.getUser().getId());

        verify(resultSet, atLeastOnce()).getString("username");
        verify(resultSet, atLeastOnce()).getString("role");
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that the JOIN with the Approvals table populates approval details correctly.")
    void testFindPendingExpensesWithUsers_JoinWithApprovalsTableCorrect() throws SQLException {
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

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        assertEquals(1, results.size());

        Approval approval = results.get(0).getApproval();
        assertEquals(101, approval.getId());
        assertEquals("pending", approval.getStatus());
        assertEquals(reviewerId, approval.getReviewer());
        assertEquals("Awaiting review", approval.getComment());
        assertEquals("2024-12-16 10:00:00", approval.getReviewDate());

        verify(resultSet, atLeastOnce()).getInt("approval_id");
        verify(resultSet, atLeastOnce()).getString("status");
        verify(resultSet, atLeastOnce()).getObject("reviewer");
        verify(resultSet, atLeastOnce()).getString("comment");
        verify(resultSet, atLeastOnce()).getString("review_date");
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that pending expenses are returned in descending order by date.")
    void testFindPendingExpensesWithUsers_OrderingByDateDesc() throws SQLException {
        // Stub the prepared statement and result set
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Simulate 3 rows in the ResultSet
        when(resultSet.next()).thenReturn(true, true, true, false);

        // Stub all ResultSet getters used by mapRowToExpenseWithUser
        when(resultSet.getString("date")).thenReturn(
                "2024-12-20",
                "2024-12-15",
                "2024-12-10"
        );
        when(resultSet.getString("description")).thenReturn(
                "Lunch",
                "Taxi",
                "Hotel"
        );
        when(resultSet.getInt("id")).thenReturn(1, 2, 3);
        when(resultSet.getDouble("amount")).thenReturn(100.0, 50.0, 200.0);
        when(resultSet.getObject("reviewer")).thenReturn(null, null, null);

        // Call the method under test
        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        // Assertions
        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("2024-12-20", results.get(0).getExpense().getDate());
        assertEquals("2024-12-15", results.get(1).getExpense().getDate());
        assertEquals("2024-12-10", results.get(2).getExpense().getDate());

        // Verify SQL ordering
        verify(connection).prepareStatement(argThat(sql -> sql.contains("ORDER BY e.date DESC")));
    }


    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that the ExpenseWithUser object is fully populated from expenses, users, and approvals tables.")
    void testFindPendingExpensesWithUsers_ExpenseWithUserObjectFullyPopulated() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);

        int expenseId = 1, userId = 10, approvalId = 101;
        double amount = 250.50;
        String description = "Business lunch with client";
        String date = "2024-12-15";
        String username = "alice.johnson";
        String role = "manager";
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

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        assertEquals(1, results.size());

        ExpenseWithUser ewu = results.get(0);
        Expense expense = ewu.getExpense();
        User user = ewu.getUser();
        Approval approval = ewu.getApproval();

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
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that an empty list is returned when there are no pending expenses.")
    void testFindPendingExpensesWithUsers_EmptyListWhenNoPendingExpenses() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ExpenseWithUser> results = expenseRepository.findPendingExpensesWithUsers();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
