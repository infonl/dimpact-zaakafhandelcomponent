# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@beheerder
Feature: Zaakafhandelparameters
  As a beheerder
  I want to change how ZAC handles a zaaktype
  So that new zaken of that zaaktype follow the new setting

  # No other journey uses this zaaktype, so the change does not affect journeys that run at the same time.
  Background:
    Given the zaaktype "Test zaaktype 4" is set up and assigns new zaken to groep "Test group behandelaars domein test 1"

  Scenario: A changed default groep applies to new zaken
    When I open the zaakafhandelparameters of "Test zaaktype 4"
    And I change the default groep to "Test group behandelaars with a very long group name"
    And I save the zaakafhandelparameters
    Then I see the message "De zaakafhandelparameters zijn opgeslagen"
    When I open the zaakafhandelparameters of "Test zaaktype 4"
    Then the default groep is "Test group behandelaars with a very long group name"
    When I open the form to create a zaak
    And I choose the zaaktype "Test zaaktype 4"
    And I choose the communicatiekanaal "E-mail"
    And I enter the omschrijving "E2E zaak na gewijzigde parameters"
    And I create the zaak
    Then the zaak is assigned to groep "Test group behandelaars with a very long group name"
