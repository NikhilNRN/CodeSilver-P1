import pytest
from unittest.mock import Mock
from service import ExpenseService

# Fixtures used
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

#1. Returns expenses with approval status (Happy Path)
def test_get_user_expenses_with_status_returns_expenses(
    expense_service,
    mock_approval_repository
):
    """Returns expenses with approval status for a valid user."""
    user_id = 1

    expense = Mock(name="Expense")
    approval = Mock(name="Approval")

    expected_result = [(expense, approval)]
    mock_approval_repository.find_expenses_with_status_for_user.return_value = expected_result

    result = expense_service.get_user_expenses_with_status(user_id)

    assert result == expected_result

#2. Returns an empty list when the user has no expenses
def test_get_user_expenses_with_status_returns_empty_list(
    expense_service,
    mock_approval_repository
):
    """Returns empty list when user has no expenses."""
    user_id = 2

    mock_approval_repository.find_expenses_with_status_for_user.return_value = []

    result = expense_service.get_user_expenses_with_status(user_id)

    assert result == []

#3. Calls the approval repository with the correct user ID
def test_get_user_expenses_with_status_calls_repo_with_correct_user_id(
    expense_service,
    mock_approval_repository
):
    """Ensures repository method is called with correct user_id."""
    user_id = 3

    expense_service.get_user_expenses_with_status(user_id)

    mock_approval_repository.find_expenses_with_status_for_user.assert_called_once_with(
        user_id
    )

#4. Propagates repository exceptions
def test_get_user_expenses_with_status_propagates_exception(
    expense_service,
    mock_approval_repository
):
    """Exceptions from repository should propagate."""
    user_id = 4

    mock_approval_repository.find_expenses_with_status_for_user.side_effect = RuntimeError(
        "Database error"
    )

    with pytest.raises(RuntimeError, match="Database error"):
        expense_service.get_user_expenses_with_status(user_id)

#5. Returns data in the correct structure
def test_get_user_expenses_with_status_returns_list_of_tuples(
    expense_service,
    mock_approval_repository
):
    """Validates return structure."""
    expense = Mock()
    approval = Mock()

    mock_approval_repository.find_expenses_with_status_for_user.return_value = [
        (expense, approval)
    ]

    result = expense_service.get_user_expenses_with_status(user_id=5)

    assert isinstance(result, list)
    assert all(isinstance(item, tuple) and len(item) == 2 for item in result)
