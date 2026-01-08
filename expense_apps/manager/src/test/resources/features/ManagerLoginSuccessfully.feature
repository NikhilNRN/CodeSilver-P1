Feature: Manager Login
  As a manager,
  I want to log into the application using valid credentials,
  so that I can access the manager dashboard and perform management tasks.

  Background:
    Given the manager is on the login page

  Scenario Outline: Manager logs in successfully and views dashboard elements
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    And the "<element_to_verify>" should be displayed

    Examples:
      | element_to_verify              | csp |
      | manager dashboard header       | 154 |
      | pending expenses header        | 155 |
      | pending expense section        | 155 |

  #csp 150
  Scenario: Manager generates an employee report
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    When the manager clicks the show reports button
    And the manager enters employee ID "1"
    And the manager clicks the generate report button
    Then the report success message should be displayed

  #csp 152
  Scenario: Unauthorized user cannot access manager dashboard
    When the manager enters invalid username "notamanager"
    And the manager enters invalid password "notapassword"
    And the manager clicks the login button
    Then the invalid credentials error message should be displayed

  #csp 153
  Scenario: Manager session is properly managed during logout
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    And the "manager dashboard header" should be displayed
    When the manager clicks the logout button
    Then the manager should be redirected to the login page
    When the manager navigates to the manager dashboard directly
    Then the manager should be redirected to the login page