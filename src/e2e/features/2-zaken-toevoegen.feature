# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Zaken toevoegen

  Scenario: Bob wants to create a new zaak
    Given "Bob" is logged in to zac
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak
    Then "Bob" sees the zaak initiator
    Then "Bob" sees the indication that no acknowledgment has been sent
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    Then "Bob" sees the created zaak
