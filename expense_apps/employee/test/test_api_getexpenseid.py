import pytest
import requests


@pytest.fixture()
def loginfirst():
    login_url = "http://localhost:5000/api/auth/login"
    payload = {
        "username": "employee1",
        "password": "password123"
    }
    response = requests.post(login_url, json=payload)

    # print(response, response.content, response.cookies['jwt_token'])
    yield response.cookies
    # assert response.status_code == 200

#test getting real expense
#C133_01
def test_validexpense(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"
    response = requests.get(expense_url, cookies = loginfirst)
    # print(response, response.content)
    # Assert that the actual status code is equal to the expected 200
    assert response.status_code == 200
    # assert True

#test getting from other user
#C133_02
def test_wronguser(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/7"
    response = requests.get(expense_url, cookies = loginfirst)
    # Assert that the status code is 404; should not be able to locate epxenses under othre users
    assert response.status_code == 404
    assert "Expense not found" in response.json()["error"]

    # assert True


#test getting non pending expense
#C133_03
def test_nonpending_valid(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"
    response = requests.get(expense_url, cookies = loginfirst)
    print(response, response.content)
    # Assert that the actual status code is equal to the expected 200
    assert response.status_code == 200
    # assert True


#test getting expense that doesnt exist
#C133_04
def test_nonexistent(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/999999"
    response = requests.get(expense_url, cookies = loginfirst)
    # Assert that the status code is 404; should not be able to locate epxenses under othre users
    assert response.status_code == 404
    assert "Expense not found" in response.json()["error"]

    # assert True


