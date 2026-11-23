# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Zaak afronden
  As a behandelaar
  I want to record a besluit and close the zaak
  So that the outcome of the zaak is final

  As a recordmanager
  I want to reopen a closed zaak
  So that the outcome stays correctable, for example after a bezwaar

  @behandelaar
  Scenario: A behandelaar records a besluit and closes the zaak
    Given a zaak in behandeling
    And the zaak has a document "E2E besluitbrief" of type "besluit"
    When I open the zaak
    And I record a besluit and link the document "E2E besluitbrief"
    Then the zaak shows the besluit with the document "E2E besluitbrief"
    When I close the zaak with the resultaat "Toegekend"
    Then the status is "Afgerond"
    And the zaak appears in the list of afgehandelde zaken

  @recordmanager
  Scenario: A recordmanager reopens a closed zaak
    Given a closed zaak with the resultaat "Toegekend"
    When I open the zaak
    And I reopen the zaak with the reason "Bezwaar ontvangen"
    Then the zaak shows the indicatie "heropend"
    And the zaak has no resultaat
    And the zaak is gone from the list of afgehandelde zaken
