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


#C135_01
def test_deleteexpense(loginfirst):
    #todo: this is fragile since it only maintains DB state if there are 13 test cases
    find_url = "http://localhost:5000/api/expenses/2"
    expense_url = "http://localhost:5000/api/expenses/14"
    post_url = "http://localhost:5000/api/expenses"
    data = requests.get(find_url, cookies = loginfirst).json()
    # print(data)
    #creating dummy data to delete
    response = requests.post(post_url, json=data["expense"], cookies=loginfirst)
    assert response.status_code == 201


    response = requests.delete(expense_url, cookies = loginfirst)
    assert response.status_code == 200
    assert response.json()['message'] in "Expense deleted successfully"
    #revert changes


#C135_02
def test_deletenonpending(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"
    post_url = "http://localhost:5000/api/expenses"
    # print(data)
    response = requests.delete(expense_url, cookies = loginfirst)
    assert response.status_code == 400


#C135_03
def test_deletenonexistent(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/9999999"
    # print(data)
    response = requests.delete(expense_url, cookies = loginfirst)
    assert response.status_code == 404
    assert response.json()['message'] in "Expense not found"

