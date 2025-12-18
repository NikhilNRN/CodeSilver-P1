package expenseHistory;

import com.revature.api.ExpenseController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;
import io.javalin.http.Context;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

public class TestExpenseControllerHistory {
    // TODO: Declare mocks and inject mocks
    @Mock
    private static ExpenseService service;

    @Mock
    private static final Context ctx = mock(Context.class);

    @InjectMocks
    private static ExpenseController controller;

    // Test vars

    @BeforeAll
    public static void setUp() {
        // TODO: Set up test vars
        service = mock(ExpenseService.class);
        controller = new ExpenseController(service);
    }

    @AfterAll
    public static void tearDown() {
        //
    }

    // C09_03
    @Test
    public void testGetExpensesByEmployee_normal_success() {
        // TODO: Fix this, getting a nullpointerexception at line 48, ctx is null
        when(ctx.pathParamAsClass("employeeId", Integer.class).get()).thenReturn(1);
        // Stub service layer return
        List<ExpenseWithUser> validExpenseList = new ArrayList<>();
        when(service.getExpensesByEmployee(1)).thenReturn(validExpenseList);
        // Act - call the controller method
        // Assert that the controller method did not throw any exceptions
        Assertions.assertDoesNotThrow(() -> controller.getExpensesByEmployee(ctx), "No exceptions should be thrown");
        // Verify mocked behavior
        verify(service, times(1)).getExpensesByEmployee(1);
    }

    // C09_04
    @Test
    public void testGetExpensesByEmployee_invalidEmployeeIDFormat_throwsException() {
        // TODO: Implement
    }

    // C09_05
    @Test
    public void testGetExpensesByEmployee_serverError_throwsException() {
        // TODO: Implement
    }
}
