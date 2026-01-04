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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import io.qameta.allure.*;

@ExtendWith(MockitoExtension.class)
@Epic("Expense Management")
@Feature("Expense Repository - Find Expenses By Category")
public class ExpenseRepositorExpenseByCategory {

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
    @Story("Find Expenses by Category")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that expenses matching the given category are returned using LIKE query with wildcards.")
    void testFindExpensesByCategory_LikeQueryWithWildcards() throws SQLException {
        String category = "Travel";
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
                case "description": return "Travel Expense";
                case "date": return "2024-12-15";
                case "username": return "john.doe";
                case "role": return "employee";
                case "status": return "pending";
                default: return null;
            }
        });
        when(resultSet.getObject("reviewer")).thenReturn(null);

        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        assertNotNull(results);
        assertEquals(1, results.size());

        verify(preparedStatement).setString(1, "%" + category + "%");
        verify(preparedStatement).executeQuery();
    }

    @Test
    @Story("Find Expenses by Category")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that partial matches in description return all relevant expenses.")
    void testFindExpensesByCategory_PartialMatchesInDescription() throws SQLException {
        String category = "office";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        when(resultSet.next()).thenReturn(true, true, true, false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return 1;
                case "user_id": return 10;
                case "approval_id": return 101;
                default: return 0;
            }
        });

        when(resultSet.getDouble("amount")).thenReturn(50.0, 75.0, 100.0);

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description": return null;
                case "date": return "2024-12-15";
                case "username": return "user";
                case "role": return "employee";
                case "status": return "approved";
                default: return null;
            }
        });

        when(resultSet.getString("description")).thenReturn(
                "Office Supplies",
                "Home office equipment",
                "New office desk"
        );

        when(resultSet.getObject("reviewer")).thenReturn(1);

        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        assertNotNull(results);
        assertEquals(3, results.size());
        assertTrue(results.get(0).getExpense().getDescription().toLowerCase().contains(category));
        assertTrue(results.get(1).getExpense().getDescription().toLowerCase().contains(category));
        assertTrue(results.get(2).getExpense().getDescription().toLowerCase().contains(category));

        verify(preparedStatement).setString(1, "%office%");
    }

    @Test
    @Story("Find Expenses by Category")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that SQL injection attempts are safely handled using PreparedStatement parameters.")
    void testFindExpensesByCategory_SqlInjectionProtection() throws SQLException {
        String maliciousCategory = "'; DROP TABLE expenses; --";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(maliciousCategory);

        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(preparedStatement).setString(1, "%" + maliciousCategory + "%");
        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("WHERE e.description LIKE ?") &&
                        !sql.contains(maliciousCategory)
        ));
        verify(preparedStatement).executeQuery();
    }

    @Test
    @Story("Find Expenses by Category")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that a non-matching category returns an empty list.")
    void testFindExpensesByCategory_EmptyListForNoMatches() throws SQLException {
        String category = "NonExistentCategory";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).setString(1, "%" + category + "%");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}
