"""
Python Requests API Tests for GET /api/expenses/{expenseId}
Tests the endpoint from the Employee application perspective

Usage:
    pytest test_get_expense_by_id_api.py -v
    pytest test_get_expense_by_id_api.py -v -m "positive"
    pytest test_get_expense_by_id_api.py -v -m "negative"
    pytest test_get_expense_by_id_api.py --alluredir=allure-results
"""
import pytest
import requests
import allure
import os
import sys
import json
from datetime import datetime

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

BASE_URL = "http://localhost:5000"


@pytest.fixture(scope="module")
def auth_session():
    """Create authenticated session for employee1"""
    with allure.step("Setup: Create authenticated session for employee1"):
        session = requests.Session()

        with allure.step("Attempt login as employee1"):
            login_response = session.post(
                f"{BASE_URL}/api/auth/login",
                json={
                    "username": "employee1",
                    "password": "password123"
                }
            )

            allure.attach(
                json.dumps(login_response.json(), indent=2),
                name="Login Response",
                attachment_type=allure.attachment_type.JSON
            )

        if login_response.status_code == 200:
            allure.attach(
                "Authentication successful",
                name="Auth Status",
                attachment_type=allure.attachment_type.TEXT
            )
            yield session
        else:
            pytest.skip("Could not authenticate - server may not be running")

        # Logout after tests
        with allure.step("Teardown: Logout and close session"):
            session.post(f"{BASE_URL}/api/auth/logout")
            session.close()


