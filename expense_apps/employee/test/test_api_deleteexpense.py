import pytest
import requests
import allure


@pytest.fixture()
def loginfirst():
    """Fixture to authenticate and provide session cookies"""
    login_url = "http://localhost:5000/api/auth/login"
    payload = {
        "username": "employee1",
        "password": "password123"
    }
    with allure.step("Login to get authentication token"):
        response = requests.post(login_url, json=payload)
        allure.attach(str(response.status_code), "Login Status Code", allure.attachment_type.TEXT)

    yield response.cookies


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.CRITICAL)
@allure.title("C135_01: Delete a pending expense successfully")
@allure.description("Test to verify that a pending expense can be deleted successfully")
def test_deleteexpense(loginfirst):
    find_url = "http://localhost:5000/api/expenses/2"
    expense_url = "http://localhost:5000/api/expenses/14"
    post_url = "http://localhost:5000/api/expenses"

    with allure.step("Fetch expense data to duplicate"):
        data = requests.get(find_url, cookies=loginfirst).json()
        allure.attach(str(data), "Fetched Expense Data", allure.attachment_type.JSON)

    with allure.step("Create dummy expense for deletion"):
        response = requests.post(post_url, json=data["expense"], cookies=loginfirst)
        assert response.status_code == 201
        allure.attach(str(response.status_code), "Create Response Code", allure.attachment_type.TEXT)

    with allure.step("Delete the expense"):
        response = requests.delete(expense_url, cookies=loginfirst)
        allure.attach(str(response.json()), "Delete Response", allure.attachment_type.JSON)
        assert response.status_code == 200
        assert response.json()['message'] in "Expense deleted successfully"


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C135_02: Delete non-pending expense returns error")
@allure.description("Test to verify that deleting a non-pending expense returns a 400 error")
def test_deletenonpending(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"

    with allure.step("Attempt to delete non-pending expense"):
        response = requests.delete(expense_url, cookies=loginfirst)
        allure.attach(str(response.status_code), "Response Status Code", allure.attachment_type.TEXT)
        allure.attach(str(response.json()), "Response Body", allure.attachment_type.JSON)
        assert response.status_code == 400


@allure.feature("Expense Management")
@allure.story("Delete Expense")
@allure.severity(allure.severity_level.NORMAL)
@allure.title("C135_03: Delete non-existent expense returns 404")
@allure.description("Test to verify that deleting a non-existent expense returns a 404 error")
def test_deletenonexistent(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/9999999"

    with allure.step("Attempt to delete non-existent expense"):
        response = requests.delete(expense_url, cookies=loginfirst)
        allure.attach(str(response.status_code), "Response Status Code", allure.attachment_type.TEXT)
        allure.attach(str(response.json()), "Response Body", allure.attachment_type.JSON)
        assert response.status_code == 404
        assert response.json()['message'] in "Expense not found"