import pytest
from unittest.mock import Mock
from typing import Optional, Tuple
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService

@pytest.mark.expense_status
class TestGetExpenseWithStatus:

    @pytest.mark.parametrize(
        "expense_return, approval_return, user_id, expected_result",
        [
            # expense exists, belongs to user, approval exists
            (
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
                True  # Expect a tuple returned
            ),

            # expense exists, belongs to user, approval missing
            (
                Expense(
                    id=1,
                    user_id=42,
                    amount=100.0,
                    description="Dinner",
                    date="2024-01-02"
                ),
                None,
                42,
                False  # Expect None because no approval
            ),

            # expense missing (or does not belong to user)
            (
                None,
                None,
                42,
                False
            ),
        ]
    )
    def test_get_expense_with_status(self, expense_return, approval_return, user_id, expected_result):
        # Arrange
        mock_expense_repo = Mock()
        mock_approval_repo = Mock()

        service = ExpenseService(mock_expense_repo, mock_approval_repo)
        service.get_expense_by_id = Mock(return_value=expense_return)

        mock_approval_repo.find_by_expense_id.return_value = approval_return

        # Act
        result = service.get_expense_with_status(expense_id=1, user_id=user_id)

        # Assert
        if expected_result:
            assert isinstance(result, tuple)
            assert result[0] == expense_return
            assert result[1] == approval_return
        else:
            assert result is None

        service.get_expense_by_id.assert_called_once_with(1, user_id)
        if expense_return:
            mock_approval_repo.find_by_expense_id.assert_called_once_with(1)
        else:
            mock_approval_repo.find_by_expense_id.assert_not_called()
