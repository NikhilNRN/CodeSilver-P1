package expenseApproval;

import com.revature.api.AuthenticationMiddleware;
import com.revature.api.ExpenseController;
import com.revature.repository.User;
import com.revature.service.ExpenseService;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.validation.Validator;
import io.qameta.allure.*;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Map;

import static org.mockito.Mockito.*;

@Epic("Manager App")
@Feature("Expense Controller")
@DisplayName("ExpenseController Approval Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestExpenseControllerApproval {

    @Mock
    private ExpenseService service;

    @Mock
    private io.javalin.http.Context ctx;

    @Mock
    private AuthenticationMiddleware auth;

    @InjectMocks
    private ExpenseController controller;

    private User existingManager;
    private String validComment;

    @BeforeEach
    public void setUp() {
        service = mock(ExpenseService.class);
        ctx = mock(io.javalin.http.Context.class);
        auth = mock(AuthenticationMiddleware.class);
        controller = new ExpenseController(service);
        validComment = "comment";
        existingManager = new User(1, "manager1", "password123", "manager");
    }

    @Story("Expense Approval")
    @Description("Successful expense approval, no exceptions should be thrown")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @Order(1)
    @DisplayName("C20_05")
    public void testApproveExpense_success() {
        Allure.step("Stub context and authentication", () -> {
            Validator<Integer> mockValidator = mock(Validator.class);
            Map<String, Object> mockBody = mock(Map.class);

            when(mockValidator.get()).thenReturn(1);
            when(mockBody.get("comment")).thenReturn(validComment);
            when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
            when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
            when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        });

        Allure.step("Stub service layer to return success", () -> {
            when(service.approveExpense(1, existingManager.getId(), validComment)).thenReturn(true);
        });

        Allure.step("Call approveExpense and assert no exception", () -> {
            Assertions.assertDoesNotThrow(() -> controller.approveExpense(ctx));
        });

        Allure.step("Verify service layer interaction", () -> {
            verify(service, times(1)).approveExpense(1, existingManager.getId(), validComment);
        });
    }

    @Story("Expense Approval")
    @Description("Expense approval with malformed request throws BadRequestResponse")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @Order(2)
    @DisplayName("C20_07")
    public void testApproveExpense_invalidExpenseId_throwsException() {
        Allure.step("Stub context to throw NumberFormatException", () -> {
            Validator<Integer> mockValidator = mock(Validator.class);
            when(mockValidator.get()).thenThrow(NumberFormatException.class);
            when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
            when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        });

        Allure.step("Call approveExpense and assert BadRequestResponse", () -> {
            Assertions.assertThrows(BadRequestResponse.class, () -> controller.approveExpense(ctx));
        });
    }

    @Story("Expense Approval")
    @Description("Expense approval for non-existent expense throws NotFoundResponse")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @Order(2)
    @DisplayName("C20_08")
    public void testApproveExpense_expenseNotFound_throwsException() {
        Allure.step("Stub context and authentication", () -> {
            Validator<Integer> mockValidator = mock(Validator.class);
            Map<String, Object> mockBody = mock(Map.class);
            when(mockValidator.get()).thenReturn(-999);
            when(mockBody.get("comment")).thenReturn(validComment);
            when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
            when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
            when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        });

        Allure.step("Stub service to return false for non-existent expense", () -> {
            when(service.approveExpense(-999, existingManager.getId(), validComment)).thenReturn(false);
        });

        Allure.step("Call approveExpense and assert NotFoundResponse", () -> {
            Assertions.assertThrows(NotFoundResponse.class, () -> controller.approveExpense(ctx));
            verify(service, times(1)).approveExpense(-999, existingManager.getId(), validComment);
        });
    }

    @Story("Expense Denial")
    @Description("Successful expense denial, expense found")
    @Severity(SeverityLevel.CRITICAL)
    @Test
    @Order(1)
    @DisplayName("C21_05")
    public void testDenyExpense_success() {
        Allure.step("Stub context and authentication", () -> {
            Validator<Integer> mockValidator = mock(Validator.class);
            Map<String, Object> mockBody = mock(Map.class);
            when(mockValidator.get()).thenReturn(1);
            when(mockBody.get("comment")).thenReturn(validComment);
            when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
            when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
            when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        });

        Allure.step("Stub service layer to deny expense", () -> {
            when(service.denyExpense(1, existingManager.getId(), validComment)).thenReturn(true);
        });

        Allure.step("Call denyExpense and assert no exception", () -> {
            Assertions.assertDoesNotThrow(() -> controller.denyExpense(ctx));
            verify(service, times(1)).denyExpense(1, existingManager.getId(), validComment);
        });
    }

    @Story("Expense Denial")
    @Description("Denied expense not found throws NotFoundResponse")
    @Severity(SeverityLevel.BLOCKER)
    @Test
    @Order(2)
    @DisplayName("C21_08")
    public void testDenyExpense_expenseNotFound_throwsException() {
        Allure.step("Stub context and authentication", () -> {
            Validator<Integer> mockValidator = mock(Validator.class);
            Map<String, Object> mockBody = mock(Map.class);
            when(mockValidator.get()).thenReturn(-999);
            when(mockBody.get("comment")).thenReturn(validComment);
            when(ctx.pathParamAsClass("expenseId", Integer.class)).thenReturn(mockValidator);
            when(ctx.bodyAsClass(Map.class)).thenReturn(mockBody);
            when(AuthenticationMiddleware.getAuthenticatedManager(ctx)).thenReturn(existingManager);
        });

        Allure.step("Stub service layer to return false", () -> {
            when(service.denyExpense(-999, existingManager.getId(), validComment)).thenReturn(false);
        });

        Allure.step("Call denyExpense and assert NotFoundResponse", () -> {
            Assertions.assertThrows(NotFoundResponse.class, () -> controller.denyExpense(ctx));
            verify(service, times(1)).denyExpense(-999, existingManager.getId(), validComment);
        });
    }
}
