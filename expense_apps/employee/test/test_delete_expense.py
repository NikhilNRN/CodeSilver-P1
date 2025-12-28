import pytest
from unittest.mock import Mock
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService

@pytest.mark.expense_delete
class TestDeleteExpense:

    def setup_method(self):
        self.mock_expense_repo = Mock()
        self.mock_approval_repo = Mock()
        self.service = ExpenseService(self.mock_expense_repo, self.mock_approval_repo)

    def test_delete_expense_success(self):
        # Arrange
        expense = Expense(id=1, user_id=42, amount=100.0, description="Lunch", date="2024-01-01")
        approval = Approval(id=10, expense_id=1, status="pending", reviewer=5, comment=None, review_date=None)

        self.service.get_expense_with_status = Mock(return_value=(expense, approval))
        self.mock_expense_repo.delete.return_value = True

        # Act
        result = self.service.delete_expense(expense_id=1, user_id=42)

        # Assert
        assert result is True
        self.service.get_expense_with_status.assert_called_once_with(1, 42)
        self.mock_expense_repo.delete.assert_called_once_with(1)

    def test_delete_expense_not_found_returns_false(self):
        self.service.get_expense_with_status = Mock(return_value=None)

        result = self.service.delete_expense(expense_id=1, user_id=42)

        assert result is False
        self.service.get_expense_with_status.assert_called_once_with(1, 42)
        self.mock_expense_repo.delete.assert_not_called()

    def test_delete_expense_not_pending_raises(self):
        expense = Expense(id=1, user_id=42, amount=100.0, description="Dinner", date="2024-01-01")
        approval = Approval(id=11, expense_id=1, status="approved", reviewer=5, comment=None, review_date=None)

        self.service.get_expense_with_status = Mock(return_value=(expense, approval))

        with pytest.raises(ValueError, match="Cannot delete expense that has been reviewed"):
            self.service.delete_expense(expense_id=1, user_id=42)

        self.service.get_expense_with_status.assert_called_once_with(1, 42)
        self.mock_expense_repo.delete.assert_not_called()
