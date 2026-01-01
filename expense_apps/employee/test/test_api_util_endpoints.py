import allure
import pytest
import requests

@allure.epic("Employee App")
@allure.feature("Utility API")
class TestAPIUtilEndpoints:
    # IMPORTANT: These tests require the Flask server to be running.

    @pytest.fixture
    def app_version(self):
        return '1.0.0'

    @pytest.fixture
    def base_url(self):
        return 'http://127.0.0.1:5000'

    @pytest.fixture
    def session(self):
        session = requests.Session()
        session.headers.update({
            'Accept': 'application/json',
            'Content-Type': 'application/json',
        })
        yield session
        session.close()

    def test_api_health(self, session, base_url):
        response = session.get(f'{base_url}/health')
        assert response.status_code == 200
        response_data = response.json()
        assert response_data['status'] == 'healthy'
        assert response_data['message'] == 'Employee Expense Management API is running'

    def test_api_info(self, session, base_url, app_version):
        response = session.get(f'{base_url}/api')
        assert response.status_code == 200
        response_data = response.json()
        assert response_data['service'] == 'Employee Expense Management API'
        assert response_data['version'] == app_version
        assert len(response_data['endpoints']) > 0
        assert response_data['endpoints']['authentication'] == '/api/auth'
        assert response_data['endpoints']['expenses'] == '/api/expenses'
        assert response_data['endpoints']['health'] == '/health'