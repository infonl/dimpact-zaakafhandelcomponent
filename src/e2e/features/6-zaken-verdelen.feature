#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
Feature: Zaken verdelen / vrijgeven

  Scenario: Bob distributes zaken to a group
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    And the page is done searching
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" distributes the zaken to the first group available
    Then "Bob" gets a message confirming that the distribution of zaken is starting
    And after a while the snackbar disappears

  Scenario: Bob releases zaken
    Given "Bob" is logged in to zac
    And "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    And the page is done searching
    And there are at least 3 zaken
    When "Bob" selects that number of zaken
    And "Bob" releases the zaken
    Then "Bob" gets a message confirming that the releasement of zaken is starting
    And after a while the snackbar disappears
