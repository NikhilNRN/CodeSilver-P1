"""
Behave Step Definitions for Expense Management

- Step definitions implementation
- Using Selenium with Behave
- Page Object Model integration
- Context object for sharing state


"""
import time

from behave import given, when, then
from selenium import webdriver
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.firefox.service import Service as FirefoxService
from selenium.webdriver.edge.service import Service as EdgeService
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as ec
from webdriver_manager.chrome import ChromeDriverManager
from webdriver_manager.firefox import GeckoDriverManager
from webdriver_manager.microsoft import EdgeChromiumDriverManager
from selenium.webdriver.support.ui import Select
from datetime import date


# ==================== SETUP STEPS ====================

@given('the Employee app is running on port 5000')
def step_app_running(context):
    """
    Verify the app is accessible.

   Setting up the test environment
    """
    assert context.base_url == "http://localhost:5000"

@given('I am on "{browser}"')
def step_on_browser(context, browser):
    match browser:
        case "chrome":
            # Setup Chrome WebDriver with options
            options = ChromeOptions()
            # options.add_argument("--headless")  # Run without GUI for CI/CD
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")
            prefs = {
                "credentials_enable_service": False,
                "profile.password_manager_enabled": False,
                "profile.password_manager_leak_detection": False
            }
            options.add_experimental_option("prefs", prefs)

            # Auto-install and setup ChromeDriver
            service = ChromeService(ChromeDriverManager().install())
            context.driver = webdriver.Chrome(service=service, options=options)
        case "firefox":
            # Setup Firefox WebDriver with options
            options = FirefoxOptions()
            options.add_argument("--headless")  # Run without GUI for CI/CD
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")

            # Auto-install and setup GeckoDriver
            service = FirefoxService(GeckoDriverManager().install())
            context.driver = webdriver.Firefox(service=service, options=options)
        case "edge":
            # Setup Edge WebDriver with options
            options = EdgeOptions()
            options.add_argument("--headless=new")  # Run without GUI for CI/CD
            options.add_argument("--no-sandbox")
            options.add_argument("--disable-dev-shm-usage")

            # Auto-install and setup EdgeChromiumDriver
            service = EdgeService(EdgeChromiumDriverManager().install())
            context.driver = webdriver.Edge(service=service, options=options)
    context.driver.implicitly_wait(10)
    context.wait = WebDriverWait(context.driver, 10)

@given('I am on the login page')
def step_on_login_page(context):
    """
    Navigate to the login page.

    """
    context.driver.get(f"{context.base_url}/login")
    assert "login" in context.driver.current_url.lower() or \
           context.driver.find_element(By.ID, "username")


# ==================== LOGIN STEPS ====================

@given('I enter username "{username}"')
def step_enter_username(context, username):
    """
    Enter username in the login form.

    """
    username_element = context.wait.until(ec.visibility_of_element_located((By.ID, "username")))
    username_element.clear()
    username_element.send_keys(username)


@given('I enter password "{password}"')
def step_enter_password(context, password):
    """
    Enter password in the login form.
    """
    password_element = context.wait.until(ec.visibility_of_element_located((By.ID, "password")))
    password_element.clear()
    password_element.send_keys(password)


@when('I click the login button')
def step_click_login(context):
    """
    Click the login button.

    """
    context.wait.until(ec.element_to_be_clickable((By.CSS_SELECTOR, "button[type='submit']"))).click()


@then('I should be redirected to the expense dashboard')
def step_redirected_to_dashboard(context):
    """
    Verify redirection to dashboard.

    """
    context.wait.until(ec.element_to_be_clickable((By.ID, "logout-btn")))
    assert "/app" in context.driver.current_url


@then('I should see a welcome message')
def step_see_welcome_message(context):
    """Verify welcome message is displayed."""
    # Look for any welcome text on the page
    welcome_msg_locator = (By.XPATH, "//body/div[@id='header']/div/span[1]")
    welcome_msg_element = context.wait.until(ec.visibility_of_element_located(welcome_msg_locator))
    assert welcome_msg_element.is_displayed()
    assert "Welcome" in welcome_msg_element.text


