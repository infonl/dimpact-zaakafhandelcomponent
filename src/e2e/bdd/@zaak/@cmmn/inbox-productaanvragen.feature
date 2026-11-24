# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@coordinator
Feature: Inbox productaanvragen
  As a coordinator
  I want to turn an unknown productaanvraag into a zaak
  So that no request from a resident gets lost

  Background:
    Given the zaaktype "Test zaaktype 2" is set up and assigns new zaken to groep "Test group behandelaars domein test 1"
    And a productaanvraag "E2E aanvraag" of a type that ZAC does not know

  Scenario: A productaanvraag of an unknown type becomes a zaak
    When I open the inbox productaanvragen
    Then I see the productaanvraag "E2E aanvraag"
    When I start a zaak from the productaanvraag "E2E aanvraag"
    And I choose the zaaktype "Test zaaktype 2"
    And I choose the communicatiekanaal "E-formulier"
    And I enter the omschrijving "E2E zaak uit productaanvraag"
    And I create the zaak
    Then the zaak opens with status "Intake"
    And the zaak shows the document of the productaanvraag
    And the inbox productaanvragen no longer shows "E2E aanvraag"
