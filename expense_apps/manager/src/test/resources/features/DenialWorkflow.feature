Feature: Complete Expense Review Workflow
  As a manager
  I want to approve or deny employee expense requests
  So that expenses are properly reviewed and their status is updated

  #csp 150
  Scenario: Manager logs in and denies an expense request
    Given we are on the login page
    And the manager logs in
    And the manager views all pending expenses
    When the manager selects the expense with description "ooo"
    And the manager clicks the review button
    And the manager denies the expense
    And the manager clicks all expenses button
    Then the expense status should be "denied"

    #csp 156
  Scenario: Manager logs in and approves an expense request
    Given we are on the login page
    And the manager logs in
    And the manager views all pending expenses
    When the manager selects the expense with description "Gas"
    And the manager clicks the review button
    And the manager approves the expense
    And the manager clicks all expenses button
    Then the expense status should be "approved"