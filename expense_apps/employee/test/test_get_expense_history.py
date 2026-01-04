import allure
import pytest
from unittest.mock import Mock
from service import ExpenseService


# Fixtures used:
@pytest.fixture
def mock_expense_repository():
    """Mock for expense repository"""
    return Mock()


@pytest.fixture
def mock_approval_repository():
    """Mock for approval repository"""
    return Mock()


@pytest.fixture
def expense_service(mock_expense_repository, mock_approval_repository):
    """ExpenseService instance with mocked dependencies"""
    return ExpenseService(
        expense_repository=mock_expense_repository,
        approval_repository=mock_approval_repository,
    )


# Helper function:
def make_expense_with_status(status: str):
    """Helper to create expense-approval tuples with specified status"""
    expense = Mock(name="Expense")
    approval = Mock(name="Approval")
    approval.status = status
    return expense, approval


@allure.epic("Employee App")
@allure.feature("Expense Service")
@allure.story("Expense History Retrieval")
@pytest.mark.expense_history
class TestGetExpenseHistory:
    """Test suite for expense history retrieval with status filtering"""

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Get expense history without filter returns all expenses")
    @allure.description("""
        Test to verify that get_expense_history returns all expenses 
        when no status filter is provided.
        Validates:
        - All expenses are returned regardless of status
        - No filtering is applied
    """)
    @allure.testcase("EXP_HIST_01")
    def test_get_expense_history_returns_all_when_no_filter(self, expense_service):
        with allure.step("Arrange: Set up test data with multiple statuses"):
            user_id = 1
            expenses = [
                make_expense_with_status("pending"),
                make_expense_with_status("approved"),
            ]
            allure.attach(
                f"User ID: {user_id}\n"
                f"Total Expenses: {len(expenses)}\n"
                f"Statuses: pending, approved",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status to return all expenses"):
            expense_service.get_user_expenses_with_status = Mock(return_value=expenses)
            allure.attach(
                f"Mocked to return {len(expenses)} expenses",
                "Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Act: Call get_expense_history without status filter"):
            result = expense_service.get_expense_history(user_id)
            allure.attach(
                f"Returned {len(result)} expenses",
                "Result",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify all expenses are returned"):
            assert result == expenses, f"Expected {len(expenses)} expenses but got {len(result)}"
            allure.attach(
                f"✓ All {len(expenses)} expenses returned (no filtering applied)",
                "Verification Success",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Get expense history with 'pending' filter returns only pending expenses")
    @allure.description("""
        Test to verify that get_expense_history correctly filters and returns 
        only expenses with 'pending' status.
        Validates:
        - Only pending expenses are returned
        - Other statuses are excluded
    """)
    @allure.testcase("EXP_HIST_02")
    def test_get_expense_history_filters_pending(self, expense_service):
        with allure.step("Arrange: Set up expenses with mixed statuses"):
            user_id = 2
            expenses = [
                make_expense_with_status("pending"),
                make_expense_with_status("approved"),
                make_expense_with_status("pending"),
            ]
            allure.attach(
                f"User ID: {user_id}\n"
                f"Total Expenses: {len(expenses)}\n"
                f"Statuses: pending, approved, pending\n"
                f"Expected pending count: 2",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status"):
            expense_service.get_user_expenses_with_status = Mock(return_value=expenses)
            allure.attach(
                f"Mocked to return {len(expenses)} expenses with mixed statuses",
                "Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Act: Call get_expense_history with status_filter='pending'"):
            result = expense_service.get_expense_history(user_id, status_filter="pending")
            allure.attach(
                f"Filter: 'pending'\nReturned: {len(result)} expenses",
                "Filter Applied",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify only 2 pending expenses are returned"):
            assert len(result) == 2, f"Expected 2 pending expenses but got {len(result)}"
            allure.attach(
                f"✓ Correct count: {len(result)} pending expenses",
                "Count Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify all returned expenses have 'pending' status"):
            assert all(approval.status == "pending" for _, approval in result), \
                "Not all returned expenses have 'pending' status"
            allure.attach(
                "✓ All returned expenses have status='pending'",
                "Status Verification",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.NORMAL)
    @allure.title("Get expense history with 'approved' filter returns only approved expenses")
    @allure.description("""
        Test to verify that get_expense_history correctly filters and returns 
        only expenses with 'approved' status.
    """)
    @allure.testcase("EXP_HIST_03")
    def test_get_expense_history_filters_approved(self, expense_service):
        with allure.step("Arrange: Set up expenses with approved and denied statuses"):
            user_id = 3
            expenses = [
                make_expense_with_status("approved"),
                make_expense_with_status("denied"),
            ]
            allure.attach(
                f"User ID: {user_id}\n"
                f"Total Expenses: {len(expenses)}\n"
                f"Statuses: approved, denied\n"
                f"Expected approved count: 1",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status"):
            expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

        with allure.step("Act: Call get_expense_history with status_filter='approved'"):
            result = expense_service.get_expense_history(user_id, status_filter="approved")
            allure.attach(
                f"Filter: 'approved'\nReturned: {len(result)} expenses",
                "Filter Applied",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify only the approved expense is returned"):
            assert result == [expenses[0]], \
                f"Expected only the approved expense but got {len(result)} expenses"
            allure.attach(
                "✓ Returned exactly 1 approved expense (correct filtering)",
                "Verification Success",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.NORMAL)
    @allure.title("Get expense history with 'denied' filter returns only denied expenses")
    @allure.description("""
        Test to verify that get_expense_history correctly filters and returns 
        only expenses with 'denied' status.
    """)
    @allure.testcase("EXP_HIST_04")
    def test_get_expense_history_filters_denied(self, expense_service):
        with allure.step("Arrange: Set up expenses with denied and approved statuses"):
            user_id = 4
            expenses = [
                make_expense_with_status("denied"),
                make_expense_with_status("approved"),
            ]
            allure.attach(
                f"User ID: {user_id}\n"
                f"Total Expenses: {len(expenses)}\n"
                f"Statuses: denied, approved\n"
                f"Expected denied count: 1",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status"):
            expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

        with allure.step("Act: Call get_expense_history with status_filter='denied'"):
            result = expense_service.get_expense_history(user_id, status_filter="denied")
            allure.attach(
                f"Filter: 'denied'\nReturned: {len(result)} expenses",
                "Filter Applied",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify only the denied expense is returned"):
            assert result == [expenses[0]], \
                f"Expected only the denied expense but got {len(result)} expenses"
            allure.attach(
                "✓ Returned exactly 1 denied expense (correct filtering)",
                "Verification Success",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.NORMAL)
    @allure.title("Get expense history with invalid filter returns all expenses")
    @allure.description("""
        Test to verify that get_expense_history returns all expenses 
        when an invalid/unrecognized status filter is provided.
        Validates graceful handling of invalid filter values.
    """)
    @allure.testcase("EXP_HIST_05")
    def test_get_expense_history_invalid_filter_returns_all(self, expense_service):
        with allure.step("Arrange: Set up expenses with valid statuses"):
            user_id = 5
            expenses = [
                make_expense_with_status("pending"),
                make_expense_with_status("approved"),
            ]
            allure.attach(
                f"User ID: {user_id}\n"
                f"Total Expenses: {len(expenses)}\n"
                f"Statuses: pending, approved",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status"):
            expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

        with allure.step("Act: Call get_expense_history with invalid filter 'cancelled'"):
            result = expense_service.get_expense_history(user_id, status_filter="cancelled")
            allure.attach(
                f"Filter: 'cancelled' (INVALID)\nReturned: {len(result)} expenses",
                "Invalid Filter Applied",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify all expenses are returned despite invalid filter"):
            assert result == expenses, \
                f"Expected all {len(expenses)} expenses but got {len(result)}"
            allure.attach(
                f"✓ All {len(expenses)} expenses returned (invalid filter handled gracefully)",
                "Verification Success",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.NORMAL)
    @allure.title("Get expense history returns empty list when user has no expenses")
    @allure.description("""
        Test to verify that get_expense_history correctly handles 
        the case when a user has no expenses.
        Validates:
        - Empty list is returned
        - No errors occur
    """)
    @allure.testcase("EXP_HIST_06")
    def test_get_expense_history_empty_expenses(self, expense_service):
        with allure.step("Arrange: Set up user with no expenses"):
            user_id = 6
            allure.attach(
                f"User ID: {user_id}\nExpenses: [] (empty)",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status to return empty list"):
            expense_service.get_user_expenses_with_status = Mock(return_value=[])
            allure.attach(
                "Mocked to return empty list",
                "Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Act: Call get_expense_history with status_filter='pending'"):
            result = expense_service.get_expense_history(user_id, status_filter="pending")
            allure.attach(
                f"Filter: 'pending'\nReturned: {result}",
                "Result",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify empty list is returned"):
            assert result == [], f"Expected empty list but got {result}"
            allure.attach(
                "✓ Empty list returned correctly",
                "Verification Success",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Get expense history calls get_user_expenses_with_status with correct user_id")
    @allure.description("""
        Test to verify that get_expense_history correctly delegates to 
        get_user_expenses_with_status with the proper user_id parameter.
        Validates internal method call behavior.
    """)
    @allure.testcase("EXP_HIST_07")
    def test_get_expense_history_calls_get_user_expenses_with_status(self, expense_service):
        with allure.step("Arrange: Set up test user"):
            user_id = 7
            allure.attach(
                f"User ID: {user_id}",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status to return empty list"):
            expense_service.get_user_expenses_with_status = Mock(return_value=[])

        with allure.step("Act: Call get_expense_history"):
            expense_service.get_expense_history(user_id)
            allure.attach(
                f"Called get_expense_history({user_id})",
                "Method Call",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify get_user_expenses_with_status was called with correct user_id"):
            expense_service.get_user_expenses_with_status.assert_called_once_with(user_id)
            allure.attach(
                f"✓ get_user_expenses_with_status called once with user_id={user_id}",
                "Method Call Verification",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Get expense history propagates exceptions from get_user_expenses_with_status")
    @allure.description("""
        Test to verify that get_expense_history correctly propagates exceptions 
        from get_user_expenses_with_status without swallowing them.
        Validates proper error handling and exception propagation.
    """)
    @allure.testcase("EXP_HIST_08")
    def test_get_expense_history_propagates_exception(self, expense_service):
        with allure.step("Arrange: Set up test user"):
            user_id = 8
            allure.attach(
                f"User ID: {user_id}",
                "Test Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_user_expenses_with_status to raise RuntimeError"):
            error_message = "Service failure"
            expense_service.get_user_expenses_with_status = Mock(
                side_effect=RuntimeError(error_message)
            )
            allure.attach(
                f"Mocked to raise RuntimeError: '{error_message}'",
                "Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Act & Assert: Verify RuntimeError is propagated"):
            allure.attach(
                f"Expected exception: RuntimeError with message '{error_message}'",
                "Expected Exception",
                allure.attachment_type.TEXT
            )

            with pytest.raises(RuntimeError, match="Service failure") as exc_info:
                expense_service.get_expense_history(user_id)

            allure.attach(
                f"✓ RuntimeError propagated correctly\nException message: {str(exc_info.value)}",
                "Exception Verification",
                allure.attachment_type.TEXT
            )