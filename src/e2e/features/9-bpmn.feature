# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: BPMN

  Scenario: Bob wants to create a new zaak
    Given "Bob" is logged in to zac
    When "Bob" wants to create a new "BPMN" zaak
    Then "Bob" sees the created zaak
    Then "Bob" sees the indication that no acknowledgment has been sent
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    Then "Bob" sees the created zaak
