# 
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: External Systems

  Scenario: Employee wants to create a new document for a zaak Using the SmartDocuments wizard
    Given "Bob" is logged in to zac
    And "Bob" wants to create a new zaak
    When Employee "Bob" clicks on Create Document button for the new zaak
    And Employee "Bob" enters and submits the form to start the SmartDocuments wizard
    And Employee "Bob" completes the SmartDocuments wizard
    And Employee "Bob" closes the wizard result page
    And Employee "Bob" views the created document
    Then Employee "Bob" sees all added details in the created document meta data
