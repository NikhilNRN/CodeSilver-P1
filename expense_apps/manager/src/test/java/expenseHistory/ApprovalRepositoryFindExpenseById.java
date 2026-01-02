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
class ApprovalRepositoryFindExpenseById
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
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @Test
    void testFindByExpenseId_ReturnsApprovalForValidExpenseId() throws SQLException {
        // Arrange
        int expenseId = 100;
        int approvalId = 1;
        String status = "APPROVED";
        Integer reviewer = 42; // Changed to Integer
        String comment = "Looks good";
        String reviewDate = "2024-12-15 10:30:00"; // Changed to String

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);

        // Mock getInt for id and expense_id
        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id":
                    return approvalId;
                case "expense_id":
                    return expenseId;
                default:
                    return 0;
            }
        });

        // Mock getString for status, comment, and review_date
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "status":
                    return status;
                case "comment":
                    return comment;
                case "review_date":
                    return reviewDate;
                default:
                    return null;
            }
        });

        // Mock getObject for reviewer (which returns Integer)
        when(resultSet.getObject("reviewer")).thenReturn(reviewer);

        // Act
        Optional<Approval> result = approvalRepository.findByExpenseId(expenseId);

        // Assert
        assertTrue(result.isPresent());
        Approval approval = result.get();
        assertEquals(approvalId, approval.getId());
        assertEquals(expenseId, approval.getExpenseId());
        assertEquals(status, approval.getStatus());
        assertEquals(reviewer, approval.getReviewer());
        assertEquals(comment, approval.getComment());
        assertEquals(reviewDate, approval.getReviewDate());

        verify(preparedStatement).setInt(1, expenseId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindByExpenseId_ReturnsEmptyOptionalForNonExistentExpenseId() throws SQLException {
        // Arrange
        int nonExistentExpenseId = 999;

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // Act
        Optional<Approval> result = approvalRepository.findByExpenseId(nonExistentExpenseId);

        // Assert
        assertFalse(result.isPresent());
        assertEquals(Optional.empty(), result);

        verify(preparedStatement).setInt(1, nonExistentExpenseId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    void testFindByExpenseId_AllApprovalFieldsMappedCorrectly() throws SQLException {
        // Arrange
        int expenseId = 200;
        int approvalId = 42;
        String status = "PENDING";
        Integer reviewer = 123; // Changed to Integer
        String comment = "Needs clarification on item 3";
        String reviewDate = "2024-11-20 14:45:30"; // Changed to String

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);

        // Mock getInt for id and expense_id
        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id":
                    return approvalId;
                case "expense_id":
                    return expenseId;
                default:
                    return 0;
            }
        });

        // Mock getString for status, comment, and review_date
        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "status":
                    return status;
                case "comment":
                    return comment;
                case "review_date":
                    return reviewDate;
                default:
                    return null;
            }
        });

        // Mock getObject for reviewer
        when(resultSet.getObject("reviewer")).thenReturn(reviewer);

        // Act
        Optional<Approval> result = approvalRepository.findByExpenseId(expenseId);

        // Assert
        assertTrue(result.isPresent());
        Approval approval = result.get();

        // Verify each field is mapped correctly
        assertEquals(approvalId, approval.getId(), "ID should be mapped correctly");
        assertEquals(expenseId, approval.getExpenseId(), "Expense ID should be mapped correctly");
        assertEquals(status, approval.getStatus(), "Status should be mapped correctly");
        assertEquals(reviewer, approval.getReviewer(), "Reviewer should be mapped correctly");
        assertEquals(comment, approval.getComment(), "Comment should be mapped correctly");
        assertEquals(reviewDate, approval.getReviewDate(), "Review date should be mapped correctly");

        // Verify correct columns were accessed
        verify(resultSet).getInt("id");
        verify(resultSet).getInt("expense_id");
        verify(resultSet).getString("status");
        verify(resultSet).getObject("reviewer"); // Changed from getString
        verify(resultSet).getString("comment");
        verify(resultSet).getString("review_date"); // Changed from getTimestamp
    }

    @Test
    void testFindByExpenseId_ThrowsRuntimeExceptionOnSQLException() throws SQLException {
        // Arrange
        int expenseId = 300;
        SQLException sqlException = new SQLException("Database connection failed");

        when(preparedStatement.executeQuery()).thenThrow(sqlException);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.findByExpenseId(expenseId);
        });

        assertEquals("Error finding approval for expense: " + expenseId, exception.getMessage());
        assertEquals(sqlException, exception.getCause());

        verify(preparedStatement).setInt(1, expenseId);
    }
}