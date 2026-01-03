Feature: Complete Denial Workflow for Expense Requests

  As a manager
  I want to deny an employee's expense request
  So that the expense is properly rejected and logged with a comment

  Scenario: Manager logs in and denies an expense request
    Given we are on the login page
    And the manager logs in
    And the manager views all pending expenses
    When the manager selects the expense with description "ooo"
    And the manager clicks the review button
    And the manager denies the expense
    And the manager clicks all expenses button
    Then the expense status should be "denied"


