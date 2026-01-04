package expenseHistory;

import com.revature.repository.Approval;
import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import io.qameta.allure.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Approval Management")
@Feature("Approval Repository - Create Approval")
public class ApprovalRepositoryCreateApproval {

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
        lenient().when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    @Story("Create Approval with valid expense ID and status")
    @Description("Tests that a new Approval object is created successfully with a valid expense ID and status")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateApproval_CreatesApprovalWithValidExpenseIdAndStatus() throws SQLException {
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

        Approval result = approvalRepository.createApproval(expenseId, status);

        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals(expenseId, result.getExpenseId());
        assertEquals(status, result.getStatus());

        verify(preparedStatement).setInt(1, expenseId);
        verify(preparedStatement).setString(2, status);
        verify(preparedStatement).executeUpdate();
    }

    @Test
    @Story("Return created approval with generated ID")
    @Description("Verifies that the created Approval object returns the correct generated ID")
    @Severity(SeverityLevel.NORMAL)
    void testCreateApproval_ReturnsCreatedApprovalWithGeneratedId() throws SQLException {
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

        Approval result = approvalRepository.createApproval(expenseId, status);

        assertNotNull(result);
        assertEquals(generatedId, result.getId(), "Generated ID should be set correctly");
        assertEquals(expenseId, result.getExpenseId(), "Expense ID should match input");
        assertEquals(status, result.getStatus(), "Status should match input");

        verify(preparedStatement).getGeneratedKeys();
        verify(generatedKeys).next();
        verify(generatedKeys).getInt(1);
    }

    @Test
    @Story("Default null values for reviewer, comment, and review date")
    @Description("Ensures that fields not set during approval creation default to null")
    @Severity(SeverityLevel.MINOR)
    void testCreateApproval_DefaultNullValuesForReviewerCommentReviewDate() throws SQLException {
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

        Approval result = approvalRepository.createApproval(expenseId, status);

        assertNotNull(result);
        assertEquals(generatedId, result.getId());
        assertEquals(expenseId, result.getExpenseId());
        assertEquals(status, result.getStatus());

        assertNull(result.getReviewer(), "Reviewer should be null by default");
        assertNull(result.getComment(), "Comment should be null by default");
        assertNull(result.getReviewDate(), "Review date should be null by default");
    }

    @Test
    @Story("Throws RuntimeException when insert fails")
    @Description("Verifies that a RuntimeException is thrown when database insert fails")
    @Severity(SeverityLevel.BLOCKER)
    void testCreateApproval_ThrowsRuntimeExceptionWhenInsertFails() throws SQLException {
        int expenseId = 400;
        String status = "pending";
        SQLException sqlException = new SQLException("Insert failed");

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(sqlException);

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
    @Story("Throws RuntimeException when no rows affected")
    @Description("Verifies that a RuntimeException is thrown if the insert does not affect any rows")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateApproval_ThrowsRuntimeExceptionWhenNoRowsAffected() throws SQLException {
        int expenseId = 500;
        String status = "pending";

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });

        assertEquals("Creating approval failed, no rows affected.", exception.getMessage());

        verify(preparedStatement).executeUpdate();
    }

    @Test
    @Story("Throws RuntimeException when no ID obtained")
    @Description("Verifies that a RuntimeException is thrown if no ID is returned after insert")
    @Severity(SeverityLevel.CRITICAL)
    void testCreateApproval_ThrowsRuntimeExceptionWhenNoIdObtained() throws SQLException {
        int expenseId = 600;
        String status = "pending";

        ResultSet generatedKeys = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), eq(PreparedStatement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeys);
        when(generatedKeys.next()).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.createApproval(expenseId, status);
        });

        assertEquals("Creating approval failed, no ID obtained.", exception.getMessage());

        verify(preparedStatement).getGeneratedKeys();
        verify(generatedKeys).next();
    }
}
