# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
@taken-verdelen-vrijgeven
Feature: Taken verdelen / vrijgeven

  Scenario: Bob assigns taken to a group and user from the list
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/taken/werkvoorraad"
    And the page is done searching
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" assigns the taken to the first group and user available
    Then "Bob" gets a message confirming that the assigning of taken is starting
    And after a while the snackbar disappears

  Scenario: Bob releases taken
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/taken/werkvoorraad"
    And the page is done searching and reloaded
    And there are at least 3 taken
    When "Bob" selects that number of taken
    And "Bob" releases the taken
    Then "Bob" gets a message confirming that the releasing of taken is starting
    And after a while the snackbar disappears
