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

@ExtendWith(MockitoExtension.class)

public class ExpenseRepositorExpenseByCategory
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
    void setUp() throws SQLException
    {
        MockitoAnnotations.openMocks(this);
        expenseRepository = new ExpenseRepository(databaseConnection);
        when(databaseConnection.getConnection()).thenReturn(connection);

        // Use lenient for default setup that may not be used in all tests
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testFindExpensesByCategory_LikeQueryWithWildcards() throws SQLException {
        // Arrange
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

        // Act
        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        // Assert
        assertNotNull(results);
        assertEquals(1, results.size());

        // Verify the parameter was set with wildcards
        verify(preparedStatement).setString(1, "%" + category + "%");
        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindExpensesByCategory_PartialMatchesInDescription() throws SQLException {
        // Arrange
        String category = "office";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        // Mock multiple partial matches
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

        // Different descriptions that all contain "office"
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "description":
                    // Simulate different partial matches
                    return null; // Will be overridden below
                case "date": return "2024-12-15";
                case "username": return "user";
                case "role": return "employee";
                case "status": return "approved";
                default: return null;
            }
        });

        when(resultSet.getString("description")).thenReturn(
                "Office Supplies",      // Contains "office" at start
                "Home office equipment", // Contains "office" in middle
                "New office desk"        // Contains "office" in middle
        );

        when(resultSet.getObject("reviewer")).thenReturn(1);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        // Assert
        assertNotNull(results);
        assertEquals(3, results.size());

        // Verify all results contain the category substring
        assertTrue(results.get(0).getExpense().getDescription().toLowerCase().contains(category));
        assertTrue(results.get(1).getExpense().getDescription().toLowerCase().contains(category));
        assertTrue(results.get(2).getExpense().getDescription().toLowerCase().contains(category));

        verify(preparedStatement).setString(1, "%office%");
    }

    @Test
    void testFindExpensesByCategory_SqlInjectionProtection() throws SQLException {
        // Arrange
        String maliciousCategory = "'; DROP TABLE expenses; --";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(maliciousCategory);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());

        // Verify that the malicious string was passed as a parameter (safe)
        // and NOT concatenated into the SQL (which would be unsafe)
        verify(preparedStatement).setString(1, "%" + maliciousCategory + "%");

        // Verify PreparedStatement was used (which protects against SQL injection)
        verify(connection).prepareStatement(argThat(sql ->
                sql.contains("WHERE e.description LIKE ?") &&
                        !sql.contains(maliciousCategory)
        ));

        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindExpensesByCategory_EmptyListForNoMatches() throws SQLException {
        // Arrange
        String category = "NonExistentCategory";
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false); // No results

        // Act
        List<ExpenseWithUser> results = expenseRepository.findExpensesByCategory(category);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
        assertEquals(0, results.size());

        verify(preparedStatement).setString(1, "%" + category + "%");
        verify(preparedStatement).executeQuery();
        verify(resultSet).next();
    }
}