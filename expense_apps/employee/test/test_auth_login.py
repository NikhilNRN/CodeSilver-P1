import allure
from unittest.mock import MagicMock


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("User Login")
@allure.severity(allure.severity_level.BLOCKER)
@allure.title("Successful login with valid credentials")
@allure.description("""
    Test to verify that a user can successfully log in with valid credentials.
    Verifies:
    - Status code 200
    - Success message in response
    - User details returned correctly
    - JWT token set in cookies
""")
@allure.testcase("AUTH_01")
def test_login_success(client, patch_auth_service):
    with allure.step("Mock authentication service to return valid user"):
        user_mock = patch_auth_service.authenticate_user.return_value = MagicMock()
        user_mock.id = 1
        user_mock.username = "testuser"
        user_mock.role = "Employee"

        allure.attach(
            f"User ID: {user_mock.id}\nUsername: {user_mock.username}\nRole: {user_mock.role}",
            "Mocked User Data",
            allure.attachment_type.TEXT
        )

    with allure.step("Mock JWT token generation"):
        patch_auth_service.generate_jwt_token.return_value = "valid-jwt-token"
        allure.attach(
            "valid-jwt-token",
            "Generated JWT Token",
            allure.attachment_type.TEXT
        )

    with allure.step("Send POST request to /api/auth/login with valid credentials"):
        payload = {"username": "valid", "password": "valid"}
        allure.attach(
            str(payload),
            "Login Request Payload",
            allure.attachment_type.JSON
        )
        response = client.post("/api/auth/login", json=payload)
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            str(response.get_json()),
            "Response Body",
            allure.attachment_type.JSON
        )
        allure.attach(
            str(response.headers.get("Set-Cookie")),
            "Set-Cookie Header",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

    with allure.step("Parse and verify response data"):
        data = response.get_json()
        allure.attach(
            str(data),
            "Parsed Response Data",
            allure.attachment_type.JSON
        )

    with allure.step("Verify success message"):
        assert data["message"] == "Login successful", \
            f"Expected 'Login successful' but got '{data.get('message')}'"

    with allure.step("Verify username in response"):
        assert data["user"]["username"] == "testuser", \
            f"Expected username 'testuser' but got '{data['user'].get('username')}'"

    with allure.step("Verify JWT token is set in cookies"):
        cookie_header = response.headers.get("Set-Cookie")
        assert "jwt_token" in cookie_header, \
            f"JWT token not found in cookies: {cookie_header}"


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("User Login")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("Login with missing JSON data returns 400")
@allure.description("""
    Test to verify that attempting to login without JSON data returns a 400 error.
    Verifies:
    - Status code 400
    - Error message indicates JSON data is required
""")
@allure.testcase("AUTH_02")
def test_login_missing_json(client):
    with allure.step("Send POST request to /api/auth/login without JSON body"):
        allure.attach(
            "No JSON data sent",
            "Request Body",
            allure.attachment_type.TEXT
        )
        response = client.post("/api/auth/login", content_type="application/json")
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            str(response.get_json()),
            "Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify response status code is 400"):
        assert response.status_code == 400, f"Expected 400 but got {response.status_code}"

    with allure.step("Verify error message indicates JSON data required"):
        error_message = response.get_json()["error"]
        expected_error = "JSON data required"
        allure.attach(
            f"Expected: {expected_error}\nActual: {error_message}",
            "Error Message Comparison",
            allure.attachment_type.TEXT
        )
        assert error_message == expected_error, \
            f"Expected '{expected_error}' but got '{error_message}'"


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("User Login")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("Login with invalid credentials returns 401")
@allure.description("""
    Test to verify that attempting to login with invalid credentials returns a 401 error.
    Verifies:
    - Status code 401
    - Error message indicates invalid credentials
""")
@allure.testcase("AUTH_03")
def test_login_invalid_credentials(client, patch_auth_service):
    with allure.step("Mock authentication service to return None (invalid user)"):
        patch_auth_service.authenticate_user.return_value = None
        allure.attach(
            "Authentication returns None",
            "Mocked Authentication Result",
            allure.attachment_type.TEXT
        )

    with allure.step("Send POST request to /api/auth/login with invalid credentials"):
        payload = {"username": "bad", "password": "bad"}
        allure.attach(
            str(payload),
            "Login Request Payload",
            allure.attachment_type.JSON
        )
        response = client.post("/api/auth/login", json=payload)
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            str(response.get_json()),
            "Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify response status code is 401"):
        assert response.status_code == 401, f"Expected 401 but got {response.status_code}"

    with allure.step("Verify error message indicates invalid credentials"):
        error_message = response.get_json()["error"]
        expected_error = "Invalid credentials"
        allure.attach(
            f"Expected: {expected_error}\nActual: {error_message}",
            "Error Message Comparison",
            allure.attachment_type.TEXT
        )
        assert error_message == expected_error, \
            f"Expected '{expected_error}' but got '{error_message}'"