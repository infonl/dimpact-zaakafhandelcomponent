Feature: Zaken verdelen / vrijgeven

  Scenario: Bob distributes zaken to a group
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" distributes the zaken to the first group available
    Then "Bob" gets a message confirming that the distribution of zaken is complete

  Scenario: Bob releases zaken
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" releases the zaken
    Then "Bob" gets a message confirming that the releasement of zaken is complete
