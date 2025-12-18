# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Zaken verdelen / vrijgeven

  Scenario: Bob assigns zaken to a group and user from the list
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    And the page is done searching
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" assigns the zaken to 'Test groep A' and Bob
    Then "Bob" gets a message confirming that the assigning of zaken is starting
    And after a while the snackbar disappears

  Scenario: Bob releases zaken from the list
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    And the page is done searching and reloaded
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" releases the zaken
    Then "Bob" gets a message confirming that the releasing of zaken is starting
    And after a while the snackbar disappears
