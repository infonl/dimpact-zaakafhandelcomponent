Feature: Taken verdelen / vrijgeven

  Scenario: Bob distributes taken to a group
    Given "Bob" navigates to "zac" with path "/taken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" distributes the taken to group "Test group A"
    Then "Bob" gets a message confirming that the distribution of taken is complete

  Scenario: Bob releases taken
    Given "Bob" navigates to "zac" with path "/taken/werkvoorraad" with delay after of 5000 ms
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" releases the taken
    Then "Bob" gets a message confirming that the releasement of taken is complete
