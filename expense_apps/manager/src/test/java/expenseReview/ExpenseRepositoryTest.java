package expenseReview;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.revature.repository.Expense;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.DatabaseConnection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.Optional;

import io.qameta.allure.*;

@Epic("Expense Management")
@Feature("Expense Repository - Find Expense By ID")
public class ExpenseRepositoryTest {

    private DatabaseConnection mockDbConnection;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    private ExpenseRepository expenseRepository;

    @BeforeEach
    void setUp() throws SQLException {
        mockDbConnection = mock(DatabaseConnection.class);
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(mockDbConnection.getConnection()).thenReturn(mockConnection);
        expenseRepository = new ExpenseRepository(mockDbConnection);
    }

    @Test
    @Story("Find Expense By ID")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that findById returns an Expense object when the ID exists in the database.")
    void findById_returnsExpenseWhenFound() throws SQLException {
        int testExpenseId = 42;
        String expectedDescription = "Test description";

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("id")).thenReturn(testExpenseId);
        when(mockResultSet.getString("description")).thenReturn(expectedDescription);

        Optional<Expense> result = expenseRepository.findById(testExpenseId);

        assertTrue(result.isPresent());
        Expense expense = result.get();
        assertEquals(testExpenseId, expense.getId());
        assertEquals(expectedDescription, expense.getDescription());

        verify(mockPreparedStatement).setInt(1, testExpenseId);
        verify(mockPreparedStatement).executeQuery();
    }

    @Test
    @Story("Find Expense By ID")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that findById returns an empty Optional when the ID does not exist in the database.")
    void findById_returnsEmptyWhenNotFound() throws SQLException {
        int testExpenseId = 99;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(false);

        Optional<Expense> result = expenseRepository.findById(testExpenseId);

        assertTrue(result.isEmpty());

        verify(mockPreparedStatement).setInt(1, testExpenseId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet).next();
    }

    @Test
    @Story("Find Expense By ID")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that findById throws a RuntimeException wrapping SQLException if the database query fails.")
    void findById_throwsRuntimeExceptionOnSQLException() throws SQLException {
        int testExpenseId = 13;

        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            expenseRepository.findById(testExpenseId);
        });

        assertTrue(ex.getMessage().contains("Error finding expense by ID"));
        assertTrue(ex.getCause() instanceof SQLException);
    }
}
