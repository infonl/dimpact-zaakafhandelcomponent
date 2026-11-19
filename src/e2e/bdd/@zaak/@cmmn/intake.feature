# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Intake
  As a behandelaar
  I want to ask for missing information, pause the zaak and give it more time
  So that the deadlines stay correct while I wait for the initiator

  Background:
    Given a zaak in intake with a person as initiator

  Scenario: A zaak waits for aanvullende informatie, then continues and is extended
    When I open the zaak
    And I request aanvullende informatie from the initiator
    And I suspend the zaak for 14 days with the reason "Wacht op informatie"
    Then the status is "Wacht op aanvullende informatie"
    And the zaak shows the indicatie "opgeschort"
    And the uiterste einddatum moves 14 days later
    When I complete the aanvullende informatie task
    And I resume the zaak with the reason "Informatie ontvangen"
    Then the status is "Intake"
    And the zaak no longer shows the indicatie "opgeschort"
    When I finish the intake as ontvankelijk
    Then the status is "In behandeling"
    When I extend the zaak by 10 days with the reason "Extra onderzoek nodig"
    Then the zaak shows the indicatie "verlengd"
    And the uiterste einddatum moves 10 days later
    And the historie lists the request, the suspension, the resumption, the intake and the extension
