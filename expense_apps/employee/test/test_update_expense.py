import pytest
from unittest.mock import Mock
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService

@pytest.mark.expense_editing
class TestUpdateExpense:
    @pytest.fixture
    def sample_expense(self):
        return Expense(
            id=1,
            user_id=42,
            amount=100.0,
            description="Lunch",
            date="2024-01-01"
        )

    @pytest.fixture
    def sample_approval_pending(self):
        return Approval(
            id=10,
            expense_id=1,
            status="pending",
            reviewer=5,
            comment=None,
            review_date=None
        )

    @pytest.fixture
    def sample_approval_approved(self):
        return Approval(
            id=11,
            expense_id=1,
            status="approved",
            reviewer=5,
            comment=None,
            review_date=None
        )

    def test_update_expense_success(self, sample_expense, sample_approval_pending):
        mock_expense_repo = Mock()
        mock_approval_repo = Mock()

        service = ExpenseService(mock_expense_repo, mock_approval_repo)

        service.get_expense_with_status = Mock(return_value=(sample_expense, sample_approval_pending))
        mock_expense_repo.update.return_value = sample_expense

        updated_amount = 150.0
        updated_description = "Updated description"
        updated_date = "2024-02-02"

        result = service.update_expense(
            expense_id=1,
            user_id=42,
            amount=updated_amount,
            description=updated_description,
            date=updated_date
        )

        assert result == sample_expense
        assert sample_expense.amount == updated_amount
        assert sample_expense.description == updated_description
        assert sample_expense.date == updated_date
        mock_expense_repo.update.assert_called_once_with(sample_expense)

    def test_update_expense_not_found_returns_none(self):
        service = ExpenseService(Mock(), Mock())
        service.get_expense_with_status = Mock(return_value=None)

        result = service.update_expense(1, 42, 100, "desc", "2024-01-01")
        assert result is None

    def test_update_expense_not_pending_raises(self, sample_expense, sample_approval_approved):
        service = ExpenseService(Mock(), Mock())
        service.get_expense_with_status = Mock(return_value=(sample_expense, sample_approval_approved))

        with pytest.raises(ValueError, match="Cannot edit expense that has been reviewed"):
            service.update_expense(1, 42, 100, "desc", "2024-01-01")

    @pytest.mark.parametrize("bad_amount", [0, -1, -10])
    def test_update_expense_invalid_amount_raises(self, bad_amount, sample_expense, sample_approval_pending):
        service = ExpenseService(Mock(), Mock())
        service.get_expense_with_status = Mock(return_value=(sample_expense, sample_approval_pending))

        with pytest.raises(ValueError, match="Amount must be greater than 0"):
            service.update_expense(1, 42, bad_amount, "desc", "2024-01-01")

    @pytest.mark.parametrize("bad_desc", ["", "  ", "\t\n"])
    def test_update_expense_invalid_description_raises(self, bad_desc, sample_expense, sample_approval_pending):
        service = ExpenseService(Mock(), Mock())
        service.get_expense_with_status = Mock(return_value=(sample_expense, sample_approval_pending))

        with pytest.raises(ValueError, match="Description is required"):
            service.update_expense(1, 42, 100, bad_desc, "2024-01-01")
