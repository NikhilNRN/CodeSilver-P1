package expenseReview;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.revature.repository.Expense;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.revature.api.ExpenseController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;

import java.util.List;
import java.util.Map;


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
    void getPendingExpenses_returnsPendingExpenses() {
        // mock service to return a sample list
        List<ExpenseWithUser> mockExpenses = List.of(mock(ExpenseWithUser.class));
        when(expenseService.getPendingExpenses()).thenReturn(mockExpenses);

        // call controller method
        expenseController.getPendingExpenses(ctx);

        // capture the argument passed to ctx.json()
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();

        // check if response contains the expected data
        assertEquals(true, response.get("success"));
        assertEquals(mockExpenses, response.get("data"));
        assertEquals(mockExpenses.size(), response.get("count"));
    }

    @Test
    void getPendingExpenses_handlesException() {
        // throw an exception
        when(expenseService.getPendingExpenses()).thenThrow(new RuntimeException("Service failed"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getPendingExpenses(ctx);
        });
    }

    @Test
    void getPendingExpenses_includesCorrectAmount() {
        // Create an ExpenseWithUser mock
        ExpenseWithUser mockExpenseWithUser = mock(ExpenseWithUser.class);
        Expense mockExpense = mock(Expense.class);

        // Stub getExpense() and getAmount()
        when(mockExpenseWithUser.getExpense()).thenReturn(mockExpense);
        when(mockExpense.getAmount()).thenReturn(100.0);

        // mock the service to return a list with this mocked ExpenseWithUser
        when(expenseService.getPendingExpenses()).thenReturn(List.of(mockExpenseWithUser));

        // call the controller method
        expenseController.getPendingExpenses(ctx);

        // capture the JSON response
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        List<ExpenseWithUser> data = (List<ExpenseWithUser>) response.get("data");

        // assert the amount is correct
        assertEquals(100.0, data.get(0).getExpense().getAmount());
    }

    @Test
    void getPendingExpenses_handlesEmptyList() {
        // mock service to return an empty list
        when(expenseService.getPendingExpenses()).thenReturn(List.of());

        // call the controller method
        expenseController.getPendingExpenses(ctx);

        // capture the JSON response passed to ctx.json()
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();

        // verify the response contains success = true, data = empty list, count = 0
        assertEquals(true, response.get("success"));
        assertTrue(((List<?>) response.get("data")).isEmpty());
        assertEquals(0, response.get("count"));
    }

    @Test
    void getPendingExpenses_throwsInternalServerError_onNull() {
        when(expenseService.getPendingExpenses()).thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getPendingExpenses(ctx);
        });
    }

    @ParameterizedTest
    @MethodSource("provideAmountsAndIndexes")
    void getPendingExpenses_includesAmountsCorrectly(double expectedAmount, int index) {
        // Build ExpenseWithUser list dynamically from all amounts once
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

    // Load amounts from CSV and provide arguments for parameterized tests
    static List<Double> amounts = loadAmountsFromCsv("/expense_amounts.csv");

    static List<Double> loadAmountsFromCsv(String resourcePath) {
        InputStream is = ExpenseControllerTest.class.getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("CSV file not found: " + resourcePath);
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            List<Double> amounts = br.lines()
                    .skip(1) // skip header line
                    .map(line -> line.split(",")[0]) // get first column
                    .map(Double::parseDouble) // convert to double
                    .toList();
            return amounts;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load CSV", e);
        }
    }

    static Stream<Arguments> provideAmountsAndIndexes() {
        return IntStream.range(0, amounts.size())
                .mapToObj(i -> Arguments.of(amounts.get(i), i));
    }

    // =======================
    // getAllExpenses tests
    // =======================

    @Test
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
    void getAllExpenses_handlesException() {
        when(expenseService.getAllExpenses())
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getAllExpenses(ctx);
        });
    }

    @Test
    void getAllExpenses_throwsInternalServerError_onNull() {
        when(expenseService.getAllExpenses()).thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getAllExpenses(ctx);
        });
    }

    @Test
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
        List<ExpenseWithUser> data =
                (List<ExpenseWithUser>) response.get("data");

        assertEquals(250.75, data.get(0).getExpense().getAmount());
    }

    // =======================
    // getExpensesByEmployee tests
    // =======================

    @Test
    void getExpensesByEmployee_returnsExpenses() {
        int employeeId = 123;
        List<ExpenseWithUser> mockExpenses = List.of(mock(ExpenseWithUser.class));

        io.javalin.validation.Validator validator =
                mock(io.javalin.validation.Validator.class);

        when(validator.get()).thenReturn(employeeId);

        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId))
                .thenReturn(mockExpenses);

        expenseController.getExpensesByEmployee(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();

        assertEquals(true, response.get("success"));
        assertEquals(mockExpenses, response.get("data"));
        assertEquals(mockExpenses.size(), response.get("count"));
        assertEquals(employeeId, response.get("employeeId"));
    }

    @Test
    void getExpensesByEmployee_handlesEmptyList() {
        int employeeId = 42;

        io.javalin.validation.Validator validator =
                mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);

        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId))
                .thenReturn(List.of());

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
    void getExpensesByEmployee_handlesException() {
        int employeeId = 5;

        io.javalin.validation.Validator validator =
                mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);

        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId))
                .thenThrow(new RuntimeException("DB error"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getExpensesByEmployee(ctx);
        });
    }

    @Test
    void getExpensesByEmployee_throwsInternalServerError_onNull() {
        int employeeId = 99;

        io.javalin.validation.Validator validator =
                mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);

        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId))
                .thenReturn(null);

        assertThrows(InternalServerErrorResponse.class, () -> {
            expenseController.getExpensesByEmployee(ctx);
        });
    }

    @Test
    void getExpensesByEmployee_invalidEmployeeId_throwsBadRequest() {
        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenThrow(new NumberFormatException("Invalid"));

        assertThrows(io.javalin.http.BadRequestResponse.class, () -> {
            expenseController.getExpensesByEmployee(ctx);
        });
    }

    @Test
    void getExpensesByEmployee_includesCorrectAmount() {
        int employeeId = 7;

        Expense expense = mock(Expense.class);
        ExpenseWithUser ewu = mock(ExpenseWithUser.class);

        when(expense.getAmount()).thenReturn(500.50);
        when(ewu.getExpense()).thenReturn(expense);

        io.javalin.validation.Validator validator =
                mock(io.javalin.validation.Validator.class);
        when(validator.get()).thenReturn(employeeId);

        when(ctx.pathParamAsClass(eq("employeeId"), eq(Integer.class)))
                .thenReturn(validator);

        when(expenseService.getExpensesByEmployee(employeeId))
                .thenReturn(List.of(ewu));

        expenseController.getExpensesByEmployee(ctx);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(ctx).json(captor.capture());

        Map<String, Object> response = captor.getValue();
        List<ExpenseWithUser> data =
                (List<ExpenseWithUser>) response.get("data");

        assertEquals(500.50, data.get(0).getExpense().getAmount());
    }

}
