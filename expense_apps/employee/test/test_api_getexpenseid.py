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
@allure.title("C133_01: Retrieve valid expense successfully")
@allure.description("Test to verify that a valid expense can be retrieved successfully by its ID")
@allure.testcase("C133_01")
def test_validexpense(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"

    with allure.step("Send GET request to retrieve expense with ID 2"):
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

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200


@allure.feature("Expense Retrieval")
@allure.story("Get Expense by ID")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C133_02: Attempt to retrieve expense from different user returns 404")
@allure.description("Test to verify that attempting to access another user's expense returns 404 error")
@allure.testcase("C133_02")
def test_wronguser(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/7"

    with allure.step("Attempt to retrieve expense from different user (ID 7)"):
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

    with allure.step("Verify response status code is 404"):
        assert response.status_code == 404

    with allure.step("Verify error message contains 'Expense not found'"):
        assert "Expense not found" in response.json()["error"]


@allure.feature("Expense Retrieval")
@allure.story("Get Expense by ID")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C133_03: Retrieve non-pending expense successfully")
@allure.description("Test to verify that a non-pending expense can be retrieved successfully")
@allure.testcase("C133_03")
def test_nonpending_valid(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"

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

    with allure.step("Verify response status code is 200"):
        assert response.status_code == 200


@allure.feature("Expense Retrieval")
@allure.story("Get Expense by ID")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C133_04: Attempt to retrieve non-existent expense returns 404")
@allure.description("Test to verify that attempting to retrieve a non-existent expense returns 404 error")
@allure.testcase("C133_04")
def test_nonexistent(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/999999"

    with allure.step("Attempt to retrieve non-existent expense (ID 999999)"):
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

    with allure.step("Verify response status code is 404"):
        assert response.status_code == 404

    with allure.step("Verify error message contains 'Expense not found'"):
        assert "Expense not found" in response.json()["error"]