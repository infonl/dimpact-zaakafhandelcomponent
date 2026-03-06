# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@personen
Feature: Personen

  Scenario: Bob wants to view a person
    Given "Bob" is logged in to zac
    When "Bob" wants to create a new "CMMN" zaak
    Then "Bob" sees the created zaak
    Then "Bob" sees the zaak initiator

  Scenario: Bob inspects person details
    Given "Bob" is logged in to zac
    When Employee "Bob" is on the newly created zaak
    Then "Bob" navigates to initiator details page
    Then "Bob" sees the person
