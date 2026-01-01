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

        // Setup chain: databaseConnection.getConnection() returns mockConnection
        when(mockDbConnection.getConnection()).thenReturn(mockConnection);

        // expenseRepository uses the mock DatabaseConnection
        expenseRepository = new ExpenseRepository(mockDbConnection);
    }

    @Test
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
    void findById_returnsEmptyWhenNotFound() throws SQLException {
        int testExpenseId = 99;

        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // simulate empty result set
        when(mockResultSet.next()).thenReturn(false);

        Optional<Expense> result = expenseRepository.findById(testExpenseId);

        assertTrue(result.isEmpty());

        verify(mockPreparedStatement).setInt(1, testExpenseId);
        verify(mockPreparedStatement).executeQuery();
        verify(mockResultSet).next();
    }

    @Test
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