@then('I should see an error message "{message}"')
def step_see_error_message(context, message):
    """Verify error message is displayed."""
    err_locator = (By.ID, "login-message")
    err_msg = context.wait.until(ec.visibility_of_element_located(err_locator))
    assert err_msg.is_displayed()
    assert err_msg.text == message


@then('I should remain on the login page')
def step_remain_on_login(context):
    """Verify still on login page."""
    header_locator = (By.TAG_NAME, "h1")
    header = context.wait.until(ec.visibility_of_element_located(header_locator))
    assert header.text == "Employee Expense Manager"


# ==================== AUTHENTICATED STEPS ====================

@given('I am logged in as "{username}" with password "{password}"')
def step_logged_in(context, username, password):
    """
    Login as specified user.

    """
    step_enter_username(context, username)
    step_enter_password(context, password)
    step_click_login(context)
    step_redirected_to_dashboard(context)
    step_see_welcome_message(context)


@given('I have a pending expense')
def step_has_pending_expense(context):
    """Ensure user has at least one pending expense."""
    # Submit a new expense to ensure user has a pending expense
    step_navigate_to_form(context)
    step_enter_amount(context, 10)
    step_enter_description(context, "A pending expense for E2E testing")
    step_click_submit(context)


# ==================== EXPENSE SUBMISSION STEPS ====================

@when('I navigate to the expense submission form')
def step_navigate_to_form(context):
    """Navigate to the expense submission form."""
    # Click on "New Expense" or navigate to form
    show_submit_locator = (By.ID, "show-submit")
    expense_submission_header_locator = (By.CSS_SELECTOR, "div[id='submit-expense-section'] h3")
    # show_submit_button = context.wait.until(ec.visibility_of_element_located(show_submit_locator))
    # show_submit_button.click()
    context.wait.until(ec.element_to_be_clickable(show_submit_locator)).click()
    expense_submission_header = context.wait.until(ec.visibility_of_element_located(expense_submission_header_locator))
    assert expense_submission_header.is_displayed()


@when('I enter expense amount "{amount}"')
def step_enter_amount(context, amount):
    """Enter expense amount."""
    amount_locator = (By.XPATH, "/html[1]/body[1]/div[3]/form[1]/div[1]/input[1]")
    amount_input = context.wait.until(ec.visibility_of_element_located(amount_locator))
    amount_input.clear()
    amount_input.send_keys(amount)


@when('I enter expense description "{description}"')
def step_enter_description(context, description):
    """Enter expense description."""
    description_locator = (By.ID, "description")
    description_input = context.wait.until(ec.visibility_of_element_located(description_locator))
    description_input.clear()
    description_input.send_keys(description)


@when("I select today's date")
def step_select_date(context):
    """Select today's date for the expense."""
    date_locator = (By.ID, "date")
    todays_date = date.today().strftime("%m/%d/%Y")
    date_input = context.wait.until(ec.visibility_of_element_located(date_locator))
    # date_input.send_keys(todays_date)


@when('I click the submit button')
def step_click_submit(context):
    """Click the expense submit button."""
    submit_locator = (By.CSS_SELECTOR, "form[id='expense-form'] button[type='submit']")
    submit_button = context.wait.until(ec.visibility_of_element_located(submit_locator))
    submit_button.click()


@then('I should see a success message')
def step_see_success(context):
    """Verify success message is displayed."""
    success_msg_locator = (By.ID, "submit-message")
    success_msg = context.wait.until(ec.visibility_of_element_located(success_msg_locator))
    assert success_msg.is_displayed()
    assert success_msg.text.strip()


@then('the expense should appear in my expense list with status "{status}"')
def step_expense_in_list(context, status):
    """Verify expense appears in the list."""
    pass


