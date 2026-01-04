import pytest
import allure
from unittest.mock import Mock
from repository.expense_model import Expense
from repository.approval_model import Approval
from service.expense_service import ExpenseService


@allure.feature("Expense Management")
@allure.story("Expense Editing")
@pytest.mark.expense_editing
class TestUpdateExpense:
    """Test suite for updating expense records with validation and authorization checks."""

    @pytest.fixture
    def sample_expense(self):
        """Create a sample expense for testing."""
        return Expense(
            id=1,
            user_id=42,
            amount=100.0,
            description="Lunch",
            date="2024-01-01"
        )

    @pytest.fixture
    def sample_approval_pending(self):
        """Create a pending approval for testing."""
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
        """Create an approved approval for testing."""
        return Approval(
            id=11,
            expense_id=1,
            status="approved",
            reviewer=5,
            comment=None,
            review_date=None
        )

    @allure.title("Successfully update expense when status is pending")
    @allure.description("""
    Verifies that an expense can be successfully updated when it has a pending approval status.
    All fields (amount, description, date) should be updated and persisted to the repository.
    """)
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("happy-path", "update", "core-functionality")
    def test_update_expense_success(self, sample_expense, sample_approval_pending):
        """Test successful expense update with pending approval."""

        with allure.step("Arrange: Setup mocks and service"):
            mock_expense_repo = Mock()
            mock_approval_repo = Mock()
            service = ExpenseService(mock_expense_repo, mock_approval_repo)

            service.get_expense_with_status = Mock(
                return_value=(sample_expense, sample_approval_pending)
            )
            mock_expense_repo.update.return_value = sample_expense

            # Test data
            updated_amount = 150.0
            updated_description = "Updated description"
            updated_date = "2024-02-02"

            allure.dynamic.parameter("expense_id", 1)
            allure.dynamic.parameter("user_id", 42)

            allure.attach(
                f"Original:\n"
                f"  Amount: ${sample_expense.amount}\n"
                f"  Description: {sample_expense.description}\n"
                f"  Date: {sample_expense.date}\n"
                f"Updated:\n"
                f"  Amount: ${updated_amount}\n"
                f"  Description: {updated_description}\n"
                f"  Date: {updated_date}",
                name="Expense Changes",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Update the expense"):
            result = service.update_expense(
                expense_id=1,
                user_id=42,
                amount=updated_amount,
                description=updated_description,
                date=updated_date
            )

        with allure.step("Assert: Verify expense was updated correctly"):
            with allure.step("Check return value matches expected expense"):
                assert result == sample_expense

            with allure.step("Verify amount was updated"):
                assert sample_expense.amount == updated_amount

            with allure.step("Verify description was updated"):
                assert sample_expense.description == updated_description

            with allure.step("Verify date was updated"):
                assert sample_expense.date == updated_date

            with allure.step("Verify repository update was called"):
                mock_expense_repo.update.assert_called_once_with(sample_expense)

            allure.attach(
                f"Expense successfully updated:\n"
                f"  ID: {sample_expense.id}\n"
                f"  Amount: ${sample_expense.amount}\n"
                f"  Description: {sample_expense.description}\n"
                f"  Date: {sample_expense.date}",
                name="Updated Expense Details",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Returns None when expense is not found")
    @allure.description("Verifies that the service returns None when attempting to update a non-existent expense")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("edge-case", "not-found")
    def test_update_expense_not_found_returns_none(self):
        """Test that None is returned when expense doesn't exist."""

        with allure.step("Arrange: Setup service with non-existent expense"):
            service = ExpenseService(Mock(), Mock())
            service.get_expense_with_status = Mock(return_value=None)

            allure.dynamic.parameter("expense_id", 1)
            allure.dynamic.parameter("user_id", 42)

            allure.attach(
                "Expense with ID 1 does not exist or does not belong to user 42",
                name="Test Scenario",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Attempt to update non-existent expense"):
            result = service.update_expense(1, 42, 100, "desc", "2024-01-01")

        with allure.step("Assert: Verify None is returned"):
            assert result is None
            allure.attach(
                "Service correctly returned None for non-existent expense",
                name="Result",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Raises ValueError when expense has been reviewed")
    @allure.description("Verifies that updating an expense with non-pending status raises a ValueError")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("validation", "business-rule", "error-handling")
    def test_update_expense_not_pending_raises(self, sample_expense, sample_approval_approved):
        """Test that editing reviewed expense raises ValueError."""

        with allure.step("Arrange: Setup expense with approved status"):
            service = ExpenseService(Mock(), Mock())
            service.get_expense_with_status = Mock(
                return_value=(sample_expense, sample_approval_approved)
            )

            allure.dynamic.parameter("expense_id", 1)
            allure.dynamic.parameter("approval_status", sample_approval_approved.status)

            allure.attach(
                f"Expense Status: {sample_approval_approved.status}\n"
                f"Expected Error: Cannot edit expense that has been reviewed",
                name="Test Scenario",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act & Assert: Attempt update and verify ValueError is raised"):
            with pytest.raises(ValueError, match="Cannot edit expense that has been reviewed"):
                service.update_expense(1, 42, 100, "desc", "2024-01-01")

            allure.attach(
                "ValueError successfully raised for non-pending expense",
                name="Exception Handling Verified",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Raises ValueError for invalid amount: {bad_amount}")
    @allure.description("Verifies that updating an expense with zero or negative amount raises a ValueError")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("validation", "input-validation", "amount")
    @pytest.mark.parametrize("bad_amount", [
        pytest.param(0, id="zero_amount"),
        pytest.param(-1, id="negative_one"),
        pytest.param(-10, id="negative_ten")
    ])
    def test_update_expense_invalid_amount_raises(
            self,
            bad_amount,
            sample_expense,
            sample_approval_pending
    ):
        """Test that invalid amounts raise ValueError."""

        with allure.step(f"Arrange: Setup update with invalid amount: {bad_amount}"):
            service = ExpenseService(Mock(), Mock())
            service.get_expense_with_status = Mock(
                return_value=(sample_expense, sample_approval_pending)
            )

            allure.dynamic.parameter("bad_amount", bad_amount)
            allure.dynamic.parameter("expense_id", 1)

            allure.attach(
                f"Invalid Amount: {bad_amount}\n"
                f"Expected Error: Amount must be greater than 0",
                name="Validation Test Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step(f"Act & Assert: Verify ValueError for amount={bad_amount}"):
            with pytest.raises(ValueError, match="Amount must be greater than 0"):
                service.update_expense(1, 42, bad_amount, "desc", "2024-01-01")

            allure.attach(
                f"Validation correctly rejected amount: {bad_amount}",
                name="Validation Result",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.title("Raises ValueError for invalid description: '{bad_desc}'")
    @allure.description(
        "Verifies that updating an expense with empty or whitespace-only description raises a ValueError")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("validation", "input-validation", "description")
    @pytest.mark.parametrize("bad_desc", [
        pytest.param("", id="empty_string"),
        pytest.param("  ", id="spaces_only"),
        pytest.param("\t\n", id="whitespace_chars")
    ])
    def test_update_expense_invalid_description_raises(
            self,
            bad_desc,
            sample_expense,
            sample_approval_pending
    ):
        """Test that invalid descriptions raise ValueError."""

        with allure.step(f"Arrange: Setup update with invalid description"):
            service = ExpenseService(Mock(), Mock())
            service.get_expense_with_status = Mock(
                return_value=(sample_expense, sample_approval_pending)
            )

            allure.dynamic.parameter("bad_description", repr(bad_desc))
            allure.dynamic.parameter("expense_id", 1)

            allure.attach(
                f"Invalid Description: {repr(bad_desc)}\n"
                f"Description Length: {len(bad_desc)}\n"
                f"Is Whitespace Only: {bad_desc.strip() == ''}\n"
                f"Expected Error: Description is required",
                name="Validation Test Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step(f"Act & Assert: Verify ValueError for invalid description"):
            with pytest.raises(ValueError, match="Description is required"):
                service.update_expense(1, 42, 100, bad_desc, "2024-01-01")

            allure.attach(
                f"Validation correctly rejected description: {repr(bad_desc)}",
                name="Validation Result",
                attachment_type=allure.attachment_type.TEXT
            )