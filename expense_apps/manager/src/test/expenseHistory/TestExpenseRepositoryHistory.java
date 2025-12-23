package expenseHistory;

import com.revature.repository.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.function.Executable;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("ExpenseRepository History Tests")
public class TestExpenseRepositoryHistory {
    @Mock
    private static DatabaseConnection dbConn;

    @InjectMocks
    private static ExpenseRepository repo;

    // Test variables
    private Connection conn;
    private PreparedStatement pstmt;
    private ResultSet rs;

    @BeforeAll
    public static void setUpAll() {
        dbConn = mock(DatabaseConnection.class);
        repo = new ExpenseRepository(dbConn);
    }

    @BeforeEach
    public void setUp() {
        conn = mock(Connection.class);
        pstmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
        try {
            when(dbConn.getConnection()).thenReturn(conn);
            when(conn.prepareStatement(anyString())).thenReturn(pstmt);
            when(pstmt.executeQuery()).thenReturn(rs);
        } catch (SQLException e) {
            fail("Failed stubbing during setUp()");
        }
    }

    @AfterEach
    public void tearDown() {
        //
    }

    @Test
    @DisplayName("C11_01")
    @Disabled("Feature not yet implemented - CSP-11")
    public void testFindExpensesByAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    @DisplayName("C54_01")
    @Test
    public void testFindExpensesByUser_existingEmployee_returnsExpenseWithUserList() throws SQLException {
        // Arrange test vars by stubbing the result set
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getInt(anyString())).thenReturn(1);
        when(rs.getDouble(anyString())).thenReturn(2.0);
        when(rs.getString("description")).thenReturn("thing");
        when(rs.getString("date")).thenReturn("2000-01-01");
        when(rs.getString("username")).thenReturn("Jeffery");
        when(rs.getString("review_date")).thenReturn("2000-01-02");
        when(rs.getString("role")).thenReturn("Employee");
        when(rs.getString("status")).thenReturn("approved");
        when(rs.getString("comment")).thenReturn("I approve");
        when(rs.getObject("reviewer")).thenReturn(2);

        // Act
        List<ExpenseWithUser> actual = repo.findExpensesByUser(1);

        // Assert
        List<Executable> executables = actual.stream().map(expenseWithUser -> (Executable) () -> {
            assertEquals(1, expenseWithUser.getExpense().getId());
            assertEquals(1, expenseWithUser.getExpense().getUserId());
            assertEquals("thing", expenseWithUser.getExpense().getDescription());
            assertEquals("2000-01-01", expenseWithUser.getExpense().getDate());
            assertEquals(1, expenseWithUser.getUser().getId());
            assertEquals("Jeffery", expenseWithUser.getUser().getUsername());
            assertEquals("Employee", expenseWithUser.getUser().getRole());
            assertEquals(1, expenseWithUser.getApproval().getId());
            assertEquals(1, expenseWithUser.getApproval().getExpenseId());
            assertEquals("approved", expenseWithUser.getApproval().getStatus());
            assertEquals(2, expenseWithUser.getApproval().getReviewer());
            assertEquals("I approve", expenseWithUser.getApproval().getComment());
            assertEquals("2000-01-02", expenseWithUser.getApproval().getReviewDate());
        }).collect(Collectors.toList());
        assertEquals(2, actual.size());
        assertAll("All mocked expenses should be in list", executables);
        verify(pstmt).setInt(1, 1);
        verify(pstmt).executeQuery();
    }

    @DisplayName("C54_02")
    @Test
    public void testFindExpensesByUser_noSuchEmployee_returnsEmptyList() throws SQLException {
        when(rs.next()).thenReturn(false);
        List<ExpenseWithUser> actual = repo.findExpensesByUser(-999);
        assertTrue(actual.isEmpty());
        verify(pstmt).setInt(1, -999);
        verify(pstmt).executeQuery();
    }
}
