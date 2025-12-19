package expenseHistory;

import com.revature.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestExpenseRepositoryHistory {
    @Mock
    private static DatabaseConnection dbConn;

    @InjectMocks
    private static ExpenseRepository repo;

    // Test variables
    private Connection conn;
    private PreparedStatement pstmt;
    private ResultSet rs;
    ExpenseWithUser existingExpenseWithUser;

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

    // C11-01
    @Test
    public void testFindExpensesByAmount_expectedFail() {
        fail("This requirement is currently unimplemented.");
    }

    // TS-???
    @Test
    public void testFindExpensesByUser_existingEmployee_returnsExpenseWithUserList() {
        // Arrange test vars by stubbing the result set
        try {
            when(rs.getInt(anyString())).thenReturn(1);
            when(rs.getDouble(anyString())).thenReturn(2.0);
            when(rs.getString("description")).thenReturn("thing");
            when(rs.getString("date")).thenReturn("2000-01-01");
            when(rs.getString("username")).thenReturn("Jeffery");
            when(rs.getString("review_date")).thenReturn(null);
            when(rs.getString("role")).thenReturn("Employee");
            when(rs.getString("status")).thenReturn("pending");
            when(rs.getString("comment")).thenReturn(null);
            when(rs.getObject("reviewer")).thenReturn(null);
        } catch (SQLException e) {
            throw new RuntimeException("SQLException while stubbing ResultSet" + e.getMessage());
        }
        existingExpenseWithUser = new ExpenseWithUser(
                new Expense(1, 1, 2.0, "thing", "2000-01-01"),
                new User(1, "Jeffery", null, "Employee"),
                new Approval(1, 1, "pending", null, null, null));
        List<ExpenseWithUser> expected = new ArrayList<>();
        expected.add(existingExpenseWithUser);

        // Act
        List<ExpenseWithUser> actual = repo.findExpensesByUser(1);

        // Assert
        assertIterableEquals(expected, actual);
    }

    // TS-???

    // TS-???
}
