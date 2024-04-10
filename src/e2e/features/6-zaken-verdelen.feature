Feature: Zaken verdelen

  Scenario: Bob distributes zaken to a group
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" distributes the zaken to group "Test group A"
    Then "Bob" gets a message confirming that the distribution of zaken is complete
