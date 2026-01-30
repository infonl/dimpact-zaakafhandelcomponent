# 
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: BPMN

  Scenario: Bob wants to create a new BPMN zaak
    Given "Bob" is logged in to zac
    When "Bob" wants to create a new "BPMN" zaak
    Then "Bob" sees the created zaak
    Then "Bob" sees the indication that no acknowledgment has been sent
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    Then "Bob" sees the created zaak

  Scenario: Bob changes the assigned user and group
    Given "Bob" is logged in to zac
    When Employee "Bob" is on the newly created
    Then "Bob" sees group "Test groep B" and behandelaar "zaakE2etest User2" in the zaak data
    Given Employee "Bob" assigns the zaak to group "Coordinators domein test 1 - new IAM" and user "Coordinator 1 New IAM "
    Then "Bob" sees group "Coordinators domein test 1 - new IAM" and user "Coordinator 1 New IAM" in the zaak data

  Scenario: Bob opens the initial task form
    Given "Bob" is logged in to zac
    When Employee "Bob" is on the newly created zaak
    And "Bob" opens the active task
    Then "Bob" sees the form associated with the task

  Scenario: Bob creates two SmartDocuments Word files
    Given "Bob" is logged in to zac
    When Employee "Bob" is on the newly created zaak
    And "Bob" opens the active task
    Given "Bob" creates a SmartDocuments Word file named "file A"
    When "Bob" reloads the page
    Then "Bob" sees document "file A" in the documents list
    Given "Bob" creates a SmartDocuments Word file named "file B"
    When "Bob" reloads the page
    Then "Bob" sees document "file B" in the documents list

  Scenario: Bob fills-in and submits the task form
    Given "Bob" is logged in to zac
    When Employee "Bob" is on the newly created zaak
    And "Bob" opens the active task
    Then "Bob" sees the desired form fields values
    Given "Bob" fills all mandatory form fields
    And "Bob" submits the filled-in form
    When Employee "Bob" is on the newly created zaak
    Then "Bob" sees that the initial task is completed
    Then "Bob" sees that the summary task is started with group "Beheerders elk domein - new IAM" and user "Beheerder 1 New IAM"

  Scenario: Bob inspects the summary task form
    Given "Bob" is logged in to zac
    And Employee "Bob" is on the newly created zaak
    When "Bob" opens the active task
    Then "Bob" sees that the summary form contains all filled-in data

  Scenario: Bob confirms the data in the summary form
    Given "Bob" is logged in to zac
    And Employee "Bob" is on the newly created zaak
    When "Bob" opens the active task
    And "Bob" confirms the data in the form
    When Employee "Bob" is on the newly created zaak with status "Afgerond"
    Then "Bob" sees the zaak result is set to "Verleend"
