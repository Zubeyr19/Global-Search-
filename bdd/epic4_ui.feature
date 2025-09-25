Feature: User Interface & Usability

  Scenario: Highlight search keyword in results
    Given the user searches for "Temperature"
    When the system returns "Temperature Sensor Zone 1"
    Then the word "Temperature" should be highlighted

  Scenario: Paginated results
    Given the system has 200 search results
    When the user opens the results page
    Then results should be shown in pages of 20 per page

  Scenario: Grouped results
    Given search results include Companies, Locations, Zones, and Sensors
    When the results are displayed
    Then they should be grouped in hierarchy: Company → Location → Zone → Sensor

  Scenario: Responsive design
    Given the user is on a mobile device
    When the search UI is opened
    Then the layout should adjust for mobile view
