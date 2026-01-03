package com.revature.e2e.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class DenialWorkflow {
    private WebDriver driver;
    private WebDriverWait wait;
    private WebElement selectedExpenseRow;
    private String selectedDescription;

    @Before
    public void setUp() {
        WebDriverManager.firefoxdriver().setup();

        FirefoxOptions options = new FirefoxOptions();

        driver = new FirefoxDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Given("we are on the login page")
    public void we_are_on_the_login_page() {
        driver.get("http://localhost:5001/login.html");
        assertTrue(driver.getCurrentUrl().contains("login"));
    }

    @Given("the manager logs in")
    public void the_manager_logs_in() throws InterruptedException {
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("username")));
        WebElement usernameInput = driver.findElement(By.id("username"));
        usernameInput.clear();
        usernameInput.sendKeys("manager1");

        WebElement passwordInput = driver.findElement(By.id("password"));
        passwordInput.clear();
        passwordInput.sendKeys("password123");

        driver.findElement(By.xpath("//button[@type='submit']")).click();

        // Wait for navigation to complete
        wait.until(ExpectedConditions.urlContains("manager.html"));
    }

    @Given("the manager views all pending expenses")
    public void the_manager_views_all_pending_expenses() {
        // Wait explicitly for the header to be visible
        WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//h1[normalize-space()='Manager Expense Dashboard']")
        ));

        String headerText = header.getText();
        assertTrue(headerText.contains("Manager Expense Dashboard"));
    }

    @When("the manager selects the expense with description {string}")
    public void the_manager_selects_the_expense_with_description(String description) {
        this.selectedDescription = description;

        // Find and store the expense row
        selectedExpenseRow = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//tr[td[contains(text(), '" + description + "')]]")
        ));
        assertNotNull(selectedExpenseRow, "Could not find expense with description: " + description);
    }

    @When("the manager clicks the review button")
    public void the_manager_clicks_the_review_button() {
        // Click the review button in the selected row
        WebElement reviewButton = selectedExpenseRow.findElement(
                By.xpath(".//td[5]/button")
        );
        reviewButton.click();
    }

    @When("the manager denies the expense")
    public void the_manager_denies_the_expense() {
        WebElement denyButton = wait.until(ExpectedConditions.elementToBeClickable(
                By.id("deny-expense")
        ));
        denyButton.click();

        // Wait longer for modal to disappear (increase timeout)
        WebDriverWait longWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        longWait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("review-modal")));
    }

    @When("the manager clicks all expenses button")
    public void the_manager_clicks_all_expenses_button() {
        // Wait for modal to be gone
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("review-modal")));
        } catch (Exception e) {
            // Modal might already be gone, continue
        }

        WebElement allExpensesButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.id("show-all-expenses")
        ));

        // Use JavaScript click to bypass any overlay issues
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", allExpensesButton);
    }

    @Then("the expense status should be {string}")
    public void the_expense_status_should_be(String expectedStatus) {
        // Re-find the row using stored description since page was refreshed
        WebElement statusCell = wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.xpath("//tr[td[contains(text(), '" + selectedDescription + "')]]//td[5]")
        ));

        String actualStatus = statusCell.getText().trim();
        assertEquals(expectedStatus.toLowerCase(), actualStatus.toLowerCase(),
                "Expected status to be " + expectedStatus + " but was " + actualStatus);
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}