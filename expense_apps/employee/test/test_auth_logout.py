def test_logout_success(client):
    response = client.post("/api/auth/logout")

    assert response.status_code == 200
    assert response.get_json()["message"] == "Logout successful"

    # JWT cookie should be cleared
    cookie_header = response.headers.get("Set-Cookie")
    # Ensure the cookie key is present
    assert "jwt_token=" in cookie_header
    # Ensure it is set to expire in the past
    assert "Expires=Thu, 01 Jan 1970" in cookie_header or "expires=Thu, 01 Jan 1970" in cookie_header
    # Optional: check HttpOnly, Path, and SameSite
    assert "HttpOnly" in cookie_header
    assert "Path=/" in cookie_header
    assert "SameSite=Lax" in cookie_header
