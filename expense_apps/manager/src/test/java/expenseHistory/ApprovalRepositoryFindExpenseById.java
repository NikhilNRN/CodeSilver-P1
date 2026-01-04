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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Epic("Approval Management")
@Feature("Approval Repository - Find Approval by Expense ID")
class ApprovalRepositoryFindExpenseById {

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
    @Story("Find approval by valid expense ID")
    @Description("Verifies that an approval is returned for a valid expense ID with all fields mapped correctly")
    @Severity(SeverityLevel.CRITICAL)
    void testFindByExpenseId_ReturnsApprovalForValidExpenseId() throws SQLException {
        int expenseId = 100;
        int approvalId = 1;
        String status = "APPROVED";
        Integer reviewer = 42;
        String comment = "Looks good";
        String reviewDate = "2024-12-15 10:30:00";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return approvalId;
                case "expense_id": return expenseId;
                default: return 0;
            }
        });

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "status": return status;
                case "comment": return comment;
                case "review_date": return reviewDate;
                default: return null;
            }
        });

        when(resultSet.getObject("reviewer")).thenReturn(reviewer);

        Optional<Approval> result = approvalRepository.findByExpenseId(expenseId);

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
    @Story("Find approval by non-existent expense ID")
    @Description("Verifies that an empty Optional is returned for an expense ID that does not exist")
    @Severity(SeverityLevel.NORMAL)
    void testFindByExpenseId_ReturnsEmptyOptionalForNonExistentExpenseId() throws SQLException {
        int nonExistentExpenseId = 999;

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Optional<Approval> result = approvalRepository.findByExpenseId(nonExistentExpenseId);

        assertFalse(result.isPresent());
        assertEquals(Optional.empty(), result);

        verify(preparedStatement).setInt(1, nonExistentExpenseId);
        verify(preparedStatement).executeQuery();
    }

    @Test
    @Story("All approval fields are correctly mapped")
    @Description("Ensures that all columns from the database result set are correctly mapped to Approval fields")
    @Severity(SeverityLevel.CRITICAL)
    void testFindByExpenseId_AllApprovalFieldsMappedCorrectly() throws SQLException {
        int expenseId = 200;
        int approvalId = 42;
        String status = "PENDING";
        Integer reviewer = 123;
        String comment = "Needs clarification on item 3";
        String reviewDate = "2024-11-20 14:45:30";

        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(false);

        when(resultSet.getInt(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "id": return approvalId;
                case "expense_id": return expenseId;
                default: return 0;
            }
        });

        when(resultSet.getString(anyString())).thenAnswer(invocation -> {
            String columnName = invocation.getArgument(0);
            switch (columnName) {
                case "status": return status;
                case "comment": return comment;
                case "review_date": return reviewDate;
                default: return null;
            }
        });

        when(resultSet.getObject("reviewer")).thenReturn(reviewer);

        Optional<Approval> result = approvalRepository.findByExpenseId(expenseId);

        assertTrue(result.isPresent());
        Approval approval = result.get();

        assertEquals(approvalId, approval.getId(), "ID should be mapped correctly");
        assertEquals(expenseId, approval.getExpenseId(), "Expense ID should be mapped correctly");
        assertEquals(status, approval.getStatus(), "Status should be mapped correctly");
        assertEquals(reviewer, approval.getReviewer(), "Reviewer should be mapped correctly");
        assertEquals(comment, approval.getComment(), "Comment should be mapped correctly");
        assertEquals(reviewDate, approval.getReviewDate(), "Review date should be mapped correctly");

        verify(resultSet).getInt("id");
        verify(resultSet).getInt("expense_id");
        verify(resultSet).getString("status");
        verify(resultSet).getObject("reviewer");
        verify(resultSet).getString("comment");
        verify(resultSet).getString("review_date");
    }

    @Test
    @Story("Throws RuntimeException on SQLException")
    @Description("Ensures that a RuntimeException is thrown if a SQLException occurs while querying the database")
    @Severity(SeverityLevel.BLOCKER)
    void testFindByExpenseId_ThrowsRuntimeExceptionOnSQLException() throws SQLException {
        int expenseId = 300;
        SQLException sqlException = new SQLException("Database connection failed");

        when(preparedStatement.executeQuery()).thenThrow(sqlException);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            approvalRepository.findByExpenseId(expenseId);
        });

        assertEquals("Error finding approval for expense: " + expenseId, exception.getMessage());
        assertEquals(sqlException, exception.getCause());

        verify(preparedStatement).setInt(1, expenseId);
    }
}
