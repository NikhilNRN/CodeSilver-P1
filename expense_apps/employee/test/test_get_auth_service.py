import allure
import pytest
from flask import Flask
from api.auth import get_auth_service


class DummyAuthService:
    """Simple stand-in for AuthenticationService."""
    pass


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Service Retrieval")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("Get auth service from Flask app context returns correct service instance")
@allure.description("""
    Test to verify that get_auth_service() successfully retrieves the 
    auth_service instance attached to the current Flask application context.
    Validates:
    - Service is retrieved from Flask app context
    - Returned service is the same instance that was configured
""")
@allure.testcase("AUTH_SVC_01")
def test_returns_auth_service_from_flask_app_context():
    with allure.step("Arrange: Create Flask application instance"):
        app = Flask(__name__)
        allure.attach(
            f"Flask app created: {app.name}",
            "Flask Application",
            allure.attachment_type.TEXT
        )

    with allure.step("Configure dummy auth service on Flask app"):
        dummy_service = DummyAuthService()
        app.auth_service = dummy_service
        allure.attach(
            f"DummyAuthService instance attached to app\nInstance ID: {id(dummy_service)}",
            "Auth Service Configuration",
            allure.attachment_type.TEXT
        )

    with allure.step("Act: Call get_auth_service() within Flask app context"):
        with app.app_context():
            allure.attach(
                "Inside Flask application context",
                "Context Information",
                allure.attachment_type.TEXT
            )
            result = get_auth_service()
            allure.attach(
                f"Retrieved service instance ID: {id(result)}",
                "Retrieved Service",
                allure.attachment_type.TEXT
            )

    with allure.step("Assert: Verify returned service is the configured instance"):
        assert result is dummy_service, \
            f"Expected service instance {id(dummy_service)} but got {id(result)}"
        allure.attach(
            f"✓ Service instance matches\n"
            f"  Expected ID: {id(dummy_service)}\n"
            f"  Actual ID: {id(result)}",
            "Verification Result",
            allure.attachment_type.TEXT
        )


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Service Retrieval")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("Get auth service without app context raises RuntimeError")
@allure.description("""
    Test to verify that get_auth_service() raises RuntimeError when called 
    outside of a Flask application context.
    Validates:
    - RuntimeError is raised
    - Proper error handling for missing Flask context
""")
@allure.testcase("AUTH_SVC_02")
def test_raises_runtime_error_when_called_without_app_context():
    with allure.step("Arrange: Ensure no Flask app context is active"):
        allure.attach(
            "No Flask application context active",
            "Initial State",
            allure.attachment_type.TEXT
        )

    with allure.step("Act & Assert: Call get_auth_service() outside app context"):
        expected_error = "RuntimeError"
        allure.attach(
            f"Expected exception: {expected_error}",
            "Expected Exception",
            allure.attachment_type.TEXT
        )

        with pytest.raises(RuntimeError) as exc_info:
            get_auth_service()

        allure.attach(
            f"✓ RuntimeError raised\nException message: {str(exc_info.value)}",
            "Exception Verification",
            allure.attachment_type.TEXT
        )
        allure.attach(
            str(exc_info.typename),
            "Exception Type",
            allure.attachment_type.TEXT
        )


@allure.epic("Employee App")
@allure.feature("Authentication")
@allure.story("Authentication Service Retrieval")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("Get auth service from unconfigured app raises AttributeError")
@allure.description("""
    Test to verify that get_auth_service() raises AttributeError when the 
    Flask application does not have auth_service configured.
    Validates:
    - AttributeError is raised
    - Proper error handling for missing auth_service attribute
""")
@allure.testcase("AUTH_SVC_03")
def test_raises_attribute_error_when_auth_service_not_configured():
    with allure.step("Arrange: Create Flask app without auth_service configured"):
        app = Flask(__name__)
        allure.attach(
            f"Flask app created: {app.name}\nauth_service: NOT CONFIGURED",
            "Flask Application",
            allure.attachment_type.TEXT
        )

    with allure.step("Act & Assert: Call get_auth_service() with unconfigured app"):
        with app.app_context():
            allure.attach(
                "Inside Flask application context (but auth_service not configured)",
                "Context Information",
                allure.attachment_type.TEXT
            )

            expected_error = "AttributeError"
            allure.attach(
                f"Expected exception: {expected_error}",
                "Expected Exception",
                allure.attachment_type.TEXT
            )

            with pytest.raises(AttributeError) as exc_info:
                get_auth_service()

            allure.attach(
                f"✓ AttributeError raised\nException message: {str(exc_info.value)}",
                "Exception Verification",
                allure.attachment_type.TEXT
            )
            allure.attach(
                str(exc_info.typename),
                "Exception Type",
                allure.attachment_type.TEXT
            )