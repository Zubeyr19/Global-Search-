Feature: Monitoring, Logging & Reporting

  Scenario: Real-time monitoring dashboard
    Given the system is running
    When an admin views the dashboard
    Then they should see CPU, memory, and query latency metrics in real-time

  Scenario: Query logging with tenant context
    Given a user with tenant_id "12345" searches "Temperature"
    When the query is executed
    Then the system should log query, user_id, tenant_id, and timestamp

  Scenario: Log retention policy
    Given logs older than 90 days exist
    When the system performs cleanup
    Then those logs should be deleted automatically

  Scenario: Alert for slow queries
    Given a query exceeds 2 seconds response time
    When the query finishes
    Then the system should trigger an alert
    And notify the admin

  Scenario: Scheduled reporting
    Given an admin has configured weekly reports
    When the schedule is reached
    Then the system should email the report to the admin
