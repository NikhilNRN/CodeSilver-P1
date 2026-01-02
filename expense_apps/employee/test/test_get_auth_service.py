import pytest
from flask import Flask
from api.auth import get_auth_service


class DummyAuthService:
    """Simple stand-in for AuthenticationService."""
    pass


def test_returns_auth_service_from_flask_app_context():
    """
    Verify get_auth_service returns the auth_service attached
    to the current Flask application context.
    """
    app = Flask(__name__)
    dummy_service = DummyAuthService()
    app.auth_service = dummy_service

    with app.app_context():
        result = get_auth_service()

    assert result is dummy_service


def test_raises_runtime_error_when_called_without_app_context():
    """
    Verify get_auth_service raises RuntimeError when called
    outside a Flask application context.
    """
    with pytest.raises(RuntimeError):
        get_auth_service()


def test_raises_attribute_error_when_auth_service_not_configured():
    """
    Verify get_auth_service raises AttributeError when the
    Flask app does not have auth_service configured.
    """
    app = Flask(__name__)
    with app.app_context():
        with pytest.raises(AttributeError):
            get_auth_service()
