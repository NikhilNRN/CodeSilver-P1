import allure
import pytest
from unittest.mock import Mock
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService


@allure.epic("Employee App")
@allure.feature("Expense Management")
@allure.story("Delete Expense")
@pytest.mark.expense_deletion
class TestDeleteExpense:
    """Test suite for expense deletion functionality"""

    def setup_method(self):
        """Set up test fixtures before each test method"""
        with allure.step("Initialize mock repositories and expense service"):
            self.mock_expense_repo = Mock()
            self.mock_approval_repo = Mock()
            self.service = ExpenseService(self.mock_expense_repo, self.mock_approval_repo)

            allure.attach(
                "ExpenseService initialized with mocked repositories",
                "Setup Information",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Successfully delete a pending expense")
    @allure.description("""
        Test to verify that a pending expense can be successfully deleted.
        Verifies:
        - Expense exists and belongs to the user
        - Approval status is pending
        - Expense is deleted from repository
        - Returns True on success
    """)
    @allure.testcase("EXP_DEL_01")
    def test_delete_expense_success(self):
        with allure.step("Arrange: Create test expense and approval with pending status"):
            expense = Expense(
                id=1,
                user_id=42,
                amount=100.0,
                description="Lunch",
                date="2024-01-01"
            )
            approval = Approval(
                id=10,
                expense_id=1,
                status="pending",
                reviewer=5,
                comment=None,
                review_date=None
            )

            allure.attach(
                f"Expense ID: {expense.id}\nUser ID: {expense.user_id}\n"
                f"Amount: ${expense.amount}\nDescription: {expense.description}\n"
                f"Date: {expense.date}",
                "Test Expense Data",
                allure.attachment_type.TEXT
            )
            allure.attach(
                f"Approval ID: {approval.id}\nStatus: {approval.status}\n"
                f"Reviewer: {approval.reviewer}",
                "Test Approval Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_expense_with_status to return expense and approval"):
            self.service.get_expense_with_status = Mock(return_value=(expense, approval))
            allure.attach(
                "Mocked to return (expense, approval)",
                "get_expense_with_status Mock",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock expense repository delete to return True"):
            self.mock_expense_repo.delete.return_value = True
            allure.attach(
                "Mocked to return True",
                "expense_repo.delete Mock",
                allure.attachment_type.TEXT
            )

        with allure.step("Act: Call delete_expense service method"):
            result = self.service.delete_expense(expense_id=1, user_id=42)
            allure.attach(
                f"Result: {result}",
                "Delete Operation Result",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify result is True"):
            assert result is True, f"Expected True but got {result}"

        with allure.step("Assert: Verify get_expense_with_status was called with correct parameters"):
            self.service.get_expense_with_status.assert_called_once_with(1, 42)
            allure.attach(
                "✓ get_expense_with_status called once with (1, 42)",
                "Method Call Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense repository delete was called with expense ID"):
            self.mock_expense_repo.delete.assert_called_once_with(1)
            allure.attach(
                "✓ expense_repo.delete called once with (1)",
                "Repository Call Verification",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.NORMAL)
    @allure.title("Delete non-existent expense returns False")
    @allure.description("""
        Test to verify that attempting to delete a non-existent expense returns False.
        Verifies:
        - Service returns False when expense not found
        - Repository delete is not called
    """)
    @allure.testcase("EXP_DEL_02")
    def test_delete_expense_not_found_returns_false(self):
        with allure.step("Arrange: Mock get_expense_with_status to return None (not found)"):
            self.service.get_expense_with_status = Mock(return_value=None)
            allure.attach(
                "Mocked to return None (expense not found)",
                "get_expense_with_status Mock",
                allure.attachment_type.TEXT
            )

        with allure.step("Act: Attempt to delete non-existent expense"):
            result = self.service.delete_expense(expense_id=1, user_id=42)
            allure.attach(
                f"Result: {result}",
                "Delete Operation Result",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify result is False"):
            assert result is False, f"Expected False but got {result}"

        with allure.step("Assert: Verify get_expense_with_status was called"):
            self.service.get_expense_with_status.assert_called_once_with(1, 42)
            allure.attach(
                "✓ get_expense_with_status called once with (1, 42)",
                "Method Call Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense repository delete was NOT called"):
            self.mock_expense_repo.delete.assert_not_called()
            allure.attach(
                "✓ expense_repo.delete was not called (correct behavior)",
                "Repository Call Verification",
                allure.attachment_type.TEXT
            )

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Delete reviewed expense raises ValueError")
    @allure.description("""
        Test to verify that attempting to delete a reviewed (non-pending) expense raises ValueError.
        Verifies:
        - ValueError is raised with appropriate message
        - Repository delete is not called
        - Expense with 'approved' status cannot be deleted
    """)
    @allure.testcase("EXP_DEL_03")
    def test_delete_expense_not_pending_raises(self):
        with allure.step("Arrange: Create test expense and approval with approved status"):
            expense = Expense(
                id=1,
                user_id=42,
                amount=100.0,
                description="Dinner",
                date="2024-01-01"
            )
            approval = Approval(
                id=11,
                expense_id=1,
                status="approved",
                reviewer=5,
                comment=None,
                review_date=None
            )

            allure.attach(
                f"Expense ID: {expense.id}\nUser ID: {expense.user_id}\n"
                f"Amount: ${expense.amount}\nDescription: {expense.description}\n"
                f"Date: {expense.date}",
                "Test Expense Data",
                allure.attachment_type.TEXT
            )
            allure.attach(
                f"Approval ID: {approval.id}\nStatus: {approval.status} (NOT PENDING)\n"
                f"Reviewer: {approval.reviewer}",
                "Test Approval Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock get_expense_with_status to return expense with approved status"):
            self.service.get_expense_with_status = Mock(return_value=(expense, approval))
            allure.attach(
                "Mocked to return (expense, approval) with status='approved'",
                "get_expense_with_status Mock",
                allure.attachment_type.TEXT
            )

        with allure.step("Act & Assert: Verify ValueError is raised with correct message"):
            expected_error = "Cannot delete expense that has been reviewed"
            allure.attach(
                f"Expected error message: {expected_error}",
                "Expected Exception",
                allure.attachment_type.TEXT
            )

            with pytest.raises(ValueError, match=expected_error) as exc_info:
                self.service.delete_expense(expense_id=1, user_id=42)

            allure.attach(
                f"✓ ValueError raised with message: {str(exc_info.value)}",
                "Exception Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify get_expense_with_status was called"):
            self.service.get_expense_with_status.assert_called_once_with(1, 42)
            allure.attach(
                "✓ get_expense_with_status called once with (1, 42)",
                "Method Call Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense repository delete was NOT called"):
            self.mock_expense_repo.delete.assert_not_called()
            allure.attach(
                "✓ expense_repo.delete was not called (correct behavior)",
                "Repository Call Verification",
                allure.attachment_type.TEXT
            )