from unittest.mock import MagicMock

def test_status_no_token(client):
    response = client.get("/api/auth/status")
    assert response.status_code == 200
    assert response.get_json() == {"authenticated": False}


def test_status_valid_token(client, patch_auth_service):
    # Mock a valid user for token
    user_mock = patch_auth_service.get_user_from_token.return_value = MagicMock()
    user_mock.id = 1
    user_mock.username = "testuser"
    user_mock.role = "Employee"

    # Set token cookie
    client.set_cookie(key="jwt_token", value="valid-jwt-token")
    response = client.get("/api/auth/status")

    assert response.status_code == 200
    data = response.get_json()
    assert data["authenticated"] is True
    assert data["user"]["username"] == "testuser"
    assert data["user"]["role"] == "Employee"


def test_status_invalid_token(client, patch_auth_service):
    patch_auth_service.get_user_from_token.return_value = None

    client.set_cookie(key="jwt_token", value="invalid-token")
    response = client.get("/api/auth/status")

    assert response.status_code == 200
    assert response.get_json() == {"authenticated": False}
