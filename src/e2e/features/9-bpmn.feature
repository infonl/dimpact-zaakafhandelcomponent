# 
# SPDX-FileCopyrightText: 2025 INFO.nl
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

  Scenario: Bob fills the initial task form
    Given "Bob" is logged in to zac
    When "Bob" opens the first task
    Then "Bob" sees the form associated with the task

  Scenario: Bob creates two SmartDocuments Word files
    When "Bob" creates a SmartDocuments Word file named "file A"
    And "Bob" creates a SmartDocuments Word file named "file B"
    And "Bob" reloads the page
    Then "Bob" sees document "file A" in the documents list
    Then "Bob" sees document "file B" in the documents list

  Scenario: Bob submits the task form
    Given "Bob" is logged in to zac
    When "Bob" fills all mandatory form fields
    And "Bob" submits the filled-in form
    When Employee "Bob" is on the newly created zaak with status "-"
    Then "Bob" sees that the initial task is completed
    And "Bob" sees that the summary task is started

  Scenario: Bob inspects the summary task form
    Given "Bob" is logged in to zac
    When "Bob" opens the summary form
    Then "Bob" sees that the form contains all filled-in data
