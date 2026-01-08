Feature: Complete Expense Review Workflow
  As a manager
  I want to approve or deny employee expense requests
  So that expenses are properly reviewed and their status is updated

  Scenario Outline: Manager logs in and reviews an expense request
    Given we are on the login page
    And the manager logs in
    And the manager views all pending expenses
    When the manager selects the expense with description "<expense_description>"
    And the manager clicks the review button
    And the manager <action>s the expense
    And the manager clicks all expenses button
    Then the expense status should be "<expected_status>"

    Examples:
      | expense_description | action  | expected_status |
      | ooo                 | deny    | denied          |
      | Gas                 | approve | approved        |