Feature: Global Search Functionality

  Scenario: Basic search across entities
    Given the system has entities "Company A", "Location B", and "Sensor C"
    When the user searches for "Company A"
    Then the search results should include "Company A"
    And the results should not include unrelated entities

  Scenario: Autocomplete suggestions
    Given the system has entities "Zone Alpha", "Zone Beta"
    When the user types "Zo"
    Then autocomplete should suggest "Zone Alpha" and "Zone Beta"

  Scenario: Filtering results by entity type
    Given the user searches for "Zone"
    When the user applies the filter "Entity Type = Location"
    Then the results should only contain locations

  Scenario: Attribute-based filters
    Given the user searches for "Sensors"
    When the user applies a filter "Date Range = Last 7 days"
    Then only sensors updated in the last 7 days should be shown

  Scenario: Search results respect role permissions
    Given a user with role "Viewer"
    When the user searches for "Admin Reports"
    Then restricted results should not be visible

  Scenario: Tenant isolation in search
    Given a user from tenant "12345" searches for "Dashboard"
    When the system executes the query
    Then only dashboards belonging to tenant "12345" should be returned
