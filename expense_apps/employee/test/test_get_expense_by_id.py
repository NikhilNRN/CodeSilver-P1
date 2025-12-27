import pytest
from unittest.mock import Mock

from service.expense_service import ExpenseService
from repository.expense_model import Expense
@pytest.mark.parametrize(
    "repo_return, user_id, expected_result",
    [
        # Expense exists and belongs to user
        (
            Expense(
                id=1,
                user_id=42,
                amount=100.0,
                description="Lunch",
                date="2024-01-01"
            ),
            42,
            "expense"
        ),

        # Expense exists but belongs to different user
        (
            Expense(
                id=1,
                user_id=99,
                amount=100.0,
                description="Dinner",
                date="2024-01-01"
            ),
            42,
            None
        ),

        # Expense does not exist
        (
            None,
            42,
            None
        ),
    ]
)
def test_get_expense_by_id(repo_return, user_id, expected_result):
    expense_repository = Mock()
    approval_repository = Mock()

    expense_repository.find_by_id.return_value = repo_return

    service = ExpenseService(expense_repository, approval_repository)

    result = service.get_expense_by_id(expense_id=1, user_id=user_id)

    if expected_result == "expense":
        assert result == repo_return
    else:
        assert result is None

    expense_repository.find_by_id.assert_called_once_with(1)
