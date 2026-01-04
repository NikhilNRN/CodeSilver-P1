import allure
from unittest.mock import MagicMock


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Status Check")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("Check authentication status without token returns unauthenticated")
@allure.description("""
    Test to verify that checking authentication status without a JWT token 
    returns an unauthenticated response.
    Verifies:
    - Status code 200
    - authenticated field is False
""")
@allure.testcase("AUTH_05")
def test_status_no_token(client):
    with allure.step("Send GET request to /api/auth/status without token"):
        allure.attach(
            "No JWT token provided",
            "Request Context",
            allure.attachment_type.TEXT
        )
        response = client.get("/api/auth/status")
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

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

    with allure.step("Verify authenticated field is False"):
        response_data = response.get_json()
        expected_response = {"authenticated": False}
        allure.attach(
            f"Expected: {expected_response}\nActual: {response_data}",
            "Response Comparison",
            allure.attachment_type.TEXT
        )
        assert response_data == expected_response, \
            f"Expected {expected_response} but got {response_data}"


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Status Check")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("Check authentication status with valid token returns user details")
@allure.description("""
    Test to verify that checking authentication status with a valid JWT token 
    returns authenticated status and user details.
    Verifies:
    - Status code 200
    - authenticated field is True
    - User details (username and role) are returned correctly
""")
@allure.testcase("AUTH_06")
def test_status_valid_token(client, patch_auth_service):
    with allure.step("Mock authentication service to return valid user from token"):
        user_mock = patch_auth_service.get_user_from_token.return_value = MagicMock()
        user_mock.id = 1
        user_mock.username = "testuser"
        user_mock.role = "Employee"

        allure.attach(
            f"User ID: {user_mock.id}\nUsername: {user_mock.username}\nRole: {user_mock.role}",
            "Mocked User Data",
            allure.attachment_type.TEXT
        )

    with allure.step("Set valid JWT token in cookies"):
        token_value = "valid-jwt-token"
        client.set_cookie(key="jwt_token", value=token_value)
        allure.attach(
            f"Cookie: jwt_token={token_value}",
            "Request Cookies",
            allure.attachment_type.TEXT
        )

    with allure.step("Send GET request to /api/auth/status with token"):
        response = client.get("/api/auth/status")
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

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

    with allure.step("Parse response data"):
        data = response.get_json()
        allure.attach(
            str(data),
            "Parsed Response Data",
            allure.attachment_type.JSON
        )

    with allure.step("Verify authenticated field is True"):
        assert data["authenticated"] is True, \
            f"Expected authenticated=True but got {data.get('authenticated')}"

    with allure.step("Verify username is 'testuser'"):
        username = data["user"]["username"]
        expected_username = "testuser"
        allure.attach(
            f"Expected: {expected_username}\nActual: {username}",
            "Username Verification",
            allure.attachment_type.TEXT
        )
        assert username == expected_username, \
            f"Expected username '{expected_username}' but got '{username}'"

    with allure.step("Verify user role is 'Employee'"):
        role = data["user"]["role"]
        expected_role = "Employee"
        allure.attach(
            f"Expected: {expected_role}\nActual: {role}",
            "Role Verification",
            allure.attachment_type.TEXT
        )
        assert role == expected_role, \
            f"Expected role '{expected_role}' but got '{role}'"


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Status Check")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("Check authentication status with invalid token returns unauthenticated")
@allure.description("""
    Test to verify that checking authentication status with an invalid JWT token 
    returns an unauthenticated response.
    Verifies:
    - Status code 200
    - authenticated field is False
""")
@allure.testcase("AUTH_07")
def test_status_invalid_token(client, patch_auth_service):
    with allure.step("Mock authentication service to return None (invalid token)"):
        patch_auth_service.get_user_from_token.return_value = None
        allure.attach(
            "get_user_from_token returns None",
            "Mocked Authentication Result",
            allure.attachment_type.TEXT
        )

    with allure.step("Set invalid JWT token in cookies"):
        token_value = "invalid-token"
        client.set_cookie(key="jwt_token", value=token_value)
        allure.attach(
            f"Cookie: jwt_token={token_value}",
            "Request Cookies",
            allure.attachment_type.TEXT
        )

    with allure.step("Send GET request to /api/auth/status with invalid token"):
        response = client.get("/api/auth/status")
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

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

    with allure.step("Verify authenticated field is False"):
        response_data = response.get_json()
        expected_response = {"authenticated": False}
        allure.attach(
            f"Expected: {expected_response}\nActual: {response_data}",
            "Response Comparison",
            allure.attachment_type.TEXT
        )
        assert response_data == expected_response, \
            f"Expected {expected_response} but got {response_data}"