import pytest
import allure
from unittest.mock import Mock
from service import ExpenseService


@allure.feature("Expense Management")
@allure.story("User Expense Retrieval with Status")
class TestGetUserExpensesWithStatus:
    """Test suite for retrieving user expenses with their approval status."""

    # Fixtures used
    @pytest.fixture
    def mock_expense_repository(self):
        """Mock expense repository."""
        return Mock()

    @pytest.fixture
    def mock_approval_repository(self):
        """Mock approval repository."""
        return Mock()

    @pytest.fixture
    def expense_service(self, mock_expense_repository, mock_approval_repository):
        """Create ExpenseService instance with mocked repositories."""
        return ExpenseService(
            expense_repository=mock_expense_repository,
            approval_repository=mock_approval_repository,
        )

    @allure.title("Returns expenses with approval status (Happy Path)")
    @allure.description("Verifies that the service returns expenses with approval status for a valid user")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("happy-path", "core-functionality")
    def test_get_user_expenses_with_status_returns_expenses(
            self,
            expense_service,
            mock_approval_repository
    ):
        """Returns expenses with approval status for a valid user."""
        user_id = 1

        with allure.step("Arrange: Setup mock expense and approval objects"):
            expense = Mock(name="Expense")
            approval = Mock(name="Approval")
            expected_result = [(expense, approval)]

            mock_approval_repository.find_expenses_with_status_for_user.return_value = expected_result

            allure.dynamic.parameter("user_id", user_id)
            allure.attach(
                f"Expected result count: {len(expected_result)}",
                name="Test Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step(f"Act: Call get_user_expenses_with_status for user {user_id}"):
            result = expense_service.get_user_expenses_with_status(user_id)

        with allure.step("Assert: Verify result matches expected expenses"):
            assert result == expected_result
            allure.attach(
                f"Returned {len(result)} expense(s) with status",
                name="Result Summary",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Returns empty list when user has no expenses")
    @allure.description("Verifies that the service returns an empty list when the user has no expenses")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("edge-case", "empty-state")
    def test_get_user_expenses_with_status_returns_empty_list(
            self,
            expense_service,
            mock_approval_repository
    ):
        """Returns empty list when user has no expenses."""
        user_id = 2

        with allure.step("Arrange: Configure repository to return empty list"):
            mock_approval_repository.find_expenses_with_status_for_user.return_value = []
            allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Call get_user_expenses_with_status for user {user_id}"):
            result = expense_service.get_user_expenses_with_status(user_id)

        with allure.step("Assert: Verify result is empty list"):
            assert result == []
            assert len(result) == 0
            allure.attach(
                "User has no expenses",
                name="Result Status",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Calls repository with correct user ID")
    @allure.description("Ensures the approval repository method is called with the correct user_id parameter")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("integration", "mock-verification")
    def test_get_user_expenses_with_status_calls_repo_with_correct_user_id(
            self,
            expense_service,
            mock_approval_repository
    ):
        """Ensures repository method is called with correct user_id."""
        user_id = 3

        with allure.step("Arrange: Setup mock repository"):
            allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Call get_user_expenses_with_status for user {user_id}"):
            expense_service.get_user_expenses_with_status(user_id)

        with allure.step("Assert: Verify repository was called with correct user_id"):
            mock_approval_repository.find_expenses_with_status_for_user.assert_called_once_with(
                user_id
            )
            allure.attach(
                f"Repository called with user_id={user_id}",
                name="Mock Verification",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Propagates repository exceptions")
    @allure.description("Verifies that exceptions from the repository layer are properly propagated to the caller")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("error-handling", "exceptions")
    def test_get_user_expenses_with_status_propagates_exception(
            self,
            expense_service,
            mock_approval_repository
    ):
        """Exceptions from repository should propagate."""
        user_id = 4
        error_message = "Database error"

        with allure.step("Arrange: Configure repository to raise RuntimeError"):
            mock_approval_repository.find_expenses_with_status_for_user.side_effect = RuntimeError(
                error_message
            )
            allure.dynamic.parameter("user_id", user_id)
            allure.attach(
                f"Expected exception: RuntimeError('{error_message}')",
                name="Exception Details",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step(f"Act & Assert: Call service and verify exception is raised"):
            with pytest.raises(RuntimeError, match=error_message):
                expense_service.get_user_expenses_with_status(user_id)

            allure.attach(
                "Exception successfully propagated from repository to service layer",
                name="Exception Propagation Verified",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Returns data in correct structure (list of tuples)")
    @allure.description("Validates that the return type is a list and each item is a tuple with 2 elements")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("validation", "data-structure")
    def test_get_user_expenses_with_status_returns_list_of_tuples(
            self,
            expense_service,
            mock_approval_repository
    ):
        """Validates return structure."""
        user_id = 5

        with allure.step("Arrange: Setup mock data with tuple structure"):
            expense = Mock()
            approval = Mock()
            mock_approval_repository.find_expenses_with_status_for_user.return_value = [
                (expense, approval)
            ]
            allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Call get_user_expenses_with_status for user {user_id}"):
            result = expense_service.get_user_expenses_with_status(user_id=user_id)

        with allure.step("Assert: Verify result is a list of tuples"):
            with allure.step("Check result is a list"):
                assert isinstance(result, list)

            with allure.step("Check all items are tuples with 2 elements"):
                assert all(isinstance(item, tuple) and len(item) == 2 for item in result)

            allure.attach(
                f"Result type: {type(result).__name__}\n"
                f"Item count: {len(result)}\n"
                f"Each item structure: tuple(Expense, Approval)",
                name="Structure Validation",
                attachment_type=allure.attachment_type.TEXT
            )