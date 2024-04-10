Feature: Zaken verdelen

  Scenario: Bob wants to verdeel a number of taken
    Given "Bob" navigates to "zac" with path "/taken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" verdeels the taken to group "Test group A"
    Then "Bob" gets a message confirming that the verdelen of taken is complete
