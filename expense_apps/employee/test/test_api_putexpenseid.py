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

#test updating expense
#C134_01
def test_updateexpense(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"
    data = requests.get(expense_url, cookies = loginfirst).json()
    # print(data)
    original = data["expense"]["amount"]
    newdat = original + 10
    body = {"amount":newdat, "description":data["expense"]["description"], "date":data["expense"]["date"]}
    response = requests.put(expense_url, json=body, cookies = loginfirst)
    # print(response, response.content)
    # Assert that the actual status code is equal to the expected 200
    assert response.status_code == 200
    assert response.json()["expense"]["amount"] == newdat

    #revert changes
    body = {"amount":original, "description":data["expense"]["description"], "date":data["expense"]["date"]}
    response = requests.put(expense_url, json=body, cookies = loginfirst)

    assert response.status_code == 200
    assert response.json()["expense"]["amount"] == original

#test malformed request
#C134_02
def test_malformedexpense(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"
    data = requests.get(expense_url, cookies = loginfirst).json()
    # print(data)
    original = data["expense"]["amount"]
    newdat = original + 10
    body = {"amount":newdat}
    response = requests.put(expense_url, json=body, cookies = loginfirst)
    # print(response, response.content)
    assert response.status_code == 400
    assert "Amount, description, and date are required" in response.json()["error"]

#test nojson
#C134_03
def test_nojson(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/2"
    # print(data)
    response = requests.put(expense_url, json ={}, cookies = loginfirst)
    # print(response, response.content)
    assert response.status_code == 400
    assert "JSON data required" in response.json()["error"]

#test on nonpending
#C134_04

def test_notpending(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"
    data = requests.get(expense_url, cookies = loginfirst).json()
    # print(data)
    original = data["expense"]["amount"]
    newdat = original + 10
    body = {"amount": newdat, "description": data["expense"]["description"], "date": data["expense"]["date"]}

    response = requests.put(expense_url, json=body, cookies = loginfirst)
    # print(response, response.content)
    assert response.status_code == 400
    assert "Cannot edit expense that has been reviewed" in response.json()["error"]


#C134_05
def test_doesnotexist(loginfirst):
    expense_url = "http://localhost:5000/api/expenses/13"
    false_url = "http://localhost:5000/api/expenses/9999999"
    data = requests.get(expense_url, cookies = loginfirst).json()
    # print(data)
    original = data["expense"]["amount"]
    body = {"amount": original, "description": data["expense"]["description"], "date": data["expense"]["date"]}

    response = requests.put(false_url, json=body, cookies = loginfirst)
    # print(response, response.content)
    assert response.status_code == 400
    assert "Cannot edit expense that has been reviewed" in response.json()["error"]