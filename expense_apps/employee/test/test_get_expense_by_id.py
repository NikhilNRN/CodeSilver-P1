import allure
import pytest
from unittest.mock import Mock
from repository.expense_model import Expense
from service.expense_service import ExpenseService


@allure.epic("Employee App")
@allure.feature("Expense Service")
@allure.story("Expense Retrieval by ID")
@pytest.mark.expense_retrieval
class TestGetExpenseById:
    """Test suite for retrieving expenses by ID with user authorization checks"""

    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Get expense by ID with authorization check")
    @allure.description("""
        Test to verify that get_expense_by_id correctly retrieves expenses
        with proper authorization checks.

        Test scenarios:
        1. Expense exists and belongs to the requesting user → returns expense
        2. Expense exists but belongs to different user → returns None (authorization failure)
        3. Expense does not exist → returns None (not found)
    """)
    @allure.testcase("EXP_GET_01")
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
        ],
        ids=[
            "expense_exists_belongs_to_user",
            "expense_exists_different_user",
            "expense_not_found"
        ]
    )
    def test_get_expense_by_id_returns_correctly(self, repo_return, user_id, expected_found):

        with allure.step("Arrange: Determine test scenario"):
            if repo_return is None:
                scenario = "Expense does not exist"
                allure.attach(
                    f"Scenario: {scenario}\nExpected: None",
                    "Test Scenario",
                    allure.attachment_type.TEXT
                )
            elif repo_return.user_id == user_id:
                scenario = "Expense exists and belongs to user"
                allure.attach(
                    f"Scenario: {scenario}\n"
                    f"Expense ID: {repo_return.id}\n"
                    f"Owner ID: {repo_return.user_id}\n"
                    f"Requesting User ID: {user_id}\n"
                    f"Expected: Return expense",
                    "Test Scenario",
                    allure.attachment_type.TEXT
                )
            else:
                scenario = "Expense exists but belongs to different user"
                allure.attach(
                    f"Scenario: {scenario}\n"
                    f"Expense ID: {repo_return.id}\n"
                    f"Owner ID: {repo_return.user_id}\n"
                    f"Requesting User ID: {user_id}\n"
                    f"Expected: None (authorization failure)",
                    "Test Scenario",
                    allure.attachment_type.TEXT
                )

        with allure.step("Mock expense repository"):
            mock_expense_repository = Mock()
            mock_approval_repository = Mock()

            if repo_return:
                allure.attach(
                    f"ID: {repo_return.id}\n"
                    f"User ID: {repo_return.user_id}\n"
                    f"Amount: ${repo_return.amount}\n"
                    f"Description: {repo_return.description}\n"
                    f"Date: {repo_return.date}",
                    "Repository Return Value",
                    allure.attachment_type.TEXT
                )
            else:
                allure.attach(
                    "Repository returns None (expense not found)",
                    "Repository Return Value",
                    allure.attachment_type.TEXT
                )

        with allure.step("Configure mock to return test data"):
            mock_expense_repository.find_by_id.return_value = repo_return
            allure.attach(
                f"find_by_id(1) → {repo_return}",
                "Mock Configuration",
                allure.attachment_type.TEXT
            )

        with allure.step("Initialize ExpenseService with mocked repositories"):
            service = ExpenseService(mock_expense_repository, mock_approval_repository)
            allure.attach(
                "ExpenseService initialized with mocked dependencies",
                "Service Initialization",
                allure.attachment_type.TEXT
            )

        with allure.step(f"Act: Call get_expense_by_id(expense_id=1, user_id={user_id})"):
            result = service.get_expense_by_id(expense_id=1, user_id=user_id)
            allure.attach(
                f"Expense ID: 1\nUser ID: {user_id}",
                "Method Parameters",
                allure.attachment_type.TEXT
            )
            allure.attach(
                f"Result: {result}",
                "Method Return Value",
                allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify return value matches expected result"):
            if expected_found:
                allure.attach(
                    f"Expected: Expense object\nActual: {result}",
                    "Expected vs Actual",
                    allure.attachment_type.TEXT
                )
                assert result == repo_return, \
                    f"Expected expense object {repo_return} but got {result}"
                allure.attach(
                    f"✓ Returned correct expense\n"
                    f"  ID: {result.id}\n"
                    f"  User ID: {result.user_id}\n"
                    f"  Amount: ${result.amount}\n"
                    f"  Description: {result.description}",
                    "Success: Expense Retrieved",
                    allure.attachment_type.TEXT
                )
            else:
                allure.attach(
                    f"Expected: None\nActual: {result}",
                    "Expected vs Actual",
                    allure.attachment_type.TEXT
                )
                assert result is None, \
                    f"Expected None but got {result}"

                if repo_return is None:
                    allure.attach(
                        "✓ Correctly returned None (expense not found)",
                        "Success: Not Found",
                        allure.attachment_type.TEXT
                    )
                else:
                    allure.attach(
                        f"✓ Correctly returned None (authorization failure)\n"
                        f"  Expense Owner: {repo_return.user_id}\n"
                        f"  Requesting User: {user_id}",
                        "Success: Authorization Check",
                        allure.attachment_type.TEXT
                    )

        with allure.step("Assert: Verify repository method was called correctly"):
            mock_expense_repository.find_by_id.assert_called_once_with(1)
            allure.attach(
                "✓ find_by_id(1) called exactly once",
                "Repository Call Verification",
                allure.attachment_type.TEXT
            )