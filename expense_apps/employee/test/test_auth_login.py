from unittest.mock import MagicMock

def test_login_success(client, patch_auth_service):
    # Mock valid user
    user_mock = patch_auth_service.authenticate_user.return_value = MagicMock()
    user_mock.id = 1
    user_mock.username = "testuser"
    user_mock.role = "Employee"

    patch_auth_service.generate_jwt_token.return_value = "valid-jwt-token"

    response = client.post("/api/auth/login", json={"username": "valid", "password": "valid"})
    assert response.status_code == 200
    data = response.get_json()
    assert data["message"] == "Login successful"
    assert data["user"]["username"] == "testuser"
    assert "jwt_token" in response.headers.get("Set-Cookie")


def test_login_missing_json(client):
    response = client.post("/api/auth/login", content_type="application/json")
    assert response.status_code == 400
    assert response.get_json()["error"] == "JSON data required"


def test_login_invalid_credentials(client, patch_auth_service):
    patch_auth_service.authenticate_user.return_value = None

    response = client.post("/api/auth/login", json={"username": "bad", "password": "bad"})
    assert response.status_code == 401
    assert response.get_json()["error"] == "Invalid credentials"
