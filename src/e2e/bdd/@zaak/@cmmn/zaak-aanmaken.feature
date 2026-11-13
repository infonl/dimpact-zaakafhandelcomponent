# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Zaak aanmaken
  As a behandelaar
  I want to register a zaak for a resident or a company
  So that the request is stored in Open Zaak and lands with the right groep

  Background:
    Given the zaaktype "Test zaaktype 2" is set up and assigns new zaken to groep "Test group behandelaars domein test 1"

  Scenario Outline: A behandelaar creates a zaak with a <initiator> as initiator
    When I open the form to create a zaak
    And I choose the zaaktype "Test zaaktype 2"
    And I search the <initiator> with <zoekveld> "<zoekwaarde>" and select "<naam>" as initiator
    And I search an address from the BAG stub and add it as BAG object
    And I choose the communicatiekanaal "E-mail"
    And I enter the omschrijving "E2E zaak met <initiator>"
    And I create the zaak
    Then the zaak opens with status "Intake"
    And the zaak shows "<naam>" as initiator
    And the zaak shows that address as BAG object
    And the zaak is assigned to groep "Test group behandelaars domein test 1"
    And the zaak shows that no ontvangstbevestiging is sent
    And the zaak appears in the zaken werkvoorraad

    Examples:
      | initiator | zoekveld | zoekwaarde | naam            |
      | persoon   | BSN      | 999992958  | Anita van Buren |
      | bedrijf   | KvK      | 12345678   | fakeNaam1       |
