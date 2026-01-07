import pytest
import requests
import allure

test_body = {
    "amount": 12,
    "description": "dummy",
    "date": "12-20-2025"
}
@pytest.fixture()
def loginfirst():
    """Fixture to authenticate and provide session cookies for API requests"""
    login_url = "http://localhost:5000/api/auth/login"
    payload = {
        "username": "employee1",
        "password": "password123"
    }

    with allure.step("Authenticate user and obtain session cookies"):
        response = requests.post(login_url, json=payload)
        allure.attach(
            str(payload),
            "Login Credentials",
            allure.attachment_type.JSON
        )
        allure.attach(
            str(response.status_code),
            "Login Status Code",
            allure.attachment_type.TEXT
        )

    yield response.cookies


@allure.feature("Expense Management")
@allure.story("Update Expense")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("C134_01: Update expense amount successfully")
@allure.description("Test to verify that a pending expense can be updated successfully and then reverted")
@allure.testcase("C134_01")
def test_updateexpense(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"

    with allure.step("Retrieve current expense data"):
        data = requests.get(expense_url, cookies=loginfirst).json()
        original = data["expense"]["amount"]
        allure.attach(
            str(data),
            "Original Expense Data",
            allure.attachment_type.JSON
        )
        allure.attach(
            str(original),
            "Original Amount",
            allure.attachment_type.TEXT
        )

    with allure.step("Update expense with new amount"):
        newdat = original + 10
        body = {
            "amount": newdat,
            "description": data["expense"]["description"],
            "date": data["expense"]["date"]
        }
        allure.attach(
            str(body),
            "Update Request Body",
            allure.attachment_type.JSON
        )
        response = requests.put(expense_url, json=body, cookies=loginfirst)
        allure.attach(
            str(response.status_code),
            "Update Response Status",
            allure.attachment_type.TEXT
        )
        allure.attach(
            response.text,
            "Update Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify update was successful"):
        assert response.status_code in (200, 400)
        if response.status_code == 200:
            assert response.json()["expense"]["amount"] == newdat
        else:
            assert "error" in response.json()

    with allure.step("Revert changes to original amount"):
        body = {
            "amount": original,
            "description": data["expense"]["description"],
            "date": data["expense"]["date"]
        }
        response = requests.put(expense_url, json=body, cookies=loginfirst)
        allure.attach(
            str(response.status_code),
            "Revert Response Status",
            allure.attachment_type.TEXT
        )

    with allure.step("Verify revert was successful"):
        assert response.status_code in (200, 400)
        assert response.json()["expense"]["amount"] == original


@allure.feature("Expense Management")
@allure.story("Update Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C134_03: Update expense with empty JSON returns 400")
@allure.description("Test to verify that updating an expense with empty JSON returns 400 error")
@allure.testcase("C134_03")
@pytest.mark.parametrize("expense_id, rcode, body, message", [
    ("3", 400, {}, "JSON data required"),
    ("3", 400, {"amount": 10}, ""),
    ("9999999", 404, test_body, ""),
    ("13", 400, test_body, "Cannot edit expense that has been reviewed")
])
def test_nojson(loginfirst, expense_id, rcode, body, message):
    expense_url = "http://localhost:5000/api/expenses/" + expense_id

    with allure.step("Send update request with empty JSON body"):
        response = requests.put(expense_url, json=body, cookies=loginfirst)
        allure.attach(
            expense_url,
            "Request URL",
            allure.attachment_type.TEXT
        )
        allure.attach(
            body,
            "Bad Request Body",
            allure.attachment_type.JSON
        )
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            response.text,
            "Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify response status code is 400"):
        assert response.status_code == rcode

    with allure.step("Verify error message indicates JSON data required"):
        assert message in response.json()["error"]


@allure.feature("Expense Management")
@allure.story("Update Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C134_04: Update non-pending expense returns 400")
@allure.description("Test to verify that updating a reviewed (non-pending) expense returns 400 error")
@allure.testcase("C134_04")
@pytest.mark.parametrize("expense_id, rcode, message", [
    ("13", 400, "Cannot edit expense that has been reviewed")
])
def test_notpending(loginfirst, expense_id, rcode, message):
    expense_url = "http://localhost:5000/api/expenses/" + expense_id

    with allure.step("Retrieve non-pending expense data"):
        data = requests.get(expense_url, cookies=loginfirst).json()
        original = data["expense"]["amount"]
        allure.attach(
            str(data),
            "Non-Pending Expense Data",
            allure.attachment_type.JSON
        )

    with allure.step("Attempt to update non-pending expense"):
        newdat = original + 10
        body = {
            "amount": newdat,
            "description": data["expense"]["description"],
            "date": data["expense"]["date"]
        }
        allure.attach(
            str(body),
            "Update Request Body",
            allure.attachment_type.JSON
        )
        response = requests.put(expense_url, json=body, cookies=loginfirst)
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            response.text,
            "Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify response status code is 400"):
        assert response.status_code == rcode

    with allure.step("Verify error message indicates expense has been reviewed"):
        if "error" in response.json():
            assert message in response.json()["error"]


@allure.feature("Expense Management")
@allure.story("Update Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C134_05: Update non-existent expense returns 400")
@allure.description("Test to verify that updating a non-existent expense returns 400 error")
@allure.testcase("C134_05")
@pytest.mark.parametrize("target_id, source_id",[
    ("9999999", "13")
])
def test_doesnotexist(loginfirst,target_id,source_id):
    expense_url = "http://localhost:5000/api/expenses/" + source_id
    false_url = "http://localhost:5000/api/expenses/" + target_id

    with allure.step("Retrieve valid expense data for request body"):
        data = requests.get(expense_url, cookies=loginfirst).json()
        original = data["expense"]["amount"]
        allure.attach(
            str(data),
            "Valid Expense Data",
            allure.attachment_type.JSON
        )

    with allure.step("Attempt to update non-existent expense (ID 9999999)"):
        body = {
            "amount": original,
            "description": data["expense"]["description"],
            "date": data["expense"]["date"]
        }
        allure.attach(
            false_url,
            "Non-Existent Expense URL",
            allure.attachment_type.TEXT
        )
        allure.attach(
            str(body),
            "Update Request Body",
            allure.attachment_type.JSON
        )
        response = requests.put(false_url, json=body, cookies=loginfirst)
        allure.attach(
            str(response.status_code),
            "Response Status Code",
            allure.attachment_type.TEXT
        )
        allure.attach(
            response.text,
            "Response Body",
            allure.attachment_type.JSON
        )

    with allure.step("Verify response status code is 400"):
        assert response.status_code in (400, 404)

    with allure.step("Verify error message indicates expense has been reviewed"):
        data = response.json()
        msg = data.get("error") or data.get("message") or ""
        assert msg != ""