package expenseReview;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.revature.repository.Expense;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.revature.api.ExpenseController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;

import io.qameta.allure.*;

@Epic("Expense Management")
@Feature("Expense Controller Operations")
public class ExpenseControllerTest {

    @Mock
    private ExpenseService expenseService;

    @Mock
    private Context ctx;

    private ExpenseController expenseController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        expenseController = new ExpenseController(expenseService);
    }

    // =======================
    // getPendingExpenses tests
    // =======================

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getPendingExpenses returns the expected list of pending expenses.")
    void getPendingExpenses_returnsPendingExpenses() {
        List<ExpenseWithUser> mockExpenses = List.of(mock(ExpenseWithUser.class));
        when(expenseService.getPendingExpenses()).thenReturn(mockExpenses);

        expenseController.getPendingExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertEquals(mockExpenses, response.get("data"));
        assertEquals(mockExpenses.size(), response.get("count"));
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that getPendingExpenses handles exceptions by throwing InternalServerErrorResponse.")
    void getPendingExpenses_handlesException() {
        when(expenseService.getPendingExpenses()).thenThrow(new RuntimeException("Service failed"));

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getPendingExpenses(ctx));
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that the amount in pending expenses is returned correctly.")
    void getPendingExpenses_includesCorrectAmount() {
        ExpenseWithUser mockExpenseWithUser = mock(ExpenseWithUser.class);
        Expense mockExpense = mock(Expense.class);
        when(mockExpenseWithUser.getExpense()).thenReturn(mockExpense);
        when(mockExpense.getAmount()).thenReturn(100.0);

        when(expenseService.getPendingExpenses()).thenReturn(List.of(mockExpenseWithUser));
        expenseController.getPendingExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        List<ExpenseWithUser> data = (List<ExpenseWithUser>) response.get("data");

        assertEquals(100.0, data.get(0).getExpense().getAmount());
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that getPendingExpenses handles empty lists correctly.")
    void getPendingExpenses_handlesEmptyList() {
        when(expenseService.getPendingExpenses()).thenReturn(List.of());
        expenseController.getPendingExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertTrue(((List<?>) response.get("data")).isEmpty());
        assertEquals(0, response.get("count"));
    }

    @Test
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getPendingExpenses throws InternalServerErrorResponse when service returns null.")
    void getPendingExpenses_throwsInternalServerError_onNull() {
        when(expenseService.getPendingExpenses()).thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getPendingExpenses(ctx));
    }

    @ParameterizedTest
    @MethodSource("provideAmountsAndIndexes")
    @Story("Retrieve Pending Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that amounts from CSV are included correctly in pending expenses.")
    void getPendingExpenses_includesAmountsCorrectly(double expectedAmount, int index) {
        List<ExpenseWithUser> expenses = amounts.stream().map(amount -> {
            Expense e = new Expense();
            e.setAmount(amount);
            ExpenseWithUser ewu = new ExpenseWithUser();
            ewu.setExpense(e);
            return ewu;
        }).collect(Collectors.toList());

        when(expenseService.getPendingExpenses()).thenReturn(expenses);
        expenseController.getPendingExpenses(ctx);

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map response = captor.getValue();
        List<?> data = (List<?>) response.get("data");

        assertEquals(amounts.size(), data.size());
        assertInstanceOf(ExpenseWithUser.class, data.get(index));

        ExpenseWithUser ewuResult = (ExpenseWithUser) data.get(index);
        assertEquals(expectedAmount, ewuResult.getExpense().getAmount(), 0.0001);
    }

    static List<Double> amounts = loadAmountsFromCsv("/expense_amounts.csv");

    static List<Double> loadAmountsFromCsv(String resourcePath) {
        InputStream is = ExpenseControllerTest.class.getResourceAsStream(resourcePath);
        if (is == null) throw new RuntimeException("CSV file not found: " + resourcePath);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines()
                    .skip(1)
                    .map(line -> line.split(",")[0])
                    .map(Double::parseDouble)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load CSV", e);
        }
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> provideAmountsAndIndexes() {
        return IntStream.range(0, amounts.size())
                .mapToObj(i -> org.junit.jupiter.params.provider.Arguments.of(amounts.get(i), i));
    }

    // =======================
    // getAllExpenses tests
    // =======================

    @Test
    @Story("Retrieve All Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getAllExpenses returns all expenses successfully.")
    void getAllExpenses_returnsAllExpenses() {
        List<ExpenseWithUser> mockExpenses = List.of(mock(ExpenseWithUser.class));
        when(expenseService.getAllExpenses()).thenReturn(mockExpenses);

        expenseController.getAllExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertEquals(mockExpenses, response.get("data"));
        assertEquals(mockExpenses.size(), response.get("count"));
    }

    @Test
    @Story("Retrieve All Expenses")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that getAllExpenses handles empty lists correctly.")
    void getAllExpenses_handlesEmptyList() {
        when(expenseService.getAllExpenses()).thenReturn(List.of());
        expenseController.getAllExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertTrue(((List<?>) response.get("data")).isEmpty());
        assertEquals(0, response.get("count"));
    }

    @Test
    @Story("Retrieve All Expenses")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that getAllExpenses throws InternalServerErrorResponse when an exception occurs.")
    void getAllExpenses_handlesException() {
        when(expenseService.getAllExpenses()).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getAllExpenses(ctx));
    }

    @Test
    @Story("Retrieve All Expenses")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getAllExpenses throws InternalServerErrorResponse when service returns null.")
    void getAllExpenses_throwsInternalServerError_onNull() {
        when(expenseService.getAllExpenses()).thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getAllExpenses(ctx));
    }

    @Test
    @Story("Retrieve All Expenses")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that amounts in getAllExpenses response are correct.")
    void getAllExpenses_includesCorrectAmount() {
        ExpenseWithUser ewu = mock(ExpenseWithUser.class);
        Expense expense = mock(Expense.class);

        when(ewu.getExpense()).thenReturn(expense);
        when(expense.getAmount()).thenReturn(250.75);

        when(expenseService.getAllExpenses()).thenReturn(List.of(ewu));

        expenseController.getAllExpenses(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        List<ExpenseWithUser> data = (List<ExpenseWithUser>) response.get("data");
        assertEquals(250.75, data.get(0).getExpense().getAmount());
    }

    // =======================
    // getExpensesByEmployee tests
    // =======================

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getExpensesByEmployee returns the expected list of expenses for a given employee.")
    void getExpensesByEmployee_returnsExpenses() {
        int employeeId = 123;
        List<ExpenseWithUser> mockExpenses = List.of(mock(ExpenseWithUser.class));

        io.javalin.validation.Validator validator = mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class))).thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(mockExpenses);

        expenseController.getExpensesByEmployee(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertEquals(mockExpenses, response.get("data"));
        assertEquals(mockExpenses.size(), response.get("count"));
        assertEquals(employeeId, response.get("employeeId"));
    }

    // =======================
    // getExpensesByEmployee tests
    // =======================

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.MINOR)
    @Description("Verify that getExpensesByEmployee handles empty lists correctly.")
    void getExpensesByEmployee_handlesEmptyList() {
        int employeeId = 42;

        io.javalin.validation.Validator validator = mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class))).thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(List.of());

        expenseController.getExpensesByEmployee(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        assertEquals(true, response.get("success"));
        assertTrue(((List<?>) response.get("data")).isEmpty());
        assertEquals(0, response.get("count"));
        assertEquals(employeeId, response.get("employeeId"));
    }

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Verify that getExpensesByEmployee throws InternalServerErrorResponse when service throws an exception.")
    void getExpensesByEmployee_handlesException() {
        int employeeId = 5;

        io.javalin.validation.Validator validator = mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class))).thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId)).thenThrow(new RuntimeException("DB error"));

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getExpensesByEmployee(ctx));
    }

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify that getExpensesByEmployee throws InternalServerErrorResponse when service returns null.")
    void getExpensesByEmployee_throwsInternalServerError_onNull() {
        int employeeId = 99;

        io.javalin.validation.Validator validator = mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class))).thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> expenseController.getExpensesByEmployee(ctx));
    }

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that getExpensesByEmployee throws BadRequestResponse for invalid employeeId format.")
    void getExpensesByEmployee_invalidEmployeeId_throwsBadRequest() {
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenThrow(new NumberFormatException("Invalid"));

        assertThrows(io.javalin.http.BadRequestResponse.class, () -> expenseController.getExpensesByEmployee(ctx));
    }

    @Test
    @Story("Retrieve Expenses By Employee")
    @Severity(SeverityLevel.NORMAL)
    @Description("Verify that getExpensesByEmployee returns the correct expense amount for a given employee.")
    void getExpensesByEmployee_includesCorrectAmount() {
        int employeeId = 7;

        Expense expense = mock(Expense.class);
        ExpenseWithUser ewu = mock(ExpenseWithUser.class);

        when(expense.getAmount()).thenReturn(500.50);
        when(ewu.getExpense()).thenReturn(expense);

        io.javalin.validation.Validator validator = mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class))).thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(List.of(ewu));

        expenseController.getExpensesByEmployee(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        List<ExpenseWithUser> data = (List<ExpenseWithUser>) response.get("data");

        assertEquals(500.50, data.get(0).getExpense().getAmount());
    }

}

