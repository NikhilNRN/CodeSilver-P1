package expenseReporting;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.revature.api.ReportController;
import com.revature.repository.ExpenseWithUser;
import com.revature.service.ExpenseService;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.InternalServerErrorResponse;
import io.javalin.validation.Validator;

import io.qameta.allure.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

@Epic("Expense Reporting API")
@Feature("ReportController Tests")
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
    // generateAllExpensesReport
    // =======================
    @Test
    @Story("Generate all expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateAllExpensesReport - success")
    void generateAllExpensesReport_success() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateAllExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header("Content-Disposition",
                "attachment; filename=\"all_expenses_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate all expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateAllExpensesReport - empty list")
    void generateAllExpensesReport_handlesEmptyList() {
        List<ExpenseWithUser> expenses = List.of();
        String csv = "id,amount\n";

        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateAllExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header("Content-Disposition",
                "attachment; filename=\"all_expenses_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate all expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateAllExpensesReport - DB failure")
    void generateAllExpensesReport_throwsInternalServerError_whenServiceFails() {
        when(expenseService.getAllExpenses()).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generateAllExpensesReport(ctx));
    }

    @Test
    @Story("Generate all expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateAllExpensesReport - CSV generation failure")
    void generateAllExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        when(expenseService.getAllExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generateAllExpensesReport(ctx));
    }

    // =======================
    // generateEmployeeExpensesReport
    // =======================
    @Test
    @Story("Generate employee expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateEmployeeExpensesReport - success")
    void generateEmployeeExpensesReport_success() {
        int employeeId = 42;
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,100.00";

        @SuppressWarnings("unchecked")
        Validator<Integer> validatorMock = mock(Validator.class);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(validatorMock);
        when(validatorMock.get()).thenReturn(employeeId);

        when(expenseService.getExpensesByEmployee(employeeId)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generateEmployeeExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header("Content-Disposition",
                "attachment; filename=\"employee_" + employeeId + "_expenses_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate employee expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateEmployeeExpensesReport - invalid employee ID")
    void generateEmployeeExpensesReport_throwsBadRequestResponse_onInvalidEmployeeId() {
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenThrow(new NumberFormatException());
        assertThrows(BadRequestResponse.class,
                () -> reportController.generateEmployeeExpensesReport(ctx));
    }

    @Test
    @Story("Generate employee expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateEmployeeExpensesReport - DB failure")
    void generateEmployeeExpensesReport_throwsInternalServerError_whenServiceFails() {
        int employeeId = 99;

        @SuppressWarnings("unchecked")
        Validator<Integer> validatorMock = mock(Validator.class);
        when(ctx.pathParamAsClass("employeeId", Integer.class)).thenReturn(validatorMock);
        when(validatorMock.get()).thenReturn(employeeId);

        when(expenseService.getExpensesByEmployee(employeeId)).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generateEmployeeExpensesReport(ctx));
    }

    // =======================
    // generateCategoryExpensesReport
    // =======================
    @Test
    @Story("Generate category expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateCategoryExpensesReport - success")
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
        verify(ctx).header("Content-Disposition",
                "attachment; filename=\"category_" + safeCategory + "_expenses_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate category expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateCategoryExpensesReport - invalid category")
    void generateCategoryExpensesReport_throwsBadRequestResponse_onNullCategory() {
        when(ctx.pathParam("category")).thenReturn(null);
        assertThrows(BadRequestResponse.class,
                () -> reportController.generateCategoryExpensesReport(ctx));
    }

    @Test
    @Story("Generate category expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateCategoryExpensesReport - empty category")
    void generateCategoryExpensesReport_throwsBadRequestResponse_onEmptyCategory() {
        when(ctx.pathParam("category")).thenReturn("   ");
        assertThrows(BadRequestResponse.class,
                () -> reportController.generateCategoryExpensesReport(ctx));
    }

    // ===============================
    // generateDateRangeExpensesReport
    // ===============================
    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateDateRangeExpensesReport - success")
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
                "attachment; filename=\"expenses_" + startDate + "_to_" + endDate + "_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateDateRangeExpensesReport - missing start date")
    void generateDateRangeExpensesReport_throwsBadRequest_whenMissingStartDate() {
        when(ctx.queryParam("startDate")).thenReturn(null);
        when(ctx.queryParam("endDate")).thenReturn("2025-01-31");

        assertThrows(BadRequestResponse.class,
                () -> reportController.generateDateRangeExpensesReport(ctx));
    }

    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateDateRangeExpensesReport - missing end date")
    void generateDateRangeExpensesReport_throwsBadRequest_whenMissingEndDate() {
        when(ctx.queryParam("startDate")).thenReturn("2025-01-01");
        when(ctx.queryParam("endDate")).thenReturn(null);

        assertThrows(BadRequestResponse.class,
                () -> reportController.generateDateRangeExpensesReport(ctx));
    }

    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.NORMAL)
    @DisplayName("generateDateRangeExpensesReport - invalid date format")
    void generateDateRangeExpensesReport_throwsBadRequest_whenInvalidDateFormat() {
        when(ctx.queryParam("startDate")).thenReturn("2025-13-01"); // invalid month
        when(ctx.queryParam("endDate")).thenReturn("2025-01-31");

        assertThrows(BadRequestResponse.class,
                () -> reportController.generateDateRangeExpensesReport(ctx));
    }

    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateDateRangeExpensesReport - DB failure")
    void generateDateRangeExpensesReport_throwsInternalServerError_whenServiceFails() {
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";

        when(ctx.queryParam("startDate")).thenReturn(startDate);
        when(ctx.queryParam("endDate")).thenReturn(endDate);
        when(expenseService.getExpensesByDateRange(startDate, endDate))
                .thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generateDateRangeExpensesReport(ctx));
    }

    @Test
    @Story("Generate date-range expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generateDateRangeExpensesReport - CSV generation failure")
    void generateDateRangeExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        String startDate = "2025-01-01";
        String endDate = "2025-01-31";
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));

        when(ctx.queryParam("startDate")).thenReturn(startDate);
        when(ctx.queryParam("endDate")).thenReturn(endDate);
        when(expenseService.getExpensesByDateRange(startDate, endDate)).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generateDateRangeExpensesReport(ctx));
    }

    // ===============================
    // generatePendingExpensesReport
    // ===============================
    @Test
    @Story("Generate pending expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generatePendingExpensesReport - success")
    void generatePendingExpensesReport_success() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        String csv = "id,amount\n1,150.00";

        when(expenseService.getPendingExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenReturn(csv);

        reportController.generatePendingExpensesReport(ctx);

        verify(ctx).contentType("text/csv");
        verify(ctx).header(
                "Content-Disposition",
                "attachment; filename=\"pending_expenses_report.csv\"");
        verify(ctx).result(csv);
    }

    @Test
    @Story("Generate pending expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generatePendingExpensesReport - DB failure")
    void generatePendingExpensesReport_throwsInternalServerError_whenServiceFails() {
        when(expenseService.getPendingExpenses()).thenThrow(new RuntimeException("DB failure"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generatePendingExpensesReport(ctx));
    }

    @Test
    @Story("Generate pending expenses report")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("generatePendingExpensesReport - CSV generation failure")
    void generatePendingExpensesReport_throwsInternalServerError_whenCsvGenerationFails() {
        List<ExpenseWithUser> expenses = List.of(mock(ExpenseWithUser.class));
        when(expenseService.getPendingExpenses()).thenReturn(expenses);
        when(expenseService.generateCsvReport(expenses)).thenThrow(new RuntimeException("CSV error"));

        assertThrows(InternalServerErrorResponse.class,
                () -> reportController.generatePendingExpensesReport(ctx));
    }

}
