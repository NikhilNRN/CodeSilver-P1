import allure

@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("User Logout")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("Successful logout clears JWT token and sets proper cookie attributes")
@allure.description("""
    Test to verify that a user can successfully log out.
    Verifies:
    - Status code 200
    - Success message in response
    - JWT token cookie is cleared (set to empty)
    - Cookie expiration is set to past date (Thu, 01 Jan 1970)
    - Cookie security attributes are properly set (HttpOnly, Path, SameSite)
""")
@allure.testcase("AUTH_04")
def test_logout_success(client):
    with allure.step("Send POST request to /api/auth/logout"):
        response = client.post("/api/auth/logout")
        allure.attach(
            "/api/auth/logout",
            "Logout Endpoint",
            allure.attachment_type.TEXT
        )
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

    with allure.step("Verify logout success message"):
        message = response.get_json()["message"]
        expected_message = "Logout successful"
        allure.attach(
            f"Expected: {expected_message}\nActual: {message}",
            "Message Comparison",
            allure.attachment_type.TEXT
        )
        assert message == expected_message, \
            f"Expected '{expected_message}' but got '{message}'"

    with allure.step("Extract Set-Cookie header"):
        cookie_header = response.headers.get("Set-Cookie")
        allure.attach(
            str(cookie_header),
            "Set-Cookie Header",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify JWT token cookie is present in header"):
        assert "jwt_token=" in cookie_header, \
            f"jwt_token not found in cookie header: {cookie_header}"
        allure.attach(
            "✓ jwt_token= found in cookie header",
            "Cookie Key Verification",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify cookie expiration is set to past date (Thu, 01 Jan 1970)"):
        expires_check = "Expires=Thu, 01 Jan 1970" in cookie_header or \
                        "expires=Thu, 01 Jan 1970" in cookie_header
        allure.attach(
            f"Cookie Header: {cookie_header}\nExpires Check: {expires_check}",
            "Expiration Verification",
            allure.attachment_type.TEXT
        )
        assert expires_check, \
            f"Expected expiration 'Thu, 01 Jan 1970' not found in: {cookie_header}"

    with allure.step("Verify HttpOnly attribute is set"):
        assert "HttpOnly" in cookie_header, \
            f"HttpOnly attribute not found in cookie header: {cookie_header}"
        allure.attach(
            "✓ HttpOnly attribute present",
            "HttpOnly Verification",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify Path attribute is set to '/'"):
        assert "Path=/" in cookie_header, \
            f"Path=/ not found in cookie header: {cookie_header}"
        allure.attach(
            "✓ Path=/ attribute present",
            "Path Verification",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify SameSite attribute is set to 'Lax'"):
        assert "SameSite=Lax" in cookie_header, \
            f"SameSite=Lax not found in cookie header: {cookie_header}"
        allure.attach(
            "✓ SameSite=Lax attribute present",
            "SameSite Verification",
            allure.attachment_type.TEXT
        )

    with allure.step("Summary: All cookie security attributes verified"):
        summary = """
        ✓ JWT token cookie present
        ✓ Cookie expired (set to Thu, 01 Jan 1970)
        ✓ HttpOnly flag set
        ✓ Path set to /
        ✓ SameSite set to Lax
        """
        allure.attach(
            summary,
            "Cookie Security Summary",
            allure.attachment_type.TEXT
        )