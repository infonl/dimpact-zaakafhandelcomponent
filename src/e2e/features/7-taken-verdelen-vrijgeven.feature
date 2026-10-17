# 
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Taken verdelen / vrijgeven

  Scenario: Bob distributes taken to a group
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/taken/werkvoorraad"
    And the page is done searching
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" distributes the taken to the first group available
    Then "Bob" gets a message confirming that the distribution of taken is starting
    And after a while the snackbar disappears

  Scenario: Bob releases taken
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/taken/werkvoorraad"
    And the page is done searching
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" releases the taken
    Then "Bob" gets a message confirming that the releasement of taken is starting
    And after a while the snackbar disappears
