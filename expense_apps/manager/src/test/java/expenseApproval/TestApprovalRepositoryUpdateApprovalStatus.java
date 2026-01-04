package expenseApproval;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
import io.qameta.allure.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Approval Repository")
@Story("Update Approval Status")
@DisplayName("Approval Repository Update Approval Status Tests")
public class TestApprovalRepositoryUpdateApprovalStatus {

    @Mock
    private DatabaseConnection dbConn;

    @InjectMocks
    private ApprovalRepository repo;

    // Test variables
    private Connection conn;
    private PreparedStatement pstmt;

    @BeforeEach
    public void setUp() {
        dbConn = mock(DatabaseConnection.class);
        repo = new ApprovalRepository(dbConn);
        conn = mock(Connection.class);
        pstmt = mock(PreparedStatement.class);
        try {
            when(dbConn.getConnection()).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(pstmt);
        } catch (SQLException e) {
            fail("Failed stubbing during setUp()");
        }
    }

    @Description("Update approval status with valid inputs")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C50_01")
    public void testUpdateApprovalStatus_normal_returnTrue() throws SQLException {
        Allure.step("Stub PreparedStatement executeUpdate to return 1", () -> when(pstmt.executeUpdate()).thenReturn(1));

        Allure.step("Call updateApprovalStatus with valid inputs", () -> {
            boolean result = repo.updateApprovalStatus(1, "approved", 2, "comment goes here");
            Allure.step("Assert that updateApprovalStatus returned true", () -> Assertions.assertTrue(result));
        });

        Allure.step("Verify mock interactions", () -> {
            verify(dbConn).getConnection();
            verify(conn).prepareStatement(anyString());
            verify(pstmt).executeUpdate();
        });
    }


    @Description("Update approval status with invalid inputs")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @DisplayName("C50_02")
    public void testUpdateApprovalStatus_invalidInput_returnFalse() throws SQLException {
        Allure.step("Stub PreparedStatement executeUpdate to return 0", () -> when(pstmt.executeUpdate()).thenReturn(0));

        Allure.step("Call updateApprovalStatus with invalid expense ID", () -> {
            boolean result = repo.updateApprovalStatus(-420, "approved", 2, "comment goes here");
            Allure.step("Assert that updateApprovalStatus returned false", () -> Assertions.assertFalse(result));
        });

        Allure.step("Verify mock interactions", () -> {
            verify(dbConn).getConnection();
            verify(conn).prepareStatement(anyString());
            verify(pstmt).executeUpdate();
        });
    }

    @Description("Update approval status with SQLException")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @DisplayName("C50_03")
    public void testUpdateApprovalStatus_SQLException_throwsException() throws SQLException {
        Allure.step("Stub PreparedStatement executeUpdate to throw SQLException", () -> when(pstmt.executeUpdate()).thenThrow(SQLException.class));

        Allure.step("Call updateApprovalStatus expecting RuntimeException", () -> {
            Allure.step("Assert that RuntimeException is thrown", () -> {
                try {
                    repo.updateApprovalStatus(-27, "approved", 2, "comment goes here");
                    fail("Expected RuntimeException was not thrown");
                } catch (RuntimeException ex) {
                    // expected
                }
            });
        });

        Allure.step("Verify mock interactions", () -> {
            verify(dbConn).getConnection();
            verify(conn).prepareStatement(anyString());
            verify(pstmt).executeUpdate();
        });
    }
}
