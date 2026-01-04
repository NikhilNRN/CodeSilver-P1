Feature: Manager Login
  As a manager,
  I want to log into the application using valid credentials,
  so that I can access the manager dashboard and perform management tasks.

  #csp 154
  Scenario: Manager logs in successfully with valid credentials
    Given the manager is on the login page
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    And the manager dashboard header should be displayed

  #csp 155
  Scenario: Manager views pending expenses after login
    Given the manager is on the login page
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    And the pending expenses header should be displayed
    And the pending expense section should be displayed

  #csp 150
  Scenario: Manager generates an employee report
    Given the manager is on the login page
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
    Given the manager is on the login page
    When the manager enters invalid username "notamanager"
    And the manager enters invalid password "notapassword"
    And the manager clicks the login button
    Then the invalid credentials error message should be displayed

    #csp 153 - Session Management
  Scenario: Manager session is properly managed during logout
    Given the manager is on the login page
    When the manager enters a valid username
    And the manager enters a valid password
    And the manager clicks the login button
    Then the manager should be redirected to the manager dashboard
    And the manager dashboard header should be displayed
    When the manager clicks the logout button
    Then the manager should be redirected to the login page
    When the manager navigates to the manager dashboard directly
    Then the manager should be redirected to the login page