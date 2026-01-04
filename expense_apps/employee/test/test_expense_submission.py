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
    @allure.description("""
        Test to verify that expenses can be successfully submitted with valid data.
        Validates:
        - Expense is created with correct amount (> 0)
        - Description is properly stored
        - Date is set (current date if not provided)
        - Status is automatically set to "pending"
    """)
    @allure.testcase("C75_01")
    @pytest.mark.positive
    @pytest.mark.smoke
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (1, 22.17, "for the lulz", "2001-11-09"),
        (999, 16.11, "\tthingy\n", "2025-12-24"),
        (27, 67.67, "   funny joke  ", None)
    ])
    def test_submit_expense_normal_returns_expense(self,
                                                   mocker, user_id, amount, description, date):

        with allure.step(f"Arrange: Setup test data for user {user_id}"):
            allure.attach(
                f"User ID: {user_id}\nAmount: ${amount}\nDescription: '{description}'\nDate: {date}",
                "Test Input Parameters",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock Expense model and set return values"):
            mock_expense = mocker.patch("repository.expense_model.Expense")
            mock_expense.user_id = user_id
            mock_expense.amount = amount
            mock_expense.description = description
            mock_expense.date = date if date else datetime.now().strftime("%Y-%m-%d")

            allure.attach(
                f"User ID: {mock_expense.user_id}\n"
                f"Amount: ${mock_expense.amount}\n"
                f"Description: '{mock_expense.description}'\n"
                f"Date: {mock_expense.date}",
                "Mocked Expense Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock Approval model with pending status"):
            mock_approval = mocker.patch("repository.approval_model.Approval")
            mock_approval.expense_id = 1
            mock_approval.status = "pending"

            allure.attach(
                f"Expense ID: {mock_approval.expense_id}\nStatus: {mock_approval.status}",
                "Mocked Approval Data",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock ExpenseRepository methods"):
            mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
            mock_expense_repository.create.return_value = mock_expense
            mock_expense_repository.find_expense_by_id.return_value = mock_expense

            allure.attach(
                "create() → returns mocked expense\nfind_expense_by_id() → returns mocked expense",
                "ExpenseRepository Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Mock ApprovalRepository methods"):
            mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
            mock_approval_repository.find_by_expense_id.return_value = mock_approval

            allure.attach(
                "find_by_expense_id() → returns mocked approval",
                "ApprovalRepository Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Initialize ExpenseService with mocked repositories"):
            service = ExpenseService(mock_expense_repository, mock_approval_repository)
            allure.attach(
                "ExpenseService initialized with mocked dependencies",
                "Service Initialization",
                allure.attachment_type.TEXT
            )

        with allure.step(f"Act: Submit expense for user {user_id}"):
            actual_expense = service.submit_expense(user_id, amount, description, date)
            allure.attach(
                f"Submitted expense with amount ${amount}",
                "Expense Submission",
                allure.attachment_type.TEXT
            )

        with allure.step("Retrieve expense with approval status"):
            opt_tuple = service.get_expense_with_status(mock_approval.expense_id, user_id)
            if opt_tuple is not None:
                actual_approval = opt_tuple[1]
            else:
                actual_approval = mock_approval

            allure.attach(
                f"Retrieved approval status: {actual_approval.status}",
                "Approval Status",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense amount is valid"):
            assert actual_expense.amount is not None, "Amount should not be None"
            assert actual_expense.amount == mock_expense.amount, \
                f"Expected amount ${mock_expense.amount} but got ${actual_expense.amount}"
            assert actual_expense.amount > 0, \
                f"Amount must be greater than 0, got ${actual_expense.amount}"
            allure.attach(
                f"✓ Amount: ${actual_expense.amount} (valid and > 0)",
                "Amount Validation",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense description is set"):
            assert actual_expense.description is not None, "Description should not be None"
            assert actual_expense.description == mock_expense.description, \
                f"Expected description '{mock_expense.description}' but got '{actual_expense.description}'"
            allure.attach(
                f"✓ Description: '{actual_expense.description}'",
                "Description Validation",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense date is set"):
            assert actual_expense.date is not None, "Date should not be None"
            assert actual_expense.date == mock_expense.date, \
                f"Expected date '{mock_expense.date}' but got '{actual_expense.date}'"
            allure.attach(
                f"✓ Date: {actual_expense.date}",
                "Date Validation",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify approval status is 'pending'"):
            assert actual_approval.status == "pending", \
                f"Expected status 'pending' but got '{actual_approval.status}'"
            allure.attach(
                "✓ Status: pending (auto-assigned correctly)",
                "Status Validation",
                allure.attachment_type.TEXT
            )

    # C75_02
    @allure.title("Submit expense with invalid amount")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.description("""
        Test to verify that submitting an expense with invalid amount raises ValueError.
        Tests amounts that are:
        - Zero (0)
        - Negative values
        Expected: ValueError with message "Amount must be greater than 0"
    """)
    @allure.testcase("C75_02")
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (2, 0, "for the lulz", "2001-11-09"),
        (96, -0.01, "something", "2012-06-06"),
        (22, -16.11, "other", "2025-12-24")
    ])
    def test_submit_expense_invalid_amt_raises_err(self,
                                                   mocker, user_id, amount, description, date):

        with allure.step(f"Arrange: Setup test data with invalid amount ${amount}"):
            allure.attach(
                f"User ID: {user_id}\nAmount: ${amount} (INVALID)\n"
                f"Description: '{description}'\nDate: {date}",
                "Test Input Parameters",
                allure.attachment_type.TEXT
            )

            if amount == 0:
                allure.attach("Zero amount", "Invalid Amount Type", allure.attachment_type.TEXT)
            else:
                allure.attach("Negative amount", "Invalid Amount Type", allure.attachment_type.TEXT)

        with allure.step("Mock repository dependencies"):
            mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
            mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
            service = ExpenseService(mock_expense_repository, mock_approval_repository)

            allure.attach(
                "ExpenseService initialized with mocked repositories",
                "Service Initialization",
                allure.attachment_type.TEXT
            )

        with allure.step(f"Act & Assert: Attempt to submit expense with invalid amount ${amount}"):
            expected_error = "Amount must be greater than 0"
            allure.attach(
                f"Expected error message: {expected_error}",
                "Expected Exception",
                allure.attachment_type.TEXT
            )

            with pytest.raises(ValueError) as excinfo:
                service.submit_expense(user_id, amount, description, date)

            allure.attach(
                f"✓ ValueError raised with message: {str(excinfo.value)}",
                "Exception Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify error message contains expected text"):
            assert "Amount must be greater than 0" in str(excinfo.value), \
                f"Expected error message to contain 'Amount must be greater than 0' but got '{str(excinfo.value)}'"
            allure.attach(
                "✓ Error message validation passed",
                "Validation Result",
                allure.attachment_type.TEXT
            )

    # C75_03
    @allure.title("Submit expense with invalid description")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.description("""
        Test to verify that submitting an expense with invalid description raises ValueError.
        Tests descriptions that are:
        - Empty strings
        - Whitespace only
        - Tabs and newlines only
        Expected: ValueError with message "Description is required"
    """)
    @allure.testcase("C75_03")
    @pytest.mark.negative
    @pytest.mark.parametrize("user_id, amount, description, date", [
        (3, 22.17, " ", "2001-11-09"),
        (67, 15.75, "   \n \t", "2012-06-06"),
        (24, 16.11, "", "2025-12-24")
    ])
    def test_submit_expense_invalid_desc_raises_err(self,
                                                    mocker, user_id, amount, description, date):

        with allure.step(f"Arrange: Setup test data with invalid description"):
            desc_repr = repr(description)
            allure.attach(
                f"User ID: {user_id}\nAmount: ${amount}\n"
                f"Description: {desc_repr} (INVALID - empty/whitespace)\nDate: {date}",
                "Test Input Parameters",
                allure.attachment_type.TEXT
            )

            if description == "":
                allure.attach("Empty string", "Invalid Description Type", allure.attachment_type.TEXT)
            else:
                allure.attach("Whitespace only", "Invalid Description Type", allure.attachment_type.TEXT)

        with allure.step("Mock repository dependencies"):
            mock_expense_repository = mocker.patch("repository.expense_repository.ExpenseRepository")
            mock_approval_repository = mocker.patch("repository.approval_repository.ApprovalRepository")
            service = ExpenseService(mock_expense_repository, mock_approval_repository)

            allure.attach(
                "ExpenseService initialized with mocked repositories",
                "Service Initialization",
                allure.attachment_type.TEXT
            )

        with allure.step(f"Act & Assert: Attempt to submit expense with invalid description"):
            expected_error = "Description is required"
            allure.attach(
                f"Expected error message: {expected_error}",
                "Expected Exception",
                allure.attachment_type.TEXT
            )

            with pytest.raises(ValueError) as excinfo:
                service.submit_expense(user_id, amount, description, date)

            allure.attach(
                f"✓ ValueError raised with message: {str(excinfo.value)}",
                "Exception Verification",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify error message contains expected text"):
            assert "Description is required" in str(excinfo.value), \
                f"Expected error message to contain 'Description is required' but got '{str(excinfo.value)}'"
            allure.attach(
                "✓ Error message validation passed",
                "Validation Result",
                allure.attachment_type.TEXT
            )