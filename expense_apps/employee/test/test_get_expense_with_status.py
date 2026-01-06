import pytest
import allure
from unittest.mock import Mock
from typing import Optional, Tuple
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService


@allure.feature("Expense Management")
@allure.story("Expense Status Retrieval")
@pytest.mark.expense_status
class TestGetExpenseWithStatus:

    @allure.title("Get expense with status")
    @allure.description("""
    Test the get_expense_with_status method which retrieves an expense along with its approval status.
    The method should:
    - Return a tuple of (Expense, Approval) when both exist and expense belongs to user
    - Return None when expense doesn't exist or doesn't belong to user
    - Return None when approval is missing
    """)
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.parametrize(
        "expense_return, approval_return, user_id, expected_result",
        [
            # expense exists, belongs to user, approval exists
            pytest.param(
                Expense(
                    id=1,
                    user_id=42,
                    amount=100.0,
                    description="Lunch",
                    date="2024-01-01"
                ),
                Approval(
                    id=10,
                    expense_id=1,
                    status="pending",
                    reviewer=5,
                    comment=None,
                    review_date=None
                ),
                42,
                True,
                id="valid_expense_with_approval"
            ),

            # expense exists, belongs to user, approval missing
            pytest.param(
                Expense(
                    id=1,
                    user_id=42,
                    amount=100.0,
                    description="Dinner",
                    date="2024-01-02"
                ),
                None,
                42,
                False,
                id="valid_expense_without_approval"
            ),

            # expense missing (or does not belong to user)
            pytest.param(
                None,
                None,
                42,
                False,
                id="missing_expense"
            ),
        ]
    )
    def test_get_expense_with_status(self, expense_return, approval_return, user_id, expected_result):
        # Arrange
        with allure.step("Setup mocks and service"):
            mock_expense_repo = Mock()
            mock_approval_repo = Mock()

            service = ExpenseService(mock_expense_repo, mock_approval_repo)
            service.get_expense_by_id = Mock(return_value=expense_return)
            mock_approval_repo.find_by_expense_id.return_value = approval_return

            # Add test data to Allure report
            allure.dynamic.parameter("user_id", user_id)
            allure.dynamic.parameter("expense_id", 1)
            if expense_return:
                allure.attach(
                    f"Amount: ${expense_return.amount}\nDescription: {expense_return.description}\nDate: {expense_return.date}",
                    name="Expense Details",
                    attachment_type=allure.attachment_type.TEXT
                )
            if approval_return:
                allure.attach(
                    f"Status: {approval_return.status}\nReviewer: {approval_return.reviewer}",
                    name="Approval Details",
                    attachment_type=allure.attachment_type.TEXT
                )

        # Act
        with allure.step(f"Call get_expense_with_status(expense_id=1, user_id={user_id})"):
            result = service.get_expense_with_status(expense_id=1, user_id=user_id)

            # Attach result to report
            allure.attach(
                f"Result type: {type(result)}\nResult value: {result}",
                name="Service Response",
                attachment_type=allure.attachment_type.TEXT
            )

        # Assert
        with allure.step("Verify the returned result matches expectations"):
            if expected_result:
                with allure.step("Validate tuple structure and contents"):
                    assert isinstance(result, tuple), "Result should be a tuple"
                    assert result[0] == expense_return, "First element should be the expense"
                    assert result[1] == approval_return, "Second element should be the approval"
            else:
                with allure.step("Verify result is None"):
                    assert result is None, "Result should be None when expense or approval is missing"

        with allure.step("Verify mock interactions"):
            with allure.step("Verify get_expense_by_id was called correctly"):
                service.get_expense_by_id.assert_called_once_with(1, user_id)

            if expense_return:
                with allure.step("Verify find_by_expense_id was called (expense exists)"):
                    mock_approval_repo.find_by_expense_id.assert_called_once_with(1)
            else:
                with allure.step("Verify find_by_expense_id was NOT called (expense missing)"):
                    mock_approval_repo.find_by_expense_id.assert_not_called()