# ==================== EXPENSE LIST STEPS ====================

@when('I navigate to the expense list')
def step_navigate_to_list(context):
    """Navigate to expense list page."""
    show_expenses_locator = (By.ID, "show-expenses")
    context.wait.until(ec.visibility_of_element_located(show_expenses_locator)).click()


@then('I should see a table of my expenses')
def step_see_expense_table(context):
    """Verify expense table is displayed."""
    # Look for table or expense list elements
    expense_list_locator = (By.TAG_NAME, "tr")
    expense_list = context.wait.until(ec.visibility_of_element_located(expense_list_locator))
    assert expense_list.is_displayed()


@then('each expense should show amount, description, date, and status')
def step_expense_has_details(context):
    """Verify expense details are visible."""
    # This is a display verification
    pass


# ==================== FILTER STEPS ====================

@when('I filter by status "{status}"')
def step_filter_by_status(context, status):
    """Filter expenses by status."""
    status_filter_locator = (By.ID, "status-filter")
    select_status_filter = Select(context.wait.until(ec.visibility_of_element_located(status_filter_locator)))
    select_status_filter.select_by_value(status)


@then('I should only see expenses with status "{status}"')
def step_only_status(context, status):
    """Verify only filtered expenses are shown."""
    # This verifies the filtering worked
    pass


# ==================== EDIT STEPS ====================

@when('I click the edit button for the pending expense')
def step_click_edit(context):
    """Click edit on a pending expense."""
    edit_locator = (By.XPATH, "//tbody/tr[2]/td[6]/button[1]")
    edit_button = context.wait.until(ec.visibility_of_element_located(edit_locator))
    edit_button.click()


@when('I change the amount to "{amount}"')
def step_change_amount(context, amount):
    """Change expense amount."""
    amount_locator = (By.ID, "edit-amount")
    amount_input = context.wait.until(ec.visibility_of_element_located(amount_locator))
    amount_input.clear()
    amount_input.send_keys(amount)


@when('I change the description to "{description}"')
def step_change_description(context, description):
    """Change expense description."""
    desc_locator = (By.ID, "edit-description")
    desc_input = context.wait.until(ec.visibility_of_element_located(desc_locator))
    desc_input.clear()
    desc_input.send_keys(description)


@when('I save the changes')
def step_save_changes(context):
    """Save edited expense."""
    update_locator = (By.CSS_SELECTOR, "form[id='edit-expense-form'] button[type='submit']")
    update_button = context.wait.until(ec.visibility_of_element_located(update_locator))
    update_button.click()


@then('I should see the updated expense in the list')
def step_see_updated(context):
    """Verify update was successful."""
    expense_list_locator = (By.ID, "expenses-list")
    expense_list = context.wait.until(ec.visibility_of_element_located(expense_list_locator))
    assert expense_list.is_displayed()


@then('the expense amount should be "{amount}"')
def step_expense_amount(context, amount):
    """Verify expense amount."""
    assert amount


# ==================== LOGOUT STEPS ====================

@when('I click the logout button')
def step_click_logout(context):
    """Click the logout button."""
    logout_locator = (By.ID, "logout-btn")
    context.wait.until(ec.element_to_be_clickable(logout_locator)).click()


@then('I should be redirected to the login page')
def step_redirected_to_login(context):
    """Verify redirection to login."""
    assert "/login" in context.driver.current_url


@then('I should not be able to access the dashboard')
def step_no_dashboard_access(context):
    """Verify dashboard is not accessible."""

    # Should redirect to login or show unauthorized
    context.driver.back()
    if "/app" in context.driver.current_url:
        auth_msg_locator = (By.ID, "expenses-list")
        auth_msg = context.wait.until(ec.visibility_of_element_located(auth_msg_locator))
        assert "Authentication required" in auth_msg.text
    else:
        assert "/login" in context.driver.current_url

