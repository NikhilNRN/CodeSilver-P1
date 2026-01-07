import pytest
import requests
import allure


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

@allure.feature("Expense Retrieval")
@allure.story("Get Expense by ID")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("C133: Retrieve expenses successfully")
@allure.description("Test to verify that expenses are appropriately retrieved with correct return codes and error messages")
@allure.testcase("C133")
@pytest.mark.parametrize("expense_id, rcode, message", [
    ("2", 200, ""),
    ("13", 200, ""),
    ("7", 404, "Expense not found"),
    ("2000", 404, "Expense not found")
])
def test_nonpending(loginfirst, expense_id, rcode, message):
    expense_url = "http://localhost:5000/api/expenses/" + expense_id

    with allure.step("Send GET request to retrieve non-pending expense (ID 13)"):
        response = requests.get(expense_url, cookies=loginfirst)
        allure.attach(
            expense_url,
            "Request URL",
            allure.attachment_type.TEXT
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

    with allure.step("Verify response status code is appropriate"):
        assert response.status_code == rcode

    with allure.step("Verify error message contains 'Expense not found'"):
        if "error" in response.json():
            assert message in response.json()["error"]
