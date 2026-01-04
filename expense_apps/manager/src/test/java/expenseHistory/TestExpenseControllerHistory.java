package expenseHistory;

import com.revature.api.ExpenseController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.validation.Validator;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Controller")
@Story("Viewing Expense History")
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
        // cleanup if needed
    }

    @Step("Mock context path parameter for employeeId: {employeeId}")
    private Validator<Integer> mockPathParam(Context ctx, int employeeId) {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(mockValidator);
        return mockValidator;
    }

    @Step("Call service to get expenses for employee ID: {employeeId}")
    private List<ExpenseWithUser> callServiceGetExpenses(int employeeId) {
        return service.getExpensesByEmployee(employeeId);
    }

    @Test
    @Description("Getting all expenses for an employee, valid user ID")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("C09_03")
    public void testGetExpensesByEmployee_normal_success() {
        Validator<Integer> mockValidator = mockPathParam(ctx, 1);

        List<ExpenseWithUser> validExpenseList = new ArrayList<>();
        when(service.getExpensesByEmployee(1)).thenReturn(validExpenseList);

        Assertions.assertDoesNotThrow(() -> controller.getExpensesByEmployee(ctx));

        verify(service, times(1)).getExpensesByEmployee(1);
    }

    @Step("Mock invalid employee ID path parameter")
    private Validator<Integer> mockInvalidPathParam(Context ctx) {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenThrow(NumberFormatException.class);
        when(ctx.pathParamAsClass(eq("employeeId"), any(Class.class))).thenReturn(mockValidator);
        return mockValidator;
    }

    @Test
    @Description("Getting all expenses for an employee, invalid user ID format")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C09_04")
    public void testGetExpensesByEmployee_invalidEmployeeIDFormat_throwsException() {
        Validator<Integer> mockValidator = mockInvalidPathParam(ctx);

        Assertions.assertThrows(BadRequestResponse.class,
                () -> controller.getExpensesByEmployee(ctx));

        verify(mockValidator).get();
    }

    @Step("Mock service throwing runtime exception for employee ID: {employeeId}")
    private void mockServiceError(int employeeId) {
        when(service.getExpensesByEmployee(employeeId)).thenThrow(RuntimeException.class);
    }

    @Test
    @Description("Getting all expenses for an employee, server error")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("C09_05")
    public void testGetExpensesByEmployee_serverError_throwsException() {
        Validator<Integer> mockValidator = mockPathParam(ctx, -999);

        mockServiceError(-999);

        Assertions.assertThrows(InternalServerErrorResponse.class,
                () -> controller.getExpensesByEmployee(ctx));

        verify(service, times(1)).getExpensesByEmployee(-999);
    }
}
