"""
Python Requests API Tests for GET /api/expenses/{expenseId}
Tests the endpoint from the Employee application perspective

Usage:
    pytest test_get_expense_by_id_api.py -v
    pytest test_get_expense_by_id_api.py -v -m "positive"
    pytest test_get_expense_by_id_api.py -v -m "negative"
"""
import pytest
import requests
import allure
import os
import sys

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

BASE_URL = "http://localhost:5000"


@pytest.fixture(scope="module")
def auth_session():
    """Create authenticated session for employee1"""
    session = requests.Session()

    # Login as employee1
    login_response = session.post(
        f"{BASE_URL}/api/auth/login",
        json={
            "username": "employee1",
            "password": "password123"
        }
    )

    if login_response.status_code == 200:
        # Cookie is automatically handled by session
        yield session
    else:
        pytest.skip("Could not authenticate - server may not be running")

    # Logout after tests
    session.post(f"{BASE_URL}/api/auth/logout")
    session.close()


@allure.epic("Employee App")
@allure.feature("Get Expense By ID API Tests")
class test_GetExpenseByIdAPI:
    """API tests for GET /api/expenses/{expenseId} endpoint"""

    # ==================== HAPPY PATH TESTS ====================

    @allure.story("Get Expense Details")
    @allure.title("TC-API-001: Successfully retrieve own expense with valid ID")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.positive
    @pytest.mark.smoke
    def test_get_expense_by_id_valid_returns_200(self, auth_session):
        """Test retrieving an expense that belongs to the authenticated user"""

        # First, create an expense to retrieve
        create_response = auth_session.post(
            f"{BASE_URL}/api/expenses",
            json={
                "amount": 25.50,
                "description": "Test expense for retrieval",
                "date": "2024-12-01"
            }
        )
        assert create_response.status_code == 201
        expense_id = create_response.json()["expense"]["id"]

        # Now retrieve it
        response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

        # Assertions
        assert response.status_code == 200
        assert response.headers["Content-Type"] == "application/json"

        data = response.json()
        assert "expense" in data
        assert data["expense"]["id"] == expense_id
        assert data["expense"]["amount"] == 25.50
        assert data["expense"]["description"] == "Test expense for retrieval"
        assert data["expense"]["status"] in ["pending", "approved", "denied"]

    @allure.story("Get Expense Details")
    @allure.title("TC-API-002: Verify all expense fields are present")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.positive
    def test_get_expense_contains_all_fields(self, auth_session):
        """Test that response contains all required expense fields"""

        # Create an expense
        create_response = auth_session.post(
            f"{BASE_URL}/api/expenses",
            json={
                "amount": 100.00,
                "description": "Complete fields test",
                "date": "2024-12-15"
            }
        )
        expense_id = create_response.json()["expense"]["id"]

        # Retrieve it
        response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

        assert response.status_code == 200
        expense_data = response.json()["expense"]

        # Check all required fields exist
        required_fields = ["id", "amount", "description", "date", "status"]
        for field in required_fields:
            assert field in expense_data, f"Missing required field: {field}"

    @allure.story("Get Expense Details")
    @allure.title("TC-API-003: Retrieve pending expense shows correct status")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.positive
    def test_get_pending_expense_has_pending_status(self, auth_session):
        """Test that newly created expenses have pending status"""

        # Create expense (defaults to pending)
        create_response = auth_session.post(
            f"{BASE_URL}/api/expenses",
            json={
                "amount": 50.00,
                "description": "Pending status test",
                "date": "2024-12-20"
            }
        )
        expense_id = create_response.json()["expense"]["id"]

        # Retrieve and verify status
        response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

        assert response.status_code == 200
        assert response.json()["expense"]["status"] == "pending"

    # ==================== SAD PATH TESTS ====================

    @allure.story("Get Expense Details")
    @allure.title("TC-API-004: Non-existent expense ID returns 404")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.negative
    def test_get_expense_nonexistent_id_returns_404(self, auth_session):
        """Test retrieving an expense that doesn't exist"""

        response = auth_session.get(f"{BASE_URL}/api/expenses/99999")

        assert response.status_code == 404
        data = response.json()
        assert "error" in data
        assert "not found" in data["error"].lower()

    @allure.story("Get Expense Details")
    @allure.title("TC-API-005: Invalid expense ID format returns 400")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.negative
    def test_get_expense_invalid_id_format_returns_400(self, auth_session):
        """Test retrieving expense with non-numeric ID"""

        response = auth_session.get(f"{BASE_URL}/api/expenses/invalid-id")

        # May return 404 or 400 depending on routing
        assert response.status_code in [400, 404]

    @allure.story("Get Expense Details")
    @allure.title("TC-API-006: Negative expense ID returns 404")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.negative
    def test_get_expense_negative_id_returns_404(self, auth_session):
        """Test retrieving expense with negative ID"""

        response = auth_session.get(f"{BASE_URL}/api/expenses/-1")

        assert response.status_code in [400, 404]

    # ==================== AUTHENTICATION TESTS ====================

    @allure.story("Authentication")
    @allure.title("TC-API-007: Unauthenticated request returns 401")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.negative
    def test_get_expense_no_auth_returns_401(self):
        """Test accessing endpoint without authentication"""

        response = requests.get(f"{BASE_URL}/api/expenses/1")

        assert response.status_code == 401
        data = response.json()
        assert "error" in data

    @allure.story("Authentication")
    @allure.title("TC-API-008: Invalid JWT token returns 401")
    @allure.severity(allure.severity_level.CRITICAL)
    @pytest.mark.negative
    def test_get_expense_invalid_token_returns_401(self):
        """Test accessing endpoint with invalid JWT token"""

        session = requests.Session()
        session.cookies.set("jwt_token", "invalid-token-12345")

        response = session.get(f"{BASE_URL}/api/expenses/1")

        assert response.status_code in [401, 403]

    # ==================== EDGE CASE TESTS ====================

    @allure.story("Get Expense Details")
    @allure.title("TC-API-009: Expense ID zero returns 404")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.negative
    def test_get_expense_zero_id_returns_404(self, auth_session):
        """Test retrieving expense with ID of 0"""

        response = auth_session.get(f"{BASE_URL}/api/expenses/0")

        assert response.status_code == 404

    @allure.story("Get Expense Details")
    @allure.title("TC-API-010: Very large expense ID handles gracefully")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.negative
    def test_get_expense_large_id_returns_404(self, auth_session):
        """Test retrieving expense with very large ID"""

        response = auth_session.get(f"{BASE_URL}/api/expenses/999999999")

        assert response.status_code == 404

    # ==================== RESPONSE VALIDATION TESTS ====================

    @allure.story("Response Validation")
    @allure.title("TC-API-011: Response time is acceptable")
    @allure.severity(allure.severity_level.NORMAL)
    @pytest.mark.positive
    def test_get_expense_response_time_acceptable(self, auth_session):
        """Test that API responds within acceptable time"""

        # Create expense first
        create_response = auth_session.post(
            f"{BASE_URL}/api/expenses",
            json={
                "amount": 10.00,
                "description": "Performance test",
                "date": "2024-12-01"
            }
        )
        expense_id = create_response.json()["expense"]["id"]

        # Measure response time
        response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

        assert response.status_code == 200
        # API should respond within 1 second
        assert response.elapsed.total_seconds() < 1.0

    @allure.story("Response Validation")
    @allure.title("TC-API-012: Response has correct content type")
    @allure.severity(allure.severity_level.MINOR)
    @pytest.mark.positive
    def test_get_expense_content_type_is_json(self, auth_session):
        """Test that response content type is JSON"""

        # Create expense
        create_response = auth_session.post(
            f"{BASE_URL}/api/expenses",
            json={
                "amount": 15.00,
                "description": "Content type test",
                "date": "2024-12-01"
            }
        )
        expense_id = create_response.json()["expense"]["id"]

        response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

        assert response.status_code == 200
        assert "application/json" in response.headers["Content-Type"]


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--alluredir=allure-results"])