@allure.epic("Employee App")
@allure.feature("Get Expense By ID API Tests")
class TestGetExpenseByIdAPI:
    """API tests for GET /api/expenses/{expenseId} endpoint"""

    # Happy Path Tests
    @allure.story("Get Expense Details")
    @allure.title("TC-API-001: Successfully retrieve own expense with valid ID")
    @allure.description("""
    This test verifies that an authenticated employee can successfully retrieve
    their own expense details using a valid expense ID.
    
    Steps:
    1. Create a new expense
    2. Retrieve the expense by its ID
    3. Verify response status, structure, and data accuracy
    """)
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("api", "get", "happy-path")
    @allure.testcase("TC-API-001")
    @pytest.mark.positive
    @pytest.mark.smoke
    def test_get_expense_by_id_valid_returns_200(self, auth_session):
        """Test retrieving an expense that belongs to the authenticated user"""

        with allure.step("Arrange: Create a new expense for testing"):
            create_payload = {
                "amount": 25.50,
                "description": "Test expense for retrieval",
                "date": "2024-12-01"
            }

            allure.attach(
                json.dumps(create_payload, indent=2),
                name="Create Expense Payload",
                attachment_type=allure.attachment_type.JSON
            )

            create_response = auth_session.post(
                f"{BASE_URL}/api/expenses",
                json=create_payload
            )

            assert create_response.status_code == 201, "Failed to create test expense"
            expense_id = create_response.json()["expense"]["id"]

            allure.dynamic.parameter("expense_id", expense_id)
            allure.attach(
                json.dumps(create_response.json(), indent=2),
                name="Created Expense Response",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step(f"Act: Retrieve expense with ID {expense_id}"):
            start_time = datetime.now()
            response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")
            response_time = (datetime.now() - start_time).total_seconds()

            allure.attach(
                f"GET {BASE_URL}/api/expenses/{expense_id}",
                name="Request URL",
                attachment_type=allure.attachment_type.TEXT
            )

            allure.attach(
                f"{response_time:.3f} seconds",
                name="Response Time",
                attachment_type=allure.attachment_type.TEXT
            )

            allure.attach(
                json.dumps(response.json(), indent=2),
                name="Response Body",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify response status code is 200"):
            assert response.status_code == 200

        with allure.step("Assert: Verify content type is JSON"):
            assert response.headers["Content-Type"] == "application/json"

        with allure.step("Assert: Verify response structure and data"):
            data = response.json()

            with allure.step("Check 'expense' key exists"):
                assert "expense" in data

            with allure.step("Verify expense ID matches"):
                assert data["expense"]["id"] == expense_id

            with allure.step("Verify expense amount is correct"):
                assert data["expense"]["amount"] == 25.50

            with allure.step("Verify expense description matches"):
                assert data["expense"]["description"] == "Test expense for retrieval"

            with allure.step("Verify expense has valid status"):
                assert data["expense"]["status"] in ["pending", "approved", "denied"]

                allure.attach(
                    f"Expense Status: {data['expense']['status']}",
                    name="Verification Result",
                    attachment_type=allure.attachment_type.TEXT
                )

    @allure.story("Get Expense Details")
    @allure.title("TC-API-002: Verify all expense fields are present")
    @allure.description("Validates that the API response contains all required expense fields")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("api", "validation", "fields")
    @allure.testcase("TC-API-002")
    @pytest.mark.positive
    def test_get_expense_contains_all_fields(self, auth_session):
        """Test that response contains all required expense fields"""

        with allure.step("Arrange: Create expense with complete data"):
            create_response = auth_session.post(
                f"{BASE_URL}/api/expenses",
                json={
                    "amount": 100.00,
                    "description": "Complete fields test",
                    "date": "2024-12-15"
                }
            )
            expense_id = create_response.json()["expense"]["id"]
            allure.dynamic.parameter("expense_id", expense_id)

        with allure.step(f"Act: Retrieve expense {expense_id}"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")
            allure.attach(
                json.dumps(response.json(), indent=2),
                name="API Response",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify response status"):
            assert response.status_code == 200

        with allure.step("Assert: Check all required fields are present"):
            expense_data = response.json()["expense"]
            required_fields = ["id", "amount", "description", "date", "status"]

            for field in required_fields:
                with allure.step(f"Verify field '{field}' exists"):
                    assert field in expense_data, f"Missing required field: {field}"

            allure.attach(
                f"All {len(required_fields)} required fields present: {', '.join(required_fields)}",
                name="Field Validation Summary",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Get Expense Details")
    @allure.title("TC-API-003: Retrieve pending expense shows correct status")
    @allure.description("Verifies that newly created expenses have 'pending' status by default")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("api", "status", "pending")
    @allure.testcase("TC-API-003")
    @pytest.mark.positive
    def test_get_pending_expense_has_pending_status(self, auth_session):
        """Test that newly created expenses have pending status"""

        with allure.step("Arrange: Create new expense (defaults to pending)"):
            create_response = auth_session.post(
                f"{BASE_URL}/api/expenses",
                json={
                    "amount": 50.00,
                    "description": "Pending status test",
                    "date": "2024-12-20"
                }
            )
            expense_id = create_response.json()["expense"]["id"]
            allure.dynamic.parameter("expense_id", expense_id)

        with allure.step(f"Act: Retrieve expense {expense_id} and check status"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

            allure.attach(
                json.dumps(response.json(), indent=2),
                name="Expense Details",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify status code is 200"):
            assert response.status_code == 200

        with allure.step("Assert: Verify expense status is 'pending'"):
            actual_status = response.json()["expense"]["status"]
            assert actual_status == "pending"

            allure.attach(
                f"Expected: 'pending'\nActual: '{actual_status}'",
                name="Status Verification",
                attachment_type=allure.attachment_type.TEXT
            )

    # Sad Path Tests
    @allure.story("Get Expense Details")
    @allure.title("TC-API-004: Non-existent expense ID returns 404")
    @allure.description("Verifies appropriate error handling when requesting a non-existent expense")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("api", "error-handling", "404")
    @allure.testcase("TC-API-004")
    @pytest.mark.negative
    def test_get_expense_nonexistent_id_returns_404(self, auth_session):
        """Test retrieving an expense that doesn't exist"""

        nonexistent_id = 99999
        allure.dynamic.parameter("expense_id", nonexistent_id)

        with allure.step(f"Act: Attempt to retrieve non-existent expense {nonexistent_id}"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{nonexistent_id}")

            allure.attach(
                f"Status Code: {response.status_code}",
                name="Response Status",
                attachment_type=allure.attachment_type.TEXT
            )

            allure.attach(
                json.dumps(response.json(), indent=2),
                name="Error Response",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify 404 status code"):
            assert response.status_code == 404

        with allure.step("Assert: Verify error message contains 'not found'"):
            data = response.json()
            assert "error" in data
            assert "not found" in data["error"].lower()

    @allure.story("Get Expense Details")
    @allure.title("TC-API-005: Invalid expense ID format returns 400")
    @allure.description("Tests API behavior with non-numeric expense ID")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("api", "validation", "bad-request")
    @allure.testcase("TC-API-005")
    @pytest.mark.negative
    def test_get_expense_invalid_id_format_returns_400(self, auth_session):
        """Test retrieving expense with non-numeric ID"""

        invalid_id = "invalid-id"
        allure.dynamic.parameter("expense_id", invalid_id)

        with allure.step(f"Act: Request expense with invalid ID '{invalid_id}'"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{invalid_id}")

            allure.attach(
                f"Status Code: {response.status_code}",
                name="Response Status",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify error status (400 or 404)"):
            assert response.status_code in [400, 404]

    @allure.story("Get Expense Details")
    @allure.title("TC-API-006: Negative expense ID returns 404")
    @allure.description("Validates handling of negative expense IDs")
    @allure.severity(allure.severity_level.MINOR)
    @allure.tag("api", "edge-case", "negative-id")
    @allure.testcase("TC-API-006")
    @pytest.mark.negative
    def test_get_expense_negative_id_returns_404(self, auth_session):
        """Test retrieving expense with negative ID"""

        negative_id = -1
        allure.dynamic.parameter("expense_id", negative_id)

        with allure.step(f"Act: Request expense with negative ID {negative_id}"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{negative_id}")

        with allure.step("Assert: Verify appropriate error status"):
            assert response.status_code in [400, 404]

    # Auth Tests
    @allure.story("Authentication")
    @allure.title("TC-API-007: Unauthenticated request returns 401")
    @allure.description("Verifies that the endpoint requires authentication")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("api", "security", "authentication")
    @allure.testcase("TC-API-007")
    @pytest.mark.negative
    def test_get_expense_no_auth_returns_401(self):
        """Test accessing endpoint without authentication"""

        with allure.step("Act: Request expense without authentication"):
            response = requests.get(f"{BASE_URL}/api/expenses/1")

            allure.attach(
                "No authentication credentials provided",
                name="Request Context",
                attachment_type=allure.attachment_type.TEXT
            )

            allure.attach(
                json.dumps(response.json(), indent=2),
                name="Error Response",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify 401 Unauthorized status"):
            assert response.status_code == 401

        with allure.step("Assert: Verify error message exists"):
            data = response.json()
            assert "error" in data

    @allure.story("Authentication")
    @allure.title("TC-API-008: Invalid JWT token returns 401")
    @allure.description("Tests API behavior with malformed/invalid JWT token")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.tag("api", "security", "jwt")
    @allure.testcase("TC-API-008")
    @pytest.mark.negative
    def test_get_expense_invalid_token_returns_401(self):
        """Test accessing endpoint with invalid JWT token"""

        invalid_token = "invalid-token-12345"

        with allure.step("Arrange: Create session with invalid JWT token"):
            session = requests.Session()
            session.cookies.set("jwt_token", invalid_token)

            allure.attach(
                f"Invalid Token: {invalid_token}",
                name="Test Token",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Act: Attempt to access endpoint with invalid token"):
            response = session.get(f"{BASE_URL}/api/expenses/1")

        with allure.step("Assert: Verify unauthorized status (401 or 403)"):
            assert response.status_code in [401, 403]

    # Edge Case Tests
    @allure.story("Get Expense Details")
    @allure.title("TC-API-009: Expense ID zero returns 404")
    @allure.description("Tests edge case with expense ID of 0")
    @allure.severity(allure.severity_level.MINOR)
    @allure.tag("api", "edge-case", "zero-id")
    @allure.testcase("TC-API-009")
    @pytest.mark.negative
    def test_get_expense_zero_id_returns_404(self, auth_session):
        """Test retrieving expense with ID of 0"""

        with allure.step("Act: Request expense with ID = 0"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/0")
            allure.dynamic.parameter("expense_id", 0)

        with allure.step("Assert: Verify 404 status"):
            assert response.status_code == 404

    @allure.story("Get Expense Details")
    @allure.title("TC-API-010: Very large expense ID handles gracefully")
    @allure.description("Validates behavior with extremely large expense ID")
    @allure.severity(allure.severity_level.MINOR)
    @allure.tag("api", "edge-case", "large-id")
    @allure.testcase("TC-API-010")
    @pytest.mark.negative
    def test_get_expense_large_id_returns_404(self, auth_session):
        """Test retrieving expense with very large ID"""

        large_id = 999999999
        allure.dynamic.parameter("expense_id", large_id)

        with allure.step(f"Act: Request expense with large ID {large_id}"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{large_id}")

        with allure.step("Assert: Verify 404 status"):
            assert response.status_code == 404

    # Response Validation Tests
    @allure.story("Response Validation")
    @allure.title("TC-API-011: Response time is acceptable")
    @allure.description("Verifies that API responds within acceptable performance threshold (< 1 second)")
    @allure.severity(allure.severity_level.NORMAL)
    @allure.tag("api", "performance", "sla")
    @allure.testcase("TC-API-011")
    @pytest.mark.positive
    def test_get_expense_response_time_acceptable(self, auth_session):
        """Test that API responds within acceptable time"""

        with allure.step("Arrange: Create test expense"):
            create_response = auth_session.post(
                f"{BASE_URL}/api/expenses",
                json={
                    "amount": 10.00,
                    "description": "Performance test",
                    "date": "2024-12-01"
                }
            )
            expense_id = create_response.json()["expense"]["id"]

        with allure.step(f"Act: Measure response time for GET request"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")
            response_time = response.elapsed.total_seconds()

            allure.attach(
                f"{response_time:.4f} seconds",
                name="Actual Response Time",
                attachment_type=allure.attachment_type.TEXT
            )

        with allure.step("Assert: Verify status code"):
            assert response.status_code == 200

        with allure.step("Assert: Verify response time is under 1 second"):
            threshold = 1.0
            assert response_time < threshold, f"Response time {response_time}s exceeds threshold {threshold}s"

            allure.attach(
                f"✓ Response time ({response_time:.4f}s) is within acceptable threshold ({threshold}s)",
                name="Performance Check",
                attachment_type=allure.attachment_type.TEXT
            )

    @allure.story("Response Validation")
    @allure.title("TC-API-012: Response has correct content type")
    @allure.description("Validates that API returns proper JSON content type header")
    @allure.severity(allure.severity_level.MINOR)
    @allure.tag("api", "headers", "content-type")
    @allure.testcase("TC-API-012")
    @pytest.mark.positive
    def test_get_expense_content_type_is_json(self, auth_session):
        """Test that response content type is JSON"""

        with allure.step("Arrange: Create test expense"):
            create_response = auth_session.post(
                f"{BASE_URL}/api/expenses",
                json={
                    "amount": 15.00,
                    "description": "Content type test",
                    "date": "2024-12-01"
                }
            )
            expense_id = create_response.json()["expense"]["id"]

        with allure.step(f"Act: Get expense and check headers"):
            response = auth_session.get(f"{BASE_URL}/api/expenses/{expense_id}")

            allure.attach(
                json.dumps(dict(response.headers), indent=2),
                name="Response Headers",
                attachment_type=allure.attachment_type.JSON
            )

        with allure.step("Assert: Verify status code"):
            assert response.status_code == 200

        with allure.step("Assert: Verify Content-Type header"):
            content_type = response.headers.get("Content-Type", "")
            assert "application/json" in content_type

            allure.attach(
                f"Content-Type: {content_type}",
                name="Header Validation",
                attachment_type=allure.attachment_type.TEXT
            )


if __name__ == "__main__":
    pytest.main([__file__, "-v", "--alluredir=allure-results"])