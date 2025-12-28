import pytest
from unittest.mock import Mock
from repository.expense_model import Expense
from service.expense_service import ExpenseService

@pytest.mark.expense_retrieval
class TestGetExpenseById:

    @pytest.mark.parametrize(
        "repo_return, user_id, expected_found",
        [
            # expense exists and belongs to user
            (
                Expense(
                    id=1,
                    user_id=42,
                    amount=100.0,
                    description="Lunch",
                    date="2024-01-01"
                ),
                42,
                True,
            ),

            # expense exists but belongs to different user
            (
                Expense(
                    id=1,
                    user_id=99,
                    amount=100.0,
                    description="Dinner",
                    date="2024-01-01"
                ),
                42,
                False,
            ),

            # expense does not exist
            (
                None,
                42,
                False,
            ),
        ]
    )
    def test_get_expense_by_id_returns_correctly(self, repo_return, user_id, expected_found):
        # Create Mock objects for repositories
        mock_expense_repository = Mock()
        mock_approval_repository = Mock()

        # Setup find_by_id to return repo_return
        mock_expense_repository.find_by_id.return_value = repo_return

        # Instantiate service with mocks
        service = ExpenseService(mock_expense_repository, mock_approval_repository)

        # Call the method under test
        result = service.get_expense_by_id(expense_id=1, user_id=user_id)

        # Assert expected results
        if expected_found:
            assert result == repo_return
        else:
            assert result is None

        # Verify repo method was called correctly
        mock_expense_repository.find_by_id.assert_called_once_with(1)
