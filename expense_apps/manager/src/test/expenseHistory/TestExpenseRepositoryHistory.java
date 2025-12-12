package expenseHistory;

import com.revature.repository.DatabaseConnection;
import com.revature.repository.ExpenseRepository;
import com.revature.repository.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;

public class TestExpenseRepositoryHistory {
    @Mock
    private static DatabaseConnection conn;

    @InjectMocks
    private static ExpenseRepository repo;

    // Test variables

    @BeforeEach
    public void setUp() {
        //
    }

    @AfterEach
    public void tearDown() {
        //
    }

    // TS-1
    @Test
    public void testFindExpensesByAmount_throwsException() {
        fail("This requirement is currently unimplemented.");
    }

    // TS-2
    @Test
    public void testFindExpensesByUser_existingEmployee_returnsList() {
        // Arrange test vars
        // Mock database connection to return list

        // Act

        // Assert
    }

    // TS-3

    // TS-4
}
