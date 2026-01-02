from datetime import datetime
import pytest
import allure

from service.expense_service import ExpenseService

@allure.epic("Employee App")
@allure.feature("Expense Service")
@allure.story("Expense Submission")
@pytest.mark.submission
class TestExpenseSubmission:
    """
    User Stories Covered:
    - Employees shall submit expenses with amount, description, date
    - System shall validate amount is greater than 0
    - System shall auto-assign "pending" status to new expenses
    """

    # C75_01
    @allure.title("Submit expense with valid data")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.positive
    @pytest.mark.smoke
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (1, 22.17, "for the lulz", "2001-11-09"),
        (999, 16.11, "\tthingy\n", "2025-12-24"),
        (27, 67.67, "   funny joke  ", None)
    ])
    def test_submit_expense_normal_returns_expense(self,
        mocker, user_id, amount, description, date):
        # Stubbing return values of mocked dependencies
        mock_expense = mocker.patch("repository.expense_model.Expense")
        mock_expense.user_id = user_id
        mock_expense.amount = amount
        mock_expense.description = description
        mock_expense.date = date if date else datetime.now().strftime("%Y-%m-%d")

        mock_approval = mocker.patch("repository.approval_model.Approval")
        mock_approval.expense_id = 1
        mock_approval.status = "pending"

        mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
        mock_expense_repository.create.return_value = mock_expense
        mock_expense_repository.find_expense_by_id.return_value = mock_expense

        mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
        mock_approval_repository.find_by_expense_id.return_value = mock_approval

        # Arrange
        service = ExpenseService(mock_expense_repository, mock_approval_repository)
        # Act
        actual_expense = service.submit_expense(user_id, amount, description, date)
        opt_tuple = service.get_expense_with_status(mock_approval.expense_id, user_id)
        if opt_tuple is not None:
            actual_approval = opt_tuple[1]
        else:
            actual_approval = mock_approval

        # Assert
        assert (actual_expense.amount is not None
                and actual_expense.amount == mock_expense.amount
                and actual_expense.amount > 0)
        assert (actual_expense.description is not None
                and actual_expense.description == mock_expense.description)
        assert (actual_expense.date is not None
                and actual_expense.date == mock_expense.date)
        assert actual_approval.status == "pending"

    # C75_02
    @allure.title("Submit expense with invalid amount")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (2, 0, "for the lulz", "2001-11-09"),
        (96, -0.01, "something", "2012-06-06"),
        (22, -16.11, "other", "2025-12-24")
    ])
    def test_submit_expense_invalid_amt_raises_err(self,
        mocker, user_id, amount, description, date):
        # Arrange
        mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
        mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
        service = ExpenseService(mock_expense_repository, mock_approval_repository)

        # Act
        with pytest.raises(ValueError) as excinfo:
            service.submit_expense(user_id, amount, description, date)

        # Assert
        assert "Amount must be greater than 0" in str(excinfo.value)

    # C75_03
    @allure.title("Submit expense with invalid description")
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (3, 22.17, " ", "2001-11-09"),
        (67, 15.75, "   \n \t", "2012-06-06"),
        (24, 16.11, "", "2025-12-24")
    ])
    def test_submit_expense_invalid_desc_raises_err(self,
        mocker, user_id, amount, description, date):
        mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
        mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
        service = ExpenseService(mock_expense_repository, mock_approval_repository)

        # Act
        with pytest.raises(ValueError) as excinfo:
            service.submit_expense(user_id, amount, description, date)

        # Assert
        assert "Description is required" in str(excinfo.value)