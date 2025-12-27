Feature: Authentication & Authorization

  Scenario: Successful login with valid credentials
    Given a registered user exists with username "alice" and password "password123"
    When the user attempts to login with username "alice" and password "password123"
    Then the system should authenticate the user
    And the user should receive a JWT token with tenant_id and role claims

  Scenario: Session expiration after inactivity
    Given a user is logged in
    And the user has been inactive for 30 minutes
    When the user attempts to perform an action
    Then the system should require the user to log in again

  Scenario: Unauthorized access to admin-only resources
    Given a user with role "Viewer"
    When the user attempts to access the Admin Dashboard
    Then the system should deny access
    And show an "Unauthorized" error

  Scenario: Track failed login attempts
    Given a user enters the wrong password 5 times
    When the user attempts another login
    Then the system should block the login
    And return a "Too many failed attempts" message
