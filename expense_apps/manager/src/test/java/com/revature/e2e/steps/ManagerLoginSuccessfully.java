package com.revature.e2e.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ManagerLoginSuccessfully {

    private WebDriver driver;
    private WebDriverWait wait;

    @Before
    public void setUp() {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        driver = new FirefoxDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("the manager is on the login page")
    public void the_manager_is_on_the_login_page() {
        driver.get("http://localhost:5001/login.html");
        assertTrue(driver.getCurrentUrl().contains("login"));
    }

    @When("the manager enters a valid username")
    public void the_manager_enters_a_valid_username() {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement usernameInput = driver.findElement(By.id("username"));
        usernameInput.clear();
        usernameInput.sendKeys("manager1");
    }

    @When("the manager enters a valid password")
    public void the_manager_enters_a_valid_password() {
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.clear();
        passwordInput.sendKeys("password123");
    }

    @When("the manager enters invalid username {string}")
    public void the_manager_enters_invalid_username(String username) {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement usernameInput = driver.findElement(By.id("username"));
        usernameInput.clear();
        usernameInput.sendKeys(username);
    }

    @When("the manager enters invalid password {string}")
    public void the_manager_enters_invalid_password(String password) {
        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.clear();
        passwordInput.sendKeys(password);
    }

    @When("the manager clicks the login button")
    public void the_manager_clicks_the_login_button() {
        driver.findElement(By.xpath("//button[@type='submit']")).click();
    }

    @When("the manager clicks the logout button")
    public void the_manager_clicks_the_logout_button() {
        WebElement logoutButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@id='logout-btn']")
        ));
        logoutButton.click();

        // Wait for redirect to login page after logout
        wait.until(ExpectedConditions.urlContains("login.html"));
    }

    @When("the manager navigates to the manager dashboard directly")
    public void the_manager_navigates_to_the_manager_dashboard_directly() {
        driver.get("http://localhost:5001/manager.html");
    }

    @Then("the manager should be redirected to the manager dashboard")
    public void the_manager_should_be_redirected_to_the_manager_dashboard() {
        wait.until(ExpectedConditions.urlContains("manager.html"));
        assertTrue(driver.getCurrentUrl().contains("manager.html"));
    }

    @Then("the manager should be redirected to the login page")
    public void the_manager_should_be_redirected_to_the_login_page() {
        wait.until(ExpectedConditions.urlContains("login.html"));
        assertTrue(driver.getCurrentUrl().contains("login.html"));
    }

    @Then("the manager dashboard header should be displayed")
    public void the_manager_dashboard_header_should_be_displayed() {
        WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[normalize-space()='Manager Expense Dashboard']")
        ));
        String headerText = header.getText();
        assertTrue(headerText.contains("Manager Expense Dashboard"));
    }

    @Then("the pending expenses header should be displayed")
    public void the_pending_expenses_header_should_be_displayed() {
        WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h3[normalize-space()='Pending Expenses for Review']")
        ));
        String headerText = header.getText();
        assertTrue(headerText.contains("Pending Expenses for Review"));
    }

    @Then("the pending expense section should be displayed")
    public void the_pending_expense_section_should_be_displayed() {
        WebElement section = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//div[@id='pending-expenses-section']")
        ));
        assertTrue(section.isDisplayed());
    }

    @When("the manager clicks the show reports button")
    public void the_manager_clicks_the_show_reports_button() {
        WebElement showReportsButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@id='show-reports']")
        ));
        showReportsButton.click();
    }

    @When("the manager enters employee ID {string}")
    public void the_manager_enters_employee_id(String employeeId) {
        WebElement employeeIdInput = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//input[@id='employee-report-id']")
        ));
        employeeIdInput.clear();
        employeeIdInput.sendKeys(employeeId);
    }

    @When("the manager clicks the generate report button")
    public void the_manager_clicks_the_generate_report_button() {
        WebElement generateButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.xpath("//button[@id='generate-employee-report']")
        ));
        generateButton.click();
    }

    @Then("the report success message should be displayed")
    public void the_report_success_message_should_be_displayed() {
        WebElement successMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[normalize-space()='Report generated successfully!']")
        ));
        String messageText = successMessage.getText();
        assertTrue(messageText.contains("Report generated successfully!"));
    }

    @Then("the invalid credentials error message should be displayed")
    public void the_invalid_credentials_error_message_should_be_displayed() {
        WebElement errorMessage = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//p[normalize-space()='Invalid credentials or user is not a manager']")
        ));
        String messageText = errorMessage.getText();
        assertTrue(messageText.contains("Invalid credentials or user is not a manager"));
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}