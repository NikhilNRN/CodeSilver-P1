package expenseReporting;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.revature.api.ReportController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import io.javalin.validation.Validator;

class ReportControllerTest {

    @Mock
    private ExpenseService expenseService;

    @Mock
    private Context ctx;

    private ReportController reportController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        reportController = new ReportController(expenseService);
    }

    // =======================
    // generateAllExpensesReport tests
    // =======================

    @Test
    void generateAllExpensesReport_success() {
        // arrange
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        // act
        reportController.generateAllExpensesReport(ctx);

        // assert
        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"all_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generateAllExpensesReport_handlesEmptyList() {
        // arrange
        List<ExpenseWithUser> expenses = List.of();
        String csv = "id,amount\n";

        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        // act
        reportController.generateAllExpensesReport(ctx);

        // assert
        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"all_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generateAllExpensesReport_throwsInternalServerError_whenServiceFails() {
        // arrange
        when(expenseService.getAllExpenses())
                .thenThrow(new RuntimeException("DB failure"));

        // act + assert
        assertThrows(InternalServerErrorResponse.class, () ->
                reportController.generateAllExpensesReport(ctx)
        );
    }

    @Test
    void generateAllExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        // arrange
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));

        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses))
                .thenThrow(new RuntimeException("CSV error"));

        // act + assert
        assertThrows(InternalServerErrorResponse.class, () ->
                reportController.generateAllExpensesReport(ctx)
        );
    }


    @Test
    void generateAllExpensesReport_throwsInternalServerError_onNullExpenses() {
        // arrange
        when(expenseService.getAllExpenses()).thenReturn(null);
        when(expenseService.generateCsvReport(null))
                .thenThrow(new RuntimeException("Null expenses"));

        // act + assert
        assertThrows(InternalServerErrorResponse.class, () ->
                reportController.generateAllExpensesReport(ctx)
        );
    }

    // ===========================
    // generateEmployeeExpensesReport tests
    // ===========================

    @Test
    void generateEmployeeExpensesReport_success() {
        int employeeId = 42;
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        // Mock Validator<Integer> returned by pathParamAsClass
        Validator<Integer> validatorMock = mock(Validator.class);

        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(validatorMock);
        when(validatorMock.get()).thenReturn(employeeId);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateEmployeeExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"employee_" + employeeId + "_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generateEmployeeExpensesReport_throwsBadRequestResponse_onInvalidEmployeeId() {
        // Simulate invalid integer parsing throwing NumberFormatException
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenThrow(new NumberFormatException());

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateEmployeeExpensesReport(ctx);
        });
    }

    @Test
    void generateEmployeeExpensesReport_throwsInternalServerError_whenServiceFails() {
        int employeeId = 99;

        @SuppressWarnings("unchecked")
        Validator<Integer> validatorMock = mock(Validator.class);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(validatorMock);
        when(validatorMock.get()).thenReturn(employeeId);

        when(expenseService.getExpensesByEmployee(employeeId)).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateEmployeeExpensesReport(ctx);
        });
    }

    @Test
    void generateEmployeeExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        int employeeId = 7;
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));

        @SuppressWarnings("unchecked")
        Validator<Integer> validatorMock = mock(Validator.class);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(validatorMock);
        when(validatorMock.get()).thenReturn(employeeId);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateEmployeeExpensesReport(ctx);
        });
    }

    // ===============================
    // generateCategoryExpensesReport tests
    // ===============================

    @Test
    void generateCategoryExpensesReport_success() {
        String category = "travel";
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        when(ctx.pathParam("category")).thenReturn(category);
        when(expenseService.getExpensesByCategory(category)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateCategoryExpensesReport(ctx);

        String safeCategory = category.replaceAll("[^a-zA-Z0-9_-]", "_");

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"category_" + safeCategory + "_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generateCategoryExpensesReport_throwsBadRequestResponse_onNullCategory() {
        when(ctx.pathParam("category")).thenReturn(null);

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateCategoryExpensesReport(ctx);
        });
    }

    @Test
    void generateCategoryExpensesReport_throwsBadRequestResponse_onEmptyCategory() {
        when(ctx.pathParam("category")).thenReturn("   ");  // whitespace only

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateCategoryExpensesReport(ctx);
        });
    }

    @Test
    void generateCategoryExpensesReport_throwsInternalServerError_whenServiceFails() {
        String category = "food";

        when(ctx.pathParam("category")).thenReturn(category);
        when(expenseService.getExpensesByCategory(category)).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateCategoryExpensesReport(ctx);
        });
    }

    @Test
    void generateCategoryExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        String category = "office";

        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        when(ctx.pathParam("category")).thenReturn(category);
        when(expenseService.getExpensesByCategory(category)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateCategoryExpensesReport(ctx);
        });
    }

    @Test
    void generateCategoryExpensesReport_sanitizesFilename() {
        String category = "travel/with*special?chars";
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        when(ctx.pathParam("category")).thenReturn(category);
        when(expenseService.getExpensesByCategory(category)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateCategoryExpensesReport(ctx);

        // Sanitized category with all non-alphanumeric/underscore/dash replaced with underscore
        String safeCategory = category.replaceAll("[^a-zA-Z0-9_-]", "_");

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"category_" + safeCategory + "_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    // ===============================
    // generateDateRangeExpensesReport tests
    // ===============================

    @Test
    void generateDateRangeExpensesReport_success() {
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,200.00";

        when(ctx.queryParam("startDate")).thenReturn(startDate);
        when(ctx.queryParam("endDate")).thenReturn(endDate);
        when(expenseService.getExpensesByDateRange(startDate, endDate)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateDateRangeExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"expenses_" + startDate + "_to_" + endDate + "_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generateDateRangeExpensesReport_throwsBadRequestResponse_whenMissingStartDate() {
        when(ctx.queryParam("startDate")).thenReturn(null);
        when(ctx.queryParam("endDate")).thenReturn("2025-01-31");

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateDateRangeExpensesReport(ctx);
        });
    }

    @Test
    void generateDateRangeExpensesReport_throwsBadRequestResponse_whenMissingEndDate() {
        when(ctx.queryParam("startDate")).thenReturn("2025-01-01");
        when(ctx.queryParam("endDate")).thenReturn(null);

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateDateRangeExpensesReport(ctx);
        });
    }

    @Test
    void generateDateRangeExpensesReport_throwsBadRequestResponse_whenInvalidDateFormat() {
        when(ctx.queryParam("startDate")).thenReturn("2025-13-01");  // invalid month
        when(ctx.queryParam("endDate")).thenReturn("2025-01-31");

        assertThrows(BadRequestResponse.class, () -> {
            reportController.generateDateRangeExpensesReport(ctx);
        });
    }

    @Test
    void generateDateRangeExpensesReport_throwsInternalServerError_whenServiceFails() {
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";

        when(ctx.queryParam("startDate")).thenReturn(startDate);
        when(ctx.queryParam("endDate")).thenReturn(endDate);
        when(expenseService.getExpensesByDateRange(startDate, endDate))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateDateRangeExpensesReport(ctx);
        });
    }

    @Test
    void generateDateRangeExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));

        when(ctx.queryParam("startDate")).thenReturn(startDate);
        when(ctx.queryParam("endDate")).thenReturn(endDate);
        when(expenseService.getExpensesByDateRange(startDate, endDate)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generateDateRangeExpensesReport(ctx);
        });
    }

    // ===============================
    // generatePendingExpensesReport tests
    // ===============================

    @Test
    void generatePendingExpensesReport_success() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,150.00";

        when(expenseService.getPendingExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generatePendingExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"pending_expenses_report.csv\""
        );
        verify(ctx).result(csv);
    }

    @Test
    void generatePendingExpensesReport_throwsInternalServerError_whenServiceFails() {
        when(expenseService.getPendingExpenses()).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generatePendingExpensesReport(ctx);
        });
    }

    @Test
    void generatePendingExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));

        when(expenseService.getPendingExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class, () -> {
            reportController.generatePendingExpensesReport(ctx);
        });
    }

}


