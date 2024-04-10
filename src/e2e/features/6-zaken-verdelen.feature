Feature: Zaken verdelen

  Scenario: Bob wants to verdeel a number of zaken
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" verdeels the zaken to group "Test group A"
    Then "Bob" gets a message confirming that the verdelen of zaken is complete
