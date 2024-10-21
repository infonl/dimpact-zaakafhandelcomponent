# 
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

Feature: External Systems

  Scenario: Employee wants to create a new document for a zaak
    Given "Bob" is logged in to zac
    And Employee "Bob" is on the newly created zaak with status "Intake"
    
    When Employee "Bob" clicks on Create Document for zaak
    And Employee "Bob" enters create document form fields
    And Employee "Bob" clicks the SmartDocuments Wizard finish button
  