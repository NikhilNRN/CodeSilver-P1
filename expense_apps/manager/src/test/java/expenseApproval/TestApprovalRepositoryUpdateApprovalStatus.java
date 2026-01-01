package expenseApproval;

import com.revature.repository.ApprovalRepository;
import com.revature.repository.DatabaseConnection;
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

    // TODO: Parameterize status so that all statuses are tested (stretch goal)
    @Test
    @DisplayName("C50_01")
    public void testUpdateApprovalStatus_normal_returnTrue() throws SQLException {
        // Stub Prepared Statement behavior
        when(pstmt.executeUpdate()).thenReturn(1);
        // Act & assert
        Assertions.assertTrue(repo.updateApprovalStatus(1, "approved", 2, "comment goes here"));
        // Verify mock behavior
        verify(dbConn).getConnection();
        verify(conn).prepareStatement(anyString());
        verify(pstmt).executeUpdate();
    }

    // TODO: Parameterize status so that all statuses are tested (stretch goal)
    @Test
    @DisplayName("C50_02")
    public void testUpdateApprovalStatus_invalidInput_returnFalse() throws SQLException {
        // Stub Prepared Statement behavior
        when(pstmt.executeUpdate()).thenReturn(0);
        // Act & assert
        Assertions.assertFalse(repo.updateApprovalStatus(
                -420, "approved", 2, "comment goes here"));
        // Verify mock behavior
        verify(dbConn).getConnection();
        verify(conn).prepareStatement(anyString());
        verify(pstmt).executeUpdate();
    }

    // TODO: Parameterize status so that all statuses are tested (stretch goal)
    @Test
    @DisplayName("C50_03")
    public void testUpdateApprovalStatus_SQLException_throwsException() throws SQLException {
        // Stub Prepared Statement behavior
        when(pstmt.executeUpdate()).thenThrow(SQLException.class);
        // Act & assert
        Assertions.assertThrows(RuntimeException.class,
                () -> repo.updateApprovalStatus(-27, "approved", 2, "comment goes here"),
                "RuntimeException should be thrown");
        // Verify mock behavior
        verify(dbConn).getConnection();
        verify(conn).prepareStatement(anyString());
        verify(pstmt).executeUpdate();
    }
}
