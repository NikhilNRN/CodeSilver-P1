"""
Integration Tests for Repository Layer (Python Employee App)

These tests use a REAL SQLite database to verify repository operations.
The test database is created at a SEPARATE PATH from the production database.

Key Concepts:
- Testing with real database connections (vs mocked)
- Database setup and teardown patterns
- Integration test isolation
"""
import pytest
import sqlite3
import os
import sys
import allure
import json
from datetime import datetime

# Add parent directories to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from repository.database import DatabaseConnection
from repository.user_repository import UserRepository
from repository.expense_repository import ExpenseRepository
from repository.approval_repository import ApprovalRepository
from repository.user_model import User
from repository.expense_model import Expense
from repository.approval_model import Approval

# Test database path - SEPARATE from production
TEST_DB_PATH = os.path.join(os.path.dirname(__file__), 'test_expense_manager.db')
SEED_SQL_PATH = os.path.join(os.path.dirname(__file__), 'seed_data_20241229.sql')


@pytest.fixture(scope='module')
def test_db_connection():
    """
    Create a real database connection for integration testing.
    Uses a separate test database file.

    Scope is 'module' to reuse connection across all tests in this file.
    """
    with allure.step("Setup: Initialize test database"):
        # Create database connection with test path
        db_conn = DatabaseConnection(TEST_DB_PATH)

        with allure.step("Create database schema"):
            db_conn.initialize_database()
            allure.attach(
                f"Database Path: {TEST_DB_PATH}",
                name="Test Database Location",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Load seed data"):
            with open(SEED_SQL_PATH, 'r') as f:
                seed_sql = f.read()

            with db_conn.get_connection() as conn:
                conn.executescript(seed_sql)
                conn.commit()

            allure.attach(
                seed_sql[:500] + "..." if len(seed_sql) > 500 else seed_sql,
                name="Seed Data Preview",
                attachment_type=allure.attachment_type.TEXT
            )

    yield db_conn

    # Cleanup: remove test database after tests
    # Uncomment to enable cleanup
    # if os.path.exists(TEST_DB_PATH):
    #    os.remove(TEST_DB_PATH)


@pytest.fixture
def user_repository(test_db_connection):
    """Create UserRepository with real database connection."""
    return UserRepository(test_db_connection)


@pytest.fixture
def expense_repository(test_db_connection):
    """Create ExpenseRepository with real database connection."""
    return ExpenseRepository(test_db_connection)


@pytest.fixture
def approval_repository(test_db_connection):
    """Create ApprovalRepository with real database connection."""
    return ApprovalRepository(test_db_connection)


@allure.epic("Employee App")
@allure.feature("User Repository Integration")
@allure.tag("integration", "database", "user-repository")
class TestUserRepositoryIntegration:
    """Integration tests for UserRepository with real database."""

    @allure.story("Find User")
    @allure.title("TC-INT-USER-001: Find user by username from real database")
    @allure.description("Verifies that UserRepository can successfully retrieve a user by username from the seeded database")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-USER-001")
    @pytest.mark.integration
    def test_find_by_username_real_db(self, user_repository):
        """Test finding user by username from seeded database."""

        username = 'employee1'
        allure.dynamic.parameter("username", username)

        with allure.step(f"Act: Find user by username '{username}'"):
            result = user_repository.find_by_username(username)

            if result:
                allure.attach(
                    f"User ID: {result.id}\n"
                    f"Username: {result.username}\n"
                    f"Role: {result.role}",
                    name="Retrieved User",
                    attachment_type=allure.attachment_type.TEXT
                )

        with allure.step("Assert: Verify user was found"):
            assert result is not None, "User should exist in seeded database"

        with allure.step("Assert: Verify user ID is correct"):
            assert result.id == 1

        with allure.step("Assert: Verify username matches"):
            assert result.username == 'employee1'

        with allure.step("Assert: Verify user role is Employee"):
            assert result.role == 'Employee'

    @allure.story("Find User")
    @allure.title("TC-INT-USER-002: Find user by ID from real database")
    @allure.description("Verifies that UserRepository can retrieve a user by their ID")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-USER-002")
    @pytest.mark.integration
    def test_find_by_id_real_db(self, user_repository):
        """Test finding user by ID from seeded database."""

        user_id = 1
        allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Find user by ID {user_id}"):
            result = user_repository.find_by_id(user_id)

            if result:
                allure.attach(
                    f"User ID: {result.id}\n"
                    f"Username: {result.username}\n"
                    f"Role: {result.role}",
                    name="Retrieved User",
                    attachment_type=allure.attachment_type.TEXT
                )

        with allure.step("Assert: Verify user was found"):
            assert result is not None, f"User with ID {user_id} should exist"

        with allure.step("Assert: Verify user ID matches"):
            assert result.id == user_id

        with allure.step("Assert: Verify username is correct"):
            assert result.username == 'employee1'

        with allure.step("Assert: Verify role is Employee"):
            assert result.role == 'Employee'

    @allure.story("Find User")
    @allure.title("TC-INT-USER-003: Find non-existent user returns None")
    @allure.description("Verifies that querying for a non-existent user returns None instead of raising an error")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-USER-003")
    @pytest.mark.integration
    def test_find_nonexistent_user(self, user_repository):
        """Test finding non-existent user returns None."""

        nonexistent_username = 'nonexistent_user_12345'
        allure.dynamic.parameter("username", nonexistent_username)

        with allure.step(f"Act: Attempt to find non-existent user '{nonexistent_username}'"):
            result = user_repository.find_by_username(nonexistent_username)

        with allure.step("Assert: Verify result is None"):
            assert result is None, "Non-existent user should return None"

            allure.attach(
                f"Search for '{nonexistent_username}' correctly returned None",
                name="Verification Result",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Create User")
    @allure.title("TC-INT-USER-004: Create new user in real database")
    @allure.description("Verifies that UserRepository can create a new user and persist it to the database")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-USER-004")
    @pytest.mark.integration
    def test_create_user_real_db(self, user_repository):
        """Test creating a new user in real database."""

        with allure.step("Arrange: Create User object"):
            new_user = User(
                id=None,  # Will be auto-generated
                username=f'test_user_{datetime.now().timestamp()}',
                password='hashed_password_123',
                role='Employee'
            )

            allure.attach(
                f"Username: {new_user.username}\n"
                f"Role: {new_user.role}",
                name="New User Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Create user in database"):
            created_user = user_repository.create(new_user)

            allure.dynamic.parameter("user_id", created_user.id)
            allure.attach(
                f"Created User ID: {created_user.id}\n"
                f"Username: {created_user.username}\n"
                f"Role: {created_user.role}",
                name="Created User",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify user was created with ID"):
            assert created_user is not None
            assert created_user.id is not None, "Created user should have an ID"

        with allure.step("Assert: Verify username matches"):
            assert created_user.username == new_user.username

        with allure.step("Assert: Verify role matches"):
            assert created_user.role == new_user.role

        with allure.step("Verify: Find the created user by username"):
            found_user = user_repository.find_by_username(created_user.username)
            assert found_user is not None, "Created user should be retrievable"
            assert found_user.id == created_user.id

            allure.attach(
                f"User successfully created and verified:\n"
                f"  ID: {found_user.id}\n"
                f"  Username: {found_user.username}",
                name="Creation Verification",
                attachment_type=allure.attachment_type.TEXT
            )


@allure.epic("Employee App")
@allure.feature("Expense Repository Integration")
@allure.tag("integration", "database", "expense-repository")
class TestExpenseRepositoryIntegration:
    """Integration tests for ExpenseRepository with real database."""

    @allure.story("Find Expense")
    @allure.title("TC-INT-EXP-001: Find expense by ID from real database")
    @allure.description("Verifies that ExpenseRepository can retrieve an expense by ID from the seeded database")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-EXP-001")
    @pytest.mark.integration
    def test_find_by_id_real_db(self, expense_repository):
        """Test finding expense by ID from seeded database."""

        expense_id = 1
        allure.dynamic.parameter("expense_id", expense_id)

        with allure.step(f"Act: Find expense by ID {expense_id}"):
            result = expense_repository.find_by_id(expense_id)

            if result:
                allure.attach(
                    f"Expense ID: {result.id}\n"
                    f"Amount: ${result.amount}\n"
                    f"Description: {result.description}\n"
                    f"User ID: {result.user_id}\n"
                    f"Date: {result.date}",
                    name="Retrieved Expense",
                    attachment_type=allure.attachment_type.TEXT
                )

        with allure.step("Assert: Verify expense was found"):
            assert result is not None

        with allure.step("Assert: Verify expense ID is correct"):
            assert result.id == 1

        with allure.step("Assert: Verify amount is correct"):
            assert result.amount == 150.00

        with allure.step("Assert: Verify description matches"):
            assert result.description == 'Business lunch'

    @allure.story("Find Expense")
    @allure.title("TC-INT-EXP-002: Find expenses by user ID from real database")
    @allure.description("Verifies that ExpenseRepository can retrieve all expenses for a specific user")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-EXP-002")
    @pytest.mark.integration
    def test_find_by_user_id_real_db(self, expense_repository):
        """Test finding all expenses for a user from seeded database."""

        user_id = 1
        allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Find all expenses for user {user_id}"):
            results = expense_repository.find_by_user_id(user_id)

            allure.attach(
                f"Found {len(results)} expense(s) for user {user_id}",
                name="Query Result Count",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expenses were found"):
            assert results is not None
            assert len(results) > 0, f"User {user_id} should have expenses in seeded data"

        with allure.step("Assert: Verify all expenses belong to the user"):
            for expense in results:
                assert expense.user_id == user_id

            allure.attach(
                f"All {len(results)} expense(s) correctly belong to user {user_id}",
                name="User ID Verification",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expenses have required fields"):
            for expense in results:
                assert expense.id is not None
                assert expense.amount is not None
                assert expense.description is not None

    @allure.story("Create Expense")
    @allure.title("TC-INT-EXP-003: Create new expense in real database")
    @allure.description("Verifies that ExpenseRepository can create a new expense with an associated approval record")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-EXP-003")
    @pytest.mark.integration
    def test_create_expense_real_db(self, expense_repository, approval_repository):
        """Test creating a new expense with approval record."""

        with allure.step("Arrange: Create Expense object"):
            new_expense = Expense(
                id=None,
                user_id=1,
                amount=75.50,
                description=f'Integration test expense {datetime.now().timestamp()}',
                date='2024-12-30'
            )

            allure.attach(
                f"User ID: {new_expense.user_id}\n"
                f"Amount: ${new_expense.amount}\n"
                f"Description: {new_expense.description}\n"
                f"Date: {new_expense.date}",
                name="New Expense Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Create expense in database"):
            created_expense = expense_repository.create(new_expense)

            allure.dynamic.parameter("expense_id", created_expense.id)
            allure.attach(
                f"Created Expense ID: {created_expense.id}",
                name="Created Expense",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify expense was created"):
            assert created_expense is not None
            assert created_expense.id is not None

        with allure.step("Assert: Verify expense data matches"):
            assert created_expense.amount == new_expense.amount
            assert created_expense.description == new_expense.description
            assert created_expense.user_id == new_expense.user_id

        with allure.step("Verify: Check approval record was created"):
            approval = approval_repository.find_by_expense_id(created_expense.id)
            assert approval is not None, "Approval record should be created with expense"
            assert approval.expense_id == created_expense.id
            assert approval.status == 'pending', "New expenses should have pending status"

            allure.attach(
                f"Approval ID: {approval.id}\n"
                f"Expense ID: {approval.expense_id}\n"
                f"Status: {approval.status}",
                name="Associated Approval Record",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Update Expense")
    @allure.title("TC-INT-EXP-004: Update expense in real database")
    @allure.description("Verifies that ExpenseRepository can update an existing expense's data")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-EXP-004")
    @pytest.mark.integration
    def test_update_expense_real_db(self, expense_repository):
        """Test updating an expense in real database."""

        with allure.step("Arrange: Find existing expense"):
            expense = expense_repository.find_by_id(1)
            assert expense is not None, "Test expense should exist"

            original_amount = expense.amount
            original_description = expense.description

        with allure.step("Arrange: Modify expense data"):
            expense.amount = 999.99
            expense.description = 'UPDATED: Integration test'

            allure.attach(
                f"Original Amount: ${original_amount} → New: ${expense.amount}\n"
                f"Original Description: {original_description}\n"
                f"New Description: {expense.description}",
                name="Expense Modifications",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Update expense in database"):
            updated_expense = expense_repository.update(expense)

        with allure.step("Assert: Verify update was successful"):
            assert updated_expense is not None

        with allure.step("Assert: Verify updated amount"):
            assert updated_expense.amount == 999.99

        with allure.step("Assert: Verify updated description"):
            assert updated_expense.description == 'UPDATED: Integration test'

        with allure.step("Verify: Fetch expense again to confirm persistence"):
            fetched_expense = expense_repository.find_by_id(expense.id)
            assert fetched_expense.amount == 999.99
            assert fetched_expense.description == 'UPDATED: Integration test'

            allure.attach(
                "Update successfully persisted to database",
                name="Persistence Verification",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Delete Expense")
    @allure.title("TC-INT-EXP-005: Delete expense from real database")
    @allure.description("Verifies that ExpenseRepository can delete an expense and its associated approval record")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-EXP-005")
    @pytest.mark.integration
    def test_delete_expense_real_db(self, expense_repository, approval_repository):
        """Test deleting an expense and its approval record."""

        with allure.step("Arrange: Create an expense to delete"):
            new_expense = Expense(
                id=None,
                user_id=1,
                amount=50.00,
                description='Expense to be deleted',
                date='2024-12-31'
            )
            created_expense = expense_repository.create(new_expense)
            expense_id = created_expense.id

            allure.dynamic.parameter("expense_id", expense_id)
            allure.attach(
                f"Created expense ID {expense_id} for deletion test",
                name="Test Setup",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step(f"Act: Delete expense {expense_id}"):
            expense_repository.delete(expense_id)

        with allure.step("Assert: Verify expense is deleted"):
            deleted_expense = expense_repository.find_by_id(expense_id)
            assert deleted_expense is None, "Deleted expense should not be found"

        with allure.step("Verify: Check approval record is also deleted"):
            approval = approval_repository.find_by_expense_id(expense_id)
            assert approval is None, "Associated approval should be deleted with expense"

            allure.attach(
                f"Expense {expense_id} and its approval record successfully deleted",
                name="Deletion Verification",
                attachment_type=allure.attachment_type.TEXT
            )


@allure.epic("Employee App")
@allure.feature("Approval Repository Integration")
@allure.tag("integration", "database", "approval-repository")
class TestApprovalRepositoryIntegration:
    """Integration tests for ApprovalRepository with real database."""

    @allure.story("Find Approval")
    @allure.title("TC-INT-APR-001: Find approval by expense ID from real database")
    @allure.description("Verifies that ApprovalRepository can retrieve an approval record by expense ID")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-APR-001")
    @pytest.mark.integration
    def test_find_by_expense_id_real_db(self, approval_repository):
        """Test finding approval by expense ID from seeded database."""

        expense_id = 1
        allure.dynamic.parameter("expense_id", expense_id)

        with allure.step(f"Act: Find approval for expense {expense_id}"):
            result = approval_repository.find_by_expense_id(expense_id)

            if result:
                allure.attach(
                    f"Approval ID: {result.id}\n"
                    f"Expense ID: {result.expense_id}\n"
                    f"Status: {result.status}\n"
                    f"Reviewer: {result.reviewer}\n"
                    f"Comment: {result.comment}\n"
                    f"Review Date: {result.review_date}",
                    name="Retrieved Approval",
                    attachment_type=allure.attachment_type.TEXT
                )

        with allure.step("Assert: Verify approval was found"):
            assert result is not None, f"Approval for expense {expense_id} should exist"

        with allure.step("Assert: Verify approval belongs to correct expense"):
            assert result.expense_id == expense_id

        with allure.step("Assert: Verify approval has valid status"):
            assert result.status in ['pending', 'approved', 'denied']

    @allure.story("Find Approval")
    @allure.title("TC-INT-APR-002: Find pending approval from real database")
    @allure.description("Verifies that ApprovalRepository can filter approvals by pending status")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-APR-002")
    @pytest.mark.integration
    def test_find_pending_approval_real_db(self, approval_repository):
        """Test finding pending approval from seeded database."""

        with allure.step("Act: Find all pending approvals"):
            pending_approvals = approval_repository.find_by_status('pending')

            allure.attach(
                f"Found {len(pending_approvals)} pending approval(s)",
                name="Query Result",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify pending approvals exist"):
            assert len(pending_approvals) > 0, "Should have pending approvals in seeded data"

        with allure.step("Assert: Verify all approvals have pending status"):
            for approval in pending_approvals:
                assert approval.status == 'pending'

            allure.attach(
                f"All {len(pending_approvals)} approval(s) correctly have 'pending' status",
                name="Status Verification",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Update Approval")
    @allure.title("TC-INT-APR-003: Update approval status in real database")
    @allure.description("Verifies that ApprovalRepository can update an approval's status and metadata")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.testcase("TC-INT-APR-003")
    @pytest.mark.integration
    def test_update_status_real_db(self, approval_repository, expense_repository):
        """Test updating approval status in real database."""

        with allure.step("Arrange: Create expense with pending approval"):
            new_expense = Expense(
                id=None,
                user_id=1,
                amount=100.00,
                description='Approval update test',
                date='2024-12-31'
            )
            created_expense = expense_repository.create(new_expense)
            expense_id = created_expense.id

        with allure.step("Arrange: Get the approval record"):
            approval = approval_repository.find_by_expense_id(expense_id)
            original_status = approval.status

            allure.dynamic.parameter("approval_id", approval.id)
            allure.attach(
                f"Original Status: {original_status}",
                name="Initial State",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Update approval to 'approved' status"):
            approval.status = 'approved'
            approval.reviewer = 3
            approval.comment = 'Approved during integration test'
            approval.review_date = '2024-12-31'

            approval_repository.update_status(approval)

            allure.attach(
                f"New Status: {approval.status}\n"
                f"Reviewer: {approval.reviewer}\n"
                f"Comment: {approval.comment}\n"
                f"Review Date: {approval.review_date}",
                name="Updated Approval Data",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Verify: Fetch approval to confirm update"):
            updated_approval = approval_repository.find_by_expense_id(expense_id)

            with allure.step("Assert: Verify status was updated"):
                assert updated_approval.status == 'approved'

            with allure.step("Assert: Verify reviewer was set"):
                assert updated_approval.reviewer == 3

            with allure.step("Assert: Verify comment was saved"):
                assert updated_approval.comment == 'Approved during integration test'

            allure.attach(
                f"Status updated: {original_status} → {updated_approval.status}",
                name="Update Verification",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Find with Status")
    @allure.title("TC-INT-APR-004: Find expenses with status for user")
    @allure.description("Verifies that ApprovalRepository can join expenses with their approval status for a user")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.testcase("TC-INT-APR-004")
    @pytest.mark.integration
    def test_find_expenses_with_status_real_db(self, approval_repository):
        """Test finding expenses with approval status for a user."""

        user_id = 1
        allure.dynamic.parameter("user_id", user_id)

        with allure.step(f"Act: Find expenses with status for user {user_id}"):
            results = approval_repository.find_expenses_with_status_for_user(user_id)

            allure.attach(
                f"Found {len(results)} expense(s) with status",
                name="Query Result Count",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify results exist"):
            assert len(results) > 0, f"User {user_id} should have expenses with status"

        with allure.step("Assert: Verify result structure"):
            for item in results:
                with allure.step(f"Check tuple structure for expense {item[0].id if item[0] else 'N/A'}"):
                    assert isinstance(item, tuple), "Each result should be a tuple"
                    assert len(item) == 2, "Tuple should contain (Expense, Approval)"

                    expense, approval = item
                    assert isinstance(expense, Expense), "First element should be Expense"
                    assert isinstance(approval, Approval), "Second element should be Approval"
                    assert expense.id == approval.expense_id, "Expense and Approval should be related"

            allure.attach(
                f"All {len(results)} result(s) have correct structure:\n"
                f"  - Type: tuple\n"
                f"  - Length: 2\n"
                f"  - Elements: (Expense, Approval)",
                name="Structure Validation",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify all expenses belong to the user"):
            for expense, approval in results:
                assert expense.user_id == user_id

            allure.attach(
                f"All expenses correctly belong to user {user_id}",
                name="User ID Verification",
                attachment_type=allure.attachment_type.TEXT
            )


if __name__ == '__main__':
    pytest.main([__file__, '-v', '-m', 'integration', '--alluredir=allure-results'])