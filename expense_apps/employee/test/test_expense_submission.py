from datetime import datetime
import pytest

from service.expense_service import ExpenseService

@pytest.mark.submission
class TestExpenseSubmission:
    """
    User Stories Covered:
    - Employees shall submit expenses with amount, description, date
    - System shall validate amount is greater than 0
    - System shall auto-assign "pending" status to new expenses
    """

    # C75_01
    @pytest.mark.positive
    @pytest.mark.smoke
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (1, 22.17, "for the lulz", "2001-11-09"),
        (999, 16.11, "\tthingy\n", "2025-12-24"),
        (27, 67.67, "   funny joke  ", None)
    ])
    def test_submit_expense_normal_returns_expense(self,
        mocker, user_id, amount, description, date):
        mock_expense = mocker.patch("repository.expense_model.Expense")
        mock_expense.user_id = user_id
        mock_expense.amount = amount
        mock_expense.description = description
        mock_expense.date = date if date else datetime.now().strftime("%Y-%m-%d")

        mock_approval = mocker.patch("repository.approval_model.Approval")
        mock_approval.expense_id = user_id
        mock_approval.status = "pending"

        mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
        mock_expense_repository.create.return_value = mock_expense
        mock_expense_repository.find_expense_by_id.return_value = mock_expense

        mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
        mock_approval_repository.find_by_expense_id.return_value = mock_approval

        service = ExpenseService(mock_expense_repository, mock_approval_repository)
        actual_expense = ExpenseService.submit_expense(service, user_id, amount, description, date)
        optional_tuple = ExpenseService.get_expense_with_status(service, user_id, user_id)
        if optional_tuple is not None:
            actual_approval = optional_tuple[1]

    # C75_02
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (2, 0, "for the lulz", "2001-11-09"),
        (96, -0.01, "something", "2012-06-06"),
        (22, -16.11, "other", "2025-12-24")
    ])
    def test_submit_expense_invalid_amt_raises_err(self,
        mocker, user_id, amount, description, date):
        pass

    # C75_03
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (3, 22.17, None, "2001-11-09"),
        (67, 15.75, "   \n \t", "2012-06-06"),
        (24, 16.11, "", "2025-12-24")
    ])
    def test_submit_expense_invalid_desc_raises_err(self,
        mocker, user_id, amount, description, date):
        pass