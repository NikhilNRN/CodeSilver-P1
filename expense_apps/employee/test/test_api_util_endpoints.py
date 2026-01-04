import allure
import pytest
import requests


@allure.epic("Employee App")
@allure.feature("Utility API")
class TestAPIUtilEndpoints:
    """Test suite for API utility and health check endpoints"""

    # IMPORTANT: These tests require the Flask server to be running.

    @pytest.fixture
    def app_version(self):
        """Expected application version"""
        return '1.0.0'

    @pytest.fixture
    def base_url(self):
        """Base URL for the API"""
        return 'http://127.0.0.1:5000'

    @pytest.fixture
    def session(self):
        """Create and configure a requests session with proper headers"""
        with allure.step("Initialize HTTP session with JSON headers"):
            session = requests.Session()
            session.headers.update({
                'Accept': 'application/json',
                'Content-Type': 'application/json',
            })
            allure.attach(
                str(session.headers),
                "Session Headers",
                allure.attachment_type.TEXT
            )
        yield session
        with allure.step("Close HTTP session"):
            session.close()

    @allure.story("Health Check")
    @allure.severity(allure.severity_level.BLOCKER)
    @allure.title("Verify API health endpoint returns healthy status")
    @allure.description("""
        Test to verify that the /health endpoint is accessible and returns:
        - Status code 200
        - Status field with value 'healthy'
        - Message indicating the API is running
    """)
    @allure.testcase("UTIL_01")
    def test_api_health(self, session, base_url):
        endpoint = f'{base_url}/health'

        with allure.step(f"Send GET request to {endpoint}"):
            allure.attach(
                endpoint,
                "Health Check URL",
                allure.attachment_type.TEXT
            )
            response = session.get(endpoint)
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
            assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

        with allure.step("Parse JSON response"):
            response_data = response.json()
            allure.attach(
                str(response_data),
                "Parsed Response Data",
                allure.attachment_type.JSON
            )

        with allure.step("Verify status field is 'healthy'"):
            assert response_data['status'] == 'healthy', \
                f"Expected status 'healthy' but got '{response_data.get('status')}'"

        with allure.step("Verify message indicates API is running"):
            expected_message = 'Employee Expense Management API is running'
            actual_message = response_data['message']
            assert actual_message == expected_message, \
                f"Expected message '{expected_message}' but got '{actual_message}'"

    @allure.story("API Information")
    @allure.severity(allure.severity_level.CRITICAL)
    @allure.title("Verify API info endpoint returns correct service details")
    @allure.description("""
        Test to verify that the /api endpoint returns:
        - Status code 200
        - Correct service name
        - Correct version number
        - List of available endpoints with their paths
    """)
    @allure.testcase("UTIL_02")
    def test_api_info(self, session, base_url, app_version):
        endpoint = f'{base_url}/api'

        with allure.step(f"Send GET request to {endpoint}"):
            allure.attach(
                endpoint,
                "API Info URL",
                allure.attachment_type.TEXT
            )
            response = session.get(endpoint)
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
            assert response.status_code == 200, f"Expected 200 but got {response.status_code}"

        with allure.step("Parse JSON response"):
            response_data = response.json()
            allure.attach(
                str(response_data),
                "Parsed Response Data",
                allure.attachment_type.JSON
            )

        with allure.step("Verify service name"):
            expected_service = 'Employee Expense Management API'
            actual_service = response_data['service']
            allure.attach(
                actual_service,
                "Service Name",
                allure.attachment_type.TEXT
            )
            assert actual_service == expected_service, \
                f"Expected service '{expected_service}' but got '{actual_service}'"

        with allure.step(f"Verify API version is {app_version}"):
            actual_version = response_data['version']
            allure.attach(
                f"Expected: {app_version}\nActual: {actual_version}",
                "Version Comparison",
                allure.attachment_type.TEXT
            )
            assert actual_version == app_version, \
                f"Expected version '{app_version}' but got '{actual_version}'"

        with allure.step("Verify endpoints list is not empty"):
            endpoints = response_data['endpoints']
            allure.attach(
                str(endpoints),
                "Available Endpoints",
                allure.attachment_type.JSON
            )
            assert len(endpoints) > 0, "Endpoints list should not be empty"

        with allure.step("Verify authentication endpoint path"):
            assert endpoints['authentication'] == '/api/auth', \
                f"Expected '/api/auth' but got '{endpoints.get('authentication')}'"

        with allure.step("Verify expenses endpoint path"):
            assert endpoints['expenses'] == '/api/expenses', \
                f"Expected '/api/expenses' but got '{endpoints.get('expenses')}'"

        with allure.step("Verify health endpoint path"):
            assert endpoints['health'] == '/health', \
                f"Expected '/health' but got '{endpoints.get('health')}'"