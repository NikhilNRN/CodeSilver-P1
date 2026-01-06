import pytest
import requests
import allure


@pytest.fixture()
def loginfirst():
    """Fixture to authenticate and provide session cookies"""
    login_url = "http://localhost:5000/api/auth/login"
    payload = {"username": "employee1", "password": "password123"}

    with allure.step("Login to get authentication cookies"):
        response = requests.post(login_url, json=payload)
        allure.attach(str(payload), "Login Payload", allure.attachment_type.JSON)
        allure.attach(str(response.status_code), "Login Status Code", allure.attachment_type.TEXT)
        try:
            allure.attach(str(response.json()), "Login Response Body", allure.attachment_type.JSON)
        except Exception:
            allure.attach(response.text, "Login Response Text", allure.attachment_type.TEXT)

    yield response.cookies


def _get_msg_or_error(resp_json: dict) -> str:
    return (resp_json.get("message") or resp_json.get("error") or resp_json.get("detail") or "").strip()


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("C135_01: Delete a pending expense successfully")
@allure.description("Creates an expense, then deletes THAT created expense ID (no hardcoded IDs).")
def test_deleteexpense(loginfirst):
    find_url = "http://localhost:5000/api/expenses/2"
    post_url = "http://localhost:5000/api/expenses"

    with allure.step("Fetch expense data to duplicate"):
        data = requests.get(find_url, cookies=loginfirst).json()
        allure.attach(str(data), "Fetched Expense Data", allure.attachment_type.JSON)

    with allure.step("Create dummy expense for deletion"):
        create_resp = requests.post(post_url, json=data["expense"], cookies=loginfirst)
        allure.attach(str(create_resp.status_code), "Create Status", allure.attachment_type.TEXT)
        allure.attach(create_resp.text, "Create Body", allure.attachment_type.TEXT)
        assert create_resp.status_code in (200, 201), f"Expected 200/201 but got {create_resp.status_code}"

        created_json = create_resp.json()
        # tolerate both shapes: {"expense": {"id": ...}} or {"id": ...}
        created_id = None
        if isinstance(created_json, dict):
            if "expense" in created_json and isinstance(created_json["expense"], dict):
                created_id = created_json["expense"].get("id")
            created_id = created_id or created_json.get("id")

        assert created_id is not None, f"Could not determine created expense id from: {created_json}"

    delete_url = f"http://localhost:5000/api/expenses/{created_id}"

    with allure.step(f"Delete the created expense (id={created_id})"):
        delete_resp = requests.delete(delete_url, cookies=loginfirst)
        allure.attach(str(delete_resp.status_code), "Delete Status", allure.attachment_type.TEXT)
        allure.attach(delete_resp.text, "Delete Body", allure.attachment_type.TEXT)

        # Some APIs return 204 with empty body; accept 200/204 as success
        assert delete_resp.status_code in (200, 204), f"Expected 200/204 but got {delete_resp.status_code}"

        if delete_resp.status_code == 200:
            body = delete_resp.json()
            msg = _get_msg_or_error(body)
            # accept either key/message convention
            assert "deleted" in msg.lower(), f"Expected delete confirmation, got: {body}"


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C135_02: Delete non-pending expense returns error")
@allure.description("Deleting a reviewed expense should return 400 (or 409).")
def test_deletenonpending(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"

    with allure.step("Attempt to delete non-pending expense"):
        response = requests.delete(expense_url, cookies=loginfirst)
        allure.attach(str(response.status_code), "Response Status Code", allure.attachment_type.TEXT)
        allure.attach(response.text, "Response Body", allure.attachment_type.TEXT)

        assert response.status_code in (400, 409), f"Expected 400/409 but got {response.status_code}"


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C135_03: Delete non-existent expense returns 404")
@allure.description("Deleting a missing expense should return 404 with either message/error key.")
def test_deletenonexistent(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/9999999"

    with allure.step("Attempt to delete non-existent expense"):
        response = requests.delete(expense_url, cookies=loginfirst)
        allure.attach(str(response.status_code), "Response Status Code", allure.attachment_type.TEXT)
        allure.attach(response.text, "Response Body", allure.attachment_type.TEXT)

        assert response.status_code == 404, f"Expected 404 but got {response.status_code}"

        try:
            body = response.json()
        except Exception:
            body = {}

        msg = _get_msg_or_error(body)
        # tolerate whatever wording your API uses
        assert msg == "" or "not found" in msg.lower(), f"Expected not found message, got: {body}"
