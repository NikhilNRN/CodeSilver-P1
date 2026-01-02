# Feature: Employee Expense Management (Edge)

Feature: Employee Expense Management (Edge)
  As an employee
  I want to manage my expenses through the web application
  So that I can submit reimbursement requests and track their status

  Background:
    Given the Employee app is running on port 5000
    And I am on "Edge"
    And I am on the login page

  # Test Case: TC-E2E-001
  @login @happy_path
  Scenario: Successful employee login
    Given I enter username "employee1"
    And I enter password "password123"
    When I click the login button
    Then I should be redirected to the expense dashboard
    And I should see a welcome message

  # Test Case: TC-E2E-002
  @login @sad_path
  Scenario: Failed login with invalid credentials
    Given I enter username "notReal"
    And I enter password "wrongPassword321"
    When I click the login button
    Then I should remain on the login page
    And I should see an error message "Invalid credentials"

  # Test Case: TC-E2E-003
  @expense @submit
  Scenario: Submit a new expense
    Given I am logged in as "employee1" with password "password123"
    When I navigate to the expense submission form
    And I enter expense amount "75.50"
    And I enter expense description "Team lunch meeting"
    And I select today's date
    And I click the submit button
    Then I should see a success message
    And the expense should appear in my expense list with status "pending"

  # Test Case: TC-E2E-004
  @expense @view
  Scenario: View expense list
    Given I am logged in as "employee1" with password "password123"
    When I navigate to the expense list
    Then I should see a table of my expenses
    And each expense should show amount, description, date, and status

  # Test Case: TC-E2E-005
  @expense @edit
  Scenario: Edit a pending expense
    Given I am logged in as "employee1" with password "password123"
    And I have a pending expense
    When I click the edit button for the pending expense
    And I change the amount to "24.67"
    And I change the description to "I could be gaming (updated description)"
    And I save the changes
    Then I should see the updated expense in the list
    And the expense amount should be "24.67"

  # Test Case: TC-E2E-007 - Scenario Outline
  @expense @filter
  Scenario Outline: Filter expenses by status
    Given I am logged in as "employee1" with password "password123"
    When I navigate to the expense list
    And I filter by status "<status>"
    Then I should only see expenses with status "<status>"

    Examples:
      | status   |
      | pending  |
      | approved |
      | denied   |

  @logout
  Scenario: Employee logout
    Given I am logged in as "employee1" with password "password123"
    When I click the logout button
    Then I should be redirected to the login page
    And I should not be able to access the dashboard