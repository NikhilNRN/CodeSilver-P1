package expenseApproval;

import com.revature.api.AuthenticationMiddleware;
import com.revature.api.ExpenseController;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.mockito.Mockito.*;

@DisplayName("ExpenseController Approval Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestExpenseControllerApproval {
    @Mock
    private ExpenseService service;

    @Mock
    private Context ctx;

    @Mock
    private AuthenticationMiddleware auth;

    @InjectMocks
    private ExpenseController controller;

    private User existingManager;
    private String validComment;

    @BeforeEach
    public void setUp() {
        service = mock(ExpenseService.class);
        ctx = mock(Context.class);
        auth = mock(AuthenticationMiddleware.class);
        controller = new ExpenseController(service);
        validComment = "comment";
        existingManager = new User(1, "manager1", "password123", "manager");
    }

    @Test
    @Order(1)
    @DisplayName("C20_05")
    public void testApproveExpense_success() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        Map<String, Object> mockBody = mock(Map.class);
        when(mockValidator.get()).thenReturn(1);
        when(mockBody.get("comment")).thenReturn(validComment);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
        // Stub authentication middleware
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        // Stub service layer
        when(service.approveExpense(1, existingManager.getId(), validComment)).thenReturn(true);

        Assertions.assertDoesNotThrow(() -> controller.approveExpense(ctx), "No exceptions should be thrown");
        verify(service, times(1)).approveExpense(1, existingManager.getId(), validComment);
    }

    @Test
    @Order(3)
    @DisplayName("C20_06")
    public void testApproveExpense_failure_throwsException() {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(null);
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenThrow(InternalServerErrorResponse.class);
        when(service.approveExpense(anyInt(), eq(existingManager.getId()), eq(null))).thenThrow(InternalServerErrorResponse.class);

        Assertions.assertThrows(InternalServerErrorResponse.class,
                () -> controller.approveExpense(ctx),
                "InternalServerErrorResponse should be thrown");
    }

    @Test
    @Order(2)
    @DisplayName("C20_07")
    public void testApproveExpense_invalidExpenseId_throwsException() {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenThrow(NumberFormatException.class);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        Assertions.assertThrows(BadRequestResponse.class, () -> controller.approveExpense(ctx), "BadRequestResponse should be thrown");
    }

    @Test
    @Order(2)
    @DisplayName("C20_08")
    public void testApproveExpense_expenseNotFound_throwsException() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        Map<String, Object> mockBody = mock(Map.class);
        when(mockValidator.get()).thenReturn(-999);
        when(mockBody.get("comment")).thenReturn(validComment);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
        // Stub authentication middleware
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        // Stub service layer
        when(service.approveExpense(-999, existingManager.getId(), validComment)).thenReturn(false);

        Assertions.assertThrows(NotFoundResponse.class, () -> controller.approveExpense(ctx), "NotFoundResponse exception should be thrown");
        verify(service, times(1)).approveExpense(-999, existingManager.getId(), validComment);
    }

    @Test
    @Order(1)
    @DisplayName("C21_05")
    public void testDenyExpense_success() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        Map<String, Object> mockBody = mock(Map.class);
        when(mockValidator.get()).thenReturn(1);
        when(mockBody.get("comment")).thenReturn(validComment);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
        // Stub authentication middleware
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        // Stub service layer
        when(service.denyExpense(-999, existingManager.getId(), validComment)).thenReturn(true);

        Assertions.assertThrows(NotFoundResponse.class, () -> controller.denyExpense(ctx), "NotFoundResponse exception should be thrown");
        verify(service, times(1)).denyExpense(1, existingManager.getId(), validComment);
    }

    @Test
    @Order(4)
    @DisplayName("C21_06")
    public void testDenyExpense_failure_throwsException() {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(null);
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenThrow(InternalServerErrorResponse.class);
        when(service.denyExpense(anyInt(), eq(existingManager.getId()), eq(null))).thenThrow(InternalServerErrorResponse.class);

        Assertions.assertThrows(InternalServerErrorResponse.class,
                () -> controller.denyExpense(ctx),
                "InternalServerErrorResponse should be thrown");
    }

    @Test
    @Order(2)
    @DisplayName("C21_07")
    public void testDenyExpense_invalidExpenseId_throwsException() {
        Validator<Integer> mockValidator = mock(Validator.class);
        when(mockValidator.get()).thenThrow(NumberFormatException.class);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        Assertions.assertThrows(BadRequestResponse.class, () -> controller.denyExpense(ctx), "BadRequestResponse should be thrown");
    }

    @Test
    @Order(2)
    @DisplayName("C21_08")
    public void testDenyExpense_expenseNotFound_throwsException() {
        // Stub context
        Validator<Integer> mockValidator = mock(Validator.class);
        Map<String, Object> mockBody = mock(Map.class);
        when(mockValidator.get()).thenReturn(-999);
        when(mockBody.get("comment")).thenReturn(validComment);
        when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
        when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
        // Stub authentication middleware
        when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        // Stub service layer
        when(service.denyExpense(-999, existingManager.getId(), validComment)).thenReturn(false);

        Assertions.assertThrows(NotFoundResponse.class, () -> controller.denyExpense(ctx), "NotFoundResponse exception should be thrown");
        verify(service, times(1)).denyExpense(-999, existingManager.getId(), validComment);
    }
}
