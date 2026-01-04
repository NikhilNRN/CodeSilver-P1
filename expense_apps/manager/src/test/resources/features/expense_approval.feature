# Feature: Manager Expense Approval

Feature: Manager Expense Approval Workflow
  As a manager
  I want to review and process expense requests
  So that I can manage team reimbursements effectively

  Background:
    Given the Manager app is running on port 5001
    And I am on the manager login page

  # Test Case: TC-E2E-008
  @login @happy_path
  Scenario: Manager login successfully
    Given I enter manager username "manager1"
    And I enter manager password "password123"
    When I click the manager login button
    Then I should be redirected to the manager dashboard
    And I should see the expense management panel

  # Test Case: TC-E2E-009
  @pending @view
  Scenario: View pending expenses
    Given I am logged in as manager "manager1" with password "password123"
    When I navigate to the pending expenses tab
    Then I should see a list of pending expenses
    And each expense should show employee name and amount and status

  # Test Case: TC-E2E-010
  @approval
  Scenario: Approve an expense
    Given
    And
    When
    Then
    And

  # Test Case: TC-E2E-011
  @denial
  Scenario: Deny an expense with comment
    Given I am logged in as manager "manager1" with password "password123"
    When I navigate to the pending expenses tab
    And I select first reviewable expense
    And I select decision "Deny" with comment "Test"
    Then The expense should display successful denial
    And I should be redirected to the pending expenses tab

  # Test Case: TC-E2E-012
  @reports
  Scenario: Generate expense CSV report
    Given I am logged in as manager "manager1" with password "password123"
    When I navigate to the reports section
    And I fill out Employee "1" with Category "Gas"
    And I fill out startDate "12/01/2025" with endDate "12/31/2025"
    And I click the export CSV button
    Then a CSV file should be downloaded
    And the CSV should contain expense data


  @logout
  Scenario: Manager logout successfully
    Given I am logged in as manager "manager1" with password "password123"
    When I click the manager logout button
    Then I should be redirected to the original login page

  # Scenario Outline for multiple approval decisions
  @approval @parameterized
  Scenario Outline: Process expense with different decisions
    Given
    And
    When
    And
    Then

    Examples:
      | expense_id | decision | comment              | result_status |
      | 1          | approve  | Good documentation   | approved      |
      | 2          | deny     | Missing receipts     | denied        |

  @view_all
  Scenario: View all expenses
    Given I am logged in as manager "manager1" with password "password123"
    When I click the refresh button
    Then all expenses should be visible and reviewable