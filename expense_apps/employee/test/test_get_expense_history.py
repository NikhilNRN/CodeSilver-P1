import pytest
from unittest.mock import Mock
from service import ExpenseService

#Fixtures used:
@pytest.fixture
def mock_expense_repository():
    return Mock()


@pytest.fixture
def mock_approval_repository():
    return Mock()


@pytest.fixture
def expense_service(mock_expense_repository, mock_approval_repository):
    return ExpenseService(
        expense_repository=mock_expense_repository,
        approval_repository=mock_approval_repository,
    )


# Helper function:
def make_expense_with_status(status: str):
    expense = Mock(name="Expense")
    approval = Mock(name="Approval")
    approval.status = status
    return expense, approval

#1. Returns all expenses when no status filter is provided
def test_get_expense_history_returns_all_when_no_filter(expense_service):
    user_id = 1

    expenses = [
        make_expense_with_status("pending"),
        make_expense_with_status("approved"),
    ]

    expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

    result = expense_service.get_expense_history(user_id)

    assert result == expenses

#2. Returns only pending expenses when filter is "pending"
def test_get_expense_history_filters_pending(expense_service):
    user_id = 2

    expenses = [
        make_expense_with_status("pending"),
        make_expense_with_status("approved"),
        make_expense_with_status("pending"),
    ]

    expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

    result = expense_service.get_expense_history(user_id, status_filter="pending")

    assert len(result) == 2
    assert all(approval.status == "pending" for _, approval in result)

#3. Returns only approved expenses when filter is "approved"
def test_get_expense_history_filters_approved(expense_service):
    user_id = 3

    expenses = [
        make_expense_with_status("approved"),
        make_expense_with_status("denied"),
    ]

    expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

    result = expense_service.get_expense_history(user_id, status_filter="approved")

    assert result == [expenses[0]]

#4. Returns only denied expenses when filter is "denied"
def test_get_expense_history_filters_denied(expense_service):
    user_id = 4

    expenses = [
        make_expense_with_status("denied"),
        make_expense_with_status("approved"),
    ]

    expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

    result = expense_service.get_expense_history(user_id, status_filter="denied")

    assert result == [expenses[0]]

#5. Returns all expenses when filter value is invalid
def test_get_expense_history_invalid_filter_returns_all(expense_service):
    user_id = 5

    expenses = [
        make_expense_with_status("pending"),
        make_expense_with_status("approved"),
    ]

    expense_service.get_user_expenses_with_status = Mock(return_value=expenses)

    result = expense_service.get_expense_history(user_id, status_filter="cancelled")

    assert result == expenses

#6. Returns empty list when user has no expenses
def test_get_expense_history_empty_expenses(expense_service):
    user_id = 6

    expense_service.get_user_expenses_with_status = Mock(return_value=[])

    result = expense_service.get_expense_history(user_id, status_filter="pending")

    assert result == []

#7. Calls get_user_expenses_with_status with correct user_id
def test_get_expense_history_calls_get_user_expenses_with_status(expense_service):
    user_id = 7

    expense_service.get_user_expenses_with_status = Mock(return_value=[])

    expense_service.get_expense_history(user_id)

    expense_service.get_user_expenses_with_status.assert_called_once_with(user_id)

#8. Propagates exceptions from get_user_expenses_with_status (ensures that failures are not swallowed)
def test_get_expense_history_propagates_exception(expense_service):
    user_id = 8

    expense_service.get_user_expenses_with_status = Mock(
        side_effect=RuntimeError("Service failure")
    )

    with pytest.raises(RuntimeError, match="Service failure"):
        expense_service.get_expense_history(user_id)
