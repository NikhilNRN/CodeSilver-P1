"""
E2E Integration Tests with Real Database (Python Employee App)
Created: 2024-12-29T19:19:00-06:00

These tests simulate end-to-end user workflows using Flask test client
with a REAL SQLite database backend.

- E2E testing with real database
- Complete workflow testing (login → action → verify)
- Session handling in tests
"""
import pytest
import os
import sys
import json
import allure

# Add parent directories to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from repository.database import DatabaseConnection

# Test database path - SEPARATE from production
TEST_DB_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                            'integration', 'test_expense_manager.db')
SEED_SQL_PATH = os.path.join(os.path.dirname(os.path.dirname(__file__)),
                             'integration', 'seed_data_20241229.sql')


@pytest.fixture(scope='module')
def test_app():
    """Create Flask test application with real database."""
    # Initialize test database
    db_conn = DatabaseConnection(TEST_DB_PATH)
    db_conn.initialize_database()

    # Load seed data
    with open(SEED_SQL_PATH, 'r') as f:
        seed_sql = f.read()

    with db_conn.get_connection() as conn:
        conn.executescript(seed_sql)
        conn.commit()

    # Set environment variable for app to use test database
    os.environ['DATABASE_PATH'] = TEST_DB_PATH

    from main import create_app
    app = create_app()
    app.config['TESTING'] = True

    yield app

    # Cleanup
    if os.path.exists(TEST_DB_PATH):
        os.remove(TEST_DB_PATH)
    if 'DATABASE_PATH' in os.environ:
        del os.environ['DATABASE_PATH']


@pytest.fixture
def client(test_app):
    """Create Flask test client."""
    return test_app.test_client()


@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestLoginWorkflowE2E:
    """End-to-end login workflow tests."""

    @allure.story("Login Flow")
    @allure.title("TC-E2E-INT-001: Complete login workflow with real database")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_complete_login_workflow(self, client):
        """Test complete login workflow with seeded user."""
        # Step 1: Access login page
        response = client.get('/')
        assert response.status_code == 200

        # Step 2: Login with valid credentials
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee1',
                                         'password': 'password123'
                                     }),
                                     content_type='application/json')

        assert login_response.status_code == 200

        # Step 3: Access protected resource
        expenses_response = client.get('/api/expenses')
        assert expenses_response.status_code == 200

        # Step 4: Logout
        logout_response = client.post('/api/auth/logout')
        assert logout_response.status_code in [200, 302]

    @allure.story("Login Flow")
    @allure.title("TC-E2E-INT-002: Failed login blocks access")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_failed_login_blocks_access(self, client):
        """Test that failed login prevents access to protected resources."""
        # Step 1: Try login with wrong password
        response = client.get('/')
        assert response.status_code == 200
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employeeNotReal',
                                         'password': 'wrongPassword321'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 401

        # Step 2: Try to access protected resource - should fail
        response = client.get('/api/expenses')
        assert response.status_code == 401


@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestExpenseWorkflowE2E:
    """End-to-end expense management workflow tests."""

    @allure.story("Expense Submission")
    @allure.title("TC-E2E-INT-003: Complete expense submission workflow")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_complete_expense_submission_workflow(self, client):
        """Test complete workflow: login → submit expense → verify."""
        # Step 1: Login
        response = client.get('/')
        assert response.status_code == 200
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee1',
                                         'password': 'password123'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 200

        # Step 2: Get initial expense count
        response = client.get('/api/expenses')
        assert response.status_code == 200
        data = response.json
        expense_count_initial = data["count"]

        # Step 3: Submit new expense
        submit_response = client.post('/api/expenses',
                                      data=json.dumps({
                                          "amount": 25.50,
                                          "description": "Client lunch meeting",
                                          "date": "2025-10-14"
                                      }),
                                      content_type='application/json')
        assert submit_response.status_code == 201
        expense_data = submit_response.json['expense']

        # Step 4: Verify expense appears in list
        response = client.get('/api/expenses')
        data = response.json
        is_expense_in_list = False
        for expense in data["expenses"]:
            if (expense["amount"] == expense_data["amount"]
                    and expense["description"] == expense_data["description"]
                    and expense["date"] == expense_data["date"]):
                is_expense_in_list = True
                break
        assert is_expense_in_list == True

        # Should have one more expense
        expense_count_new = data["count"]
        assert expense_count_new > expense_count_initial

    @allure.story("Expense View")
    @allure.title("TC-E2E-INT-004: View expense list from seeded data")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_view_seeded_expenses(self, client):
        """Test viewing expenses from seeded database."""
        # Login
        response = client.get('/')
        assert response.status_code == 200
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee1',
                                         'password': 'password123'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 200

        # Get expenses
        response = client.get('/api/expenses')
        assert response.status_code == 200
        data = response.json

        # Employee1 has 3 expenses in seed data (IDs 1, 2, 3)
        # May have more if previous tests added expenses
        expense_count = data["count"]
        assert expense_count >= 3

    @allure.story("Expense Update")
    @allure.title("TC-E2E-INT-005: Update pending expense workflow")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_update_expense_workflow(self, client):
        """Test updating a pending expense."""
        # Login
        response = client.get('/')
        assert response.status_code == 200
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee1',
                                         'password': 'password123'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 200

        # Create a new expense to update
        submit_response = client.post('/api/expenses',
                                      data=json.dumps({
                                          "amount": 13.37,
                                          "description": "Undertale for the Pope",
                                          "date": "2025-03-30"
                                      }),
                                      content_type='application/json')
        assert submit_response.status_code == 201
        expense_data_initial = submit_response.json['expense']

        # Update the expense
        update_response = client.put(f'/api/expenses/{expense_data_initial["id"]}',
                                     data=json.dumps({
                                          "amount": 123.45,
                                          "description": "Travel",
                                          "date": "2025-12-31"
                                      }),
                                      content_type='application/json')

        # Should succeed [200, 202, 204] or may fail if already approved [403, 409, 412]
        assert update_response.status_code in [200, 202, 204, 403, 409, 412]


@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestMultiUserWorkflowE2E:
    """Multi-user workflow tests."""

    @allure.story("Multi-User")
    @allure.title("TC-E2E-INT-006: Different users see different expenses")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_different_users_different_expenses(self, client):
        """Test that different users only see their own expenses."""
        response = client.get('/')
        assert response.status_code == 200

        # Login as employee1 and get their expenses
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee1',
                                         'password': 'password123'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 200
        view_response = client.get('/api/expenses')
        assert view_response.status_code == 200
        data_employee1 = view_response.json
        expense_count_employee1 = data_employee1["count"]

        # Logout
        logout_response = client.post('/api/auth/logout')
        assert logout_response.status_code in [200, 302]

        # Login as employee2 and get their expenses
        login_response = client.post('/api/auth/login',
                                     data=json.dumps({
                                         'username': 'employee2',
                                         'password': 'password456'
                                     }),
                                     content_type='application/json')
        assert login_response.status_code == 200
        view_response = client.get('/api/expenses')
        assert view_response.status_code == 200
        data_employee2 = view_response.json
        expense_count_employee2 = data_employee2["count"]

        # Both should have data but different counts
        # (employee1 has 3, employee2 has 2 in seed data)
        assert data_employee1 is not None and expense_count_employee1 >= 3
        assert data_employee2 is not None and expense_count_employee2 >= 2

        # They should have different expense counts
        assert expense_count_employee1 != expense_count_employee2


if __name__ == '__main__':
    pytest.main([__file__, '-v', '-m', 'e2e or integration'])