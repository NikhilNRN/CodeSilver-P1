package expenseHistory;

import com.revature.repository.Approval;
import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ApprovalRepositoryCreateApproval
{
    @Mock
    private DatabaseConnection databaseConnection;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ApprovalRepository approvalRepository;

    @BeforeEach
    void setUp() throws SQLException {
        approvalRepository = new ApprovalRepository(databaseConnection);
        when(databaseConnection.getConnection()).thenReturn(connection);

        // Use lenient for default setup that may not be used in all tests
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testCreateApproval_CreatesApprovalWithValidExpenseIdAndStatus() throws SQLException {
        // Arrange
        int expenseId = 100;
        String status = "pending";
        int generatedId = 1;

        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getInt(1)).thenReturn(generatedId);

        // Act
        Approval result = approvalRepository.createApproval(expenseId, status);

        // Assert
        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals(expenseId, result.getExpenseId());
        assertEquals(status, result.getStatus());

        verify(preparedStatement).setInt(1, expenseId);
        verify(preparedStatement).setString(2, status);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testCreateApproval_ReturnsCreatedApprovalWithGeneratedId() throws SQLException {
        // Arrange
        int expenseId = 200;
        String status = "approved";
        int generatedId = 42;

        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getInt(1)).thenReturn(generatedId);

        // Act
        Approval result = approvalRepository.createApproval(expenseId, status);

        // Assert
        assertNotNull(result);
        assertEquals(generatedId, result.getId(), "Generated ID should be set correctly");
        assertEquals(expenseId, result.getExpenseId(), "Expense ID should match input");
        assertEquals(status, result.getStatus(), "Status should match input");

        verify(preparedStatement).getGeneratedKeys();
        verify(generatedKeys).next();
        verify(generatedKeys).getInt(1);
    }

    @Test
    void testCreateApproval_DefaultNullValuesForReviewerCommentReviewDate() throws SQLException {
        // Arrange
        int expenseId = 300;
        String status = "pending";
        int generatedId = 5;

        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(true);
        when(generatedKeys.getInt(1)).thenReturn(generatedId);

        // Act
        Approval result = approvalRepository.createApproval(expenseId, status);

        // Assert
        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals(expenseId, result.getExpenseId());
        assertEquals(status, result.getStatus());

        // Verify default null values for fields not set during creation
        assertNull(result.getReviewer(), "Reviewer should be null by default");
        assertNull(result.getComment(), "Comment should be null by default");
        assertNull(result.getReviewDate(), "Review date should be null by default");
    }

    @Test
    void testCreateApproval_ThrowsRuntimeExceptionWhenInsertFails() throws SQLException {
        // Arrange
        int expenseId = 400;
        String status = "pending";
        SQLException sqlException = new SQLException("Insert failed");

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(sqlException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });

        assertEquals("Error creating approval for expense: " + expenseId, exception.getMessage());
        assertEquals(sqlException, exception.getCause());

        verify(preparedStatement).setInt(1, expenseId);
        verify(preparedStatement).setString(2, status);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testCreateApproval_ThrowsRuntimeExceptionWhenNoRowsAffected() throws SQLException {
        // Arrange
        int expenseId = 500;
        String status = "pending";

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0); // No rows affected

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });

        assertEquals("Creating approval failed, no rows affected.", exception.getMessage());

        verify(preparedStatement).executeUpdate();
    }

    @Test
    void testCreateApproval_ThrowsRuntimeExceptionWhenNoIdObtained() throws SQLException {
        // Arrange
        int expenseId = 600;
        String status = "pending";

        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false); // No generated key available

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });

        assertEquals("Creating approval failed, no ID obtained.", exception.getMessage());

        verify(preparedStatement).getGeneratedKeys();
        verify(generatedKeys).next();
    }
}