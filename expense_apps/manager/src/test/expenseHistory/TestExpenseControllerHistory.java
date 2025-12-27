package expenseHistory;

import com.revature.api.ExpenseController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@DisplayName("ExpenseController History Tests")
public class TestExpenseControllerHistory {
    @Mock
    private static ExpenseService service;

    @Mock
    private static Context ctx;

    @InjectMocks
    private static ExpenseController controller;

    @BeforeAll
    public static void setUp() {
        service = mock(ExpenseService.class);
        ctx = mock(Context.class);
        controller = new ExpenseController(service);
    }

    @AfterAll
    public static void tearDown() {
        //
    }

    // C09_03
    @Test
    @DisplayName("C09_03")
    public void testGetExpensesByEmployee_normal_success() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenReturn(1);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(mockValidator);
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
    @DisplayName("C09_04")
    public void testGetExpensesByEmployee_invalidEmployeeIDFormat_throwsException() {
        // Stub to mock context behavior
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenThrow(NumberFormatException.class);
        when(ctx.pathParamAsClass(eq("employeeId"), any(Class.class))).thenReturn(mockValidator);
        // Act - call the controller method
        // Assert that the controller method throws an exception
        Assertions.assertThrows(BadRequestResponse.class,
                () -> controller.getExpensesByEmployee(ctx),
                "BadRequestResponse exception should be thrown");
        // Verify mocked behavior
        verify(mockValidator).get();
    }

    // C09_05
    @Test
    @DisplayName("C09_05")
    public void testGetExpensesByEmployee_serverError_throwsException() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenReturn(-999);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(mockValidator);
        // Stub service layer
        when(service.getExpensesByEmployee(-999)).thenThrow(RuntimeException.class);
        // Act - call the controller method
        // Assert that the controller method throws an exception
        Assertions.assertThrows(InternalServerErrorResponse.class,
                () -> controller.getExpensesByEmployee(ctx),
                "InternalServerErrorResponse exception should be thrown");
        // Verify mocked behavior
        verify(service, times(1)).getExpensesByEmployee(-999);
    }
}
