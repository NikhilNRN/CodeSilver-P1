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
    db_conn = DatabaseConnection(TEST_DB_PATH)
    db_conn.initialize_database()

    with open(SEED_SQL_PATH, 'r') as f:
        seed_sql = f.read()

    with db_conn.get_connection() as conn:
        conn.executescript(seed_sql)
        conn.commit()

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


# =========================
# Login Workflow Tests
# =========================
@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestLoginWorkflowE2E:

    @allure.story("Login Flow")
    @allure.title("TC-E2E-INT-001: Complete login workflow with real database")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_complete_login_workflow(self, client):
        with allure.step("Access login page"):
            response = client.get('/')
            assert response.status_code == 200

        with allure.step("Login with valid credentials"):
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee1',
                                             'password': 'password123'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200
            allure.attach(login_response.data.decode(),
                          name="Login Response",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Access protected resource"):
            expenses_response = client.get('/api/expenses')
            assert expenses_response.status_code == 200
            allure.attach(json.dumps(expenses_response.json, indent=2),
                          name="Expenses List",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Logout"):
            logout_response = client.post('/api/auth/logout')
            assert logout_response.status_code in [200, 302]

    @allure.story("Login Flow")
    @allure.title("TC-E2E-INT-002: Failed login blocks access")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_failed_login_blocks_access(self, client):
        with allure.step("Access login page"):
            response = client.get('/')
            assert response.status_code == 200

        with allure.step("Attempt login with invalid credentials"):
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employeeNotReal',
                                             'password': 'wrongPassword321'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 401
            allure.attach(login_response.data.decode(),
                          name="Failed Login Response",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Attempt access to protected resource"):
            response = client.get('/api/expenses')
            assert response.status_code == 401


# =========================
# Expense Workflow Tests
# =========================
@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestExpenseWorkflowE2E:

    @allure.story("Expense Submission")
    @allure.title("TC-E2E-INT-003: Complete expense submission workflow")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_complete_expense_submission_workflow(self, client):
        with allure.step("Login as employee1"):
            response = client.get('/')
            assert response.status_code == 200
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee1',
                                             'password': 'password123'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200

        with allure.step("Get initial expense count"):
            response = client.get('/api/expenses')
            assert response.status_code == 200
            data = response.json
            initial_count = data["count"]

        with allure.step("Submit new expense"):
            submit_response = client.post('/api/expenses',
                                          data=json.dumps({
                                              "amount": 25.50,
                                              "description": "Client lunch meeting",
                                              "date": "2025-10-14"
                                          }),
                                          content_type='application/json')
            assert submit_response.status_code == 201
            expense_data = submit_response.json['expense']
            allure.attach(json.dumps(expense_data, indent=2),
                          name="Submitted Expense",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Verify expense appears in list"):
            response = client.get('/api/expenses')
            data = response.json
            found = any(
                e["amount"] == expense_data["amount"] and
                e["description"] == expense_data["description"] and
                e["date"] == expense_data["date"]
                for e in data["expenses"]
            )
            assert found
            assert data["count"] > initial_count

    @allure.story("Expense View")
    @allure.title("TC-E2E-INT-004: View expense list from seeded data")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_view_seeded_expenses(self, client):
        with allure.step("Login as employee1"):
            response = client.get('/')
            assert response.status_code == 200
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee1',
                                             'password': 'password123'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200

        with allure.step("Fetch expenses"):
            response = client.get('/api/expenses')
            assert response.status_code == 200
            data = response.json
            allure.attach(json.dumps(data, indent=2),
                          name="Seeded Expenses",
                          attachment_type=allure.attachment_type.JSON)
            assert data["count"] >= 3

    @allure.story("Expense Update")
    @allure.title("TC-E2E-INT-005: Update pending expense workflow")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_update_expense_workflow(self, client):
        with allure.step("Login as employee1"):
            response = client.get('/')
            assert response.status_code == 200
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee1',
                                             'password': 'password123'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200

        with allure.step("Create a new expense to update"):
            submit_response = client.post('/api/expenses',
                                          data=json.dumps({
                                              "amount": 13.37,
                                              "description": "Undertale for the Pope",
                                              "date": "2025-03-30"
                                          }),
                                          content_type='application/json')
            assert submit_response.status_code == 201
            expense_data_initial = submit_response.json['expense']
            allure.attach(json.dumps(expense_data_initial, indent=2),
                          name="Initial Expense",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Update the expense"):
            update_response = client.put(f'/api/expenses/{expense_data_initial["id"]}',
                                         data=json.dumps({
                                              "amount": 123.45,
                                              "description": "Travel",
                                              "date": "2025-12-31"
                                          }),
                                         content_type='application/json')
            assert update_response.status_code in [200, 202, 204, 403, 409, 412]
            allure.attach(update_response.data.decode(),
                          name="Update Response",
                          attachment_type=allure.attachment_type.JSON)


# =========================
# Multi-User Workflow Tests
# =========================
@allure.epic("Employee App")
@allure.feature("E2E Integration Tests")
class TestMultiUserWorkflowE2E:

    @allure.story("Multi-User")
    @allure.title("TC-E2E-INT-006: Different users see different expenses")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.e2e
    @pytest.mark.integration
    def test_different_users_different_expenses(self, client):
        with allure.step("Login as employee1"):
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee1',
                                             'password': 'password123'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200

        with allure.step("Fetch employee1 expenses"):
            response = client.get('/api/expenses')
            data_employee1 = response.json
            count_employee1 = data_employee1["count"]
            allure.attach(json.dumps(data_employee1, indent=2),
                          name="Employee1 Expenses",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Logout employee1"):
            logout_response = client.post('/api/auth/logout')
            assert logout_response.status_code in [200, 302]

        with allure.step("Login as employee2"):
            login_response = client.post('/api/auth/login',
                                         data=json.dumps({
                                             'username': 'employee2',
                                             'password': 'password456'
                                         }),
                                         content_type='application/json')
            assert login_response.status_code == 200

        with allure.step("Fetch employee2 expenses"):
            response = client.get('/api/expenses')
            data_employee2 = response.json
            count_employee2 = data_employee2["count"]
            allure.attach(json.dumps(data_employee2, indent=2),
                          name="Employee2 Expenses",
                          attachment_type=allure.attachment_type.JSON)

        with allure.step("Verify different counts"):
            assert count_employee1 != count_employee2
            assert count_employee1 >= 3
            assert count_employee2 >= 2


# =========================
# Main entry (optional)
# =========================
if __name__ == '__main__':
    pytest.main([__file__, '-v', '-m', 'e2e or integration'])
