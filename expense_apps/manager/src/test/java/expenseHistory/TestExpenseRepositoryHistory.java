package expenseHistory;

import com.revature.repository.*;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Repository")
@Story("Viewing Expense History")
@DisplayName("ExpenseRepository History Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestExpenseRepositoryHistory {

    @Mock
    private static DatabaseConnection dbConn;

    @InjectMocks
    private static ExpenseRepository repo;

    private Connection conn;
    private PreparedStatement pstmt;
    private ResultSet rs;

    @BeforeAll
    public static void setUpAll() {
        dbConn = mock(DatabaseConnection.class);
        repo = new ExpenseRepository(dbConn);
    }

    @BeforeEach
    public void setUp() throws SQLException {
        conn = mock(Connection.class);
        pstmt = mock(PreparedStatement.class);
        rs = mock(ResultSet.class);
        when(dbConn.getConnection()).thenReturn(conn);
    }

    // -----------------------------
    // Step-level methods
    // -----------------------------
    @Step("Stub result set for expenses")
    private void stubResultSetForExpenses(ResultSet rs) throws SQLException {
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
    }

    @Step("Call repository to find expenses by user ID: {userId}")
    private List<ExpenseWithUser> findExpensesByUser(int userId) {
        return repo.findExpensesByUser(userId);
    }

    @Step("Call repository to find expenses by date range: {startDate} to {endDate}")
    private List<ExpenseWithUser> findExpensesByDateRange(String startDate, String endDate) {
        return repo.findExpensesByDateRange(startDate, endDate);
    }

    // -----------------------------
    // Tests
    // -----------------------------
    @Description("Finding all expenses from an existing employee by user ID")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C54_01")
    @Test
    @Order(1)
    public void testFindExpensesByUser_existingEmployee_returnsExpenseWithUserList() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(rs);
        stubResultSetForExpenses(rs);

        List<ExpenseWithUser> actual = findExpensesByUser(1);

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

    @Description("Finding expenses for a non-existent employee ID")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C54_02")
    @Test
    @Order(2)
    public void testFindExpensesByUser_noSuchEmployee_returnsEmptyList() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<ExpenseWithUser> actual = findExpensesByUser(-999);

        assertTrue(actual.isEmpty());
        verify(pstmt).setInt(1, -999);
        verify(pstmt).executeQuery();
    }

    @Description("Finding expenses by user throws an SQLException")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C54_03")
    @Test
    @Order(3)
    public void testFindExpensesByUser_SQLException_throwsException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(SQLException.class);

        assertThrows(RuntimeException.class,
                () -> findExpensesByUser(-69));

        verify(pstmt, never()).setInt(1, -69);
    }

    @Description("Finding all expenses within a valid date range")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C55_01")
    @Test
    @Order(4)
    public void testFindExpensesByDateRange_validRange_returnsExpenseWithUserList() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(rs);
        stubResultSetForExpenses(rs);

        List<ExpenseWithUser> actual = findExpensesByDateRange("1999-01-01", "2002-12-31");

        assertEquals(2, actual.size());
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
        assertAll("All mocked expenses should be in list", executables);

        verify(pstmt).setString(1, "1999-01-01");
        verify(pstmt).setString(2, "2002-12-31");
        verify(pstmt).executeQuery();
    }

    @Description("Finding expenses for an invalid date range")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C55_02")
    @Test
    @Order(5)
    public void testFindExpensesByDateRange_invalidRange_returnsEmptyList() throws SQLException {
        when(conn.prepareStatement(anyString())).thenReturn(pstmt);
        when(pstmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        List<ExpenseWithUser> actual = findExpensesByDateRange("2025-10-33", "2001-11-09");

        assertTrue(actual.isEmpty());
        verify(pstmt).setString(1, "2025-10-33");
        verify(pstmt).setString(2, "2001-11-09");
        verify(pstmt).executeQuery();
    }

    @Description("Finding expenses by date range throws an SQLException")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C55_03")
    @Test
    @Order(6)
    public void testFindExpensesByDateRange_SQLException_throwsException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(SQLException.class);

        assertThrows(RuntimeException.class,
                () -> findExpensesByDateRange("-69", "waga bo"));

        verify(pstmt, never()).setString(1, "-69");
        verify(pstmt, never()).setString(2, "waga bo");
    }
}
