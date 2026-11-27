# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: BPMN-proces
  As a behandelaar
  I want to handle a BPMN zaak through its task forms
  So that the zaak follows the process of the municipality from start to end

  Background:
    Given the E2E process and its forms are deployed
    And the zaaktype "BPMN test zaaktype 5" uses the E2E process and assigns new zaken to groep "Test group behandelaars domein test 1"

  Scenario: A behandelaar handles a BPMN zaak from start to end
    When I open the form to create a zaak
    And I choose the zaaktype "BPMN test zaaktype 5"
    And I choose the communicatiekanaal "E-mail"
    And I enter the omschrijving "E2E BPMN-zaak"
    And I create the zaak
    Then the zaak shows the task "MultipleFormItems"
    When I open the task "MultipleFormItems" of the zaak
    And I fill in all required fields of the form, with the resultaat "Verleend" and the status "Afgerond"
    And I submit the form
    Then the zaak lists the task "MultipleFormItems" as afgerond
    And the task "MultipleFormItemsSummary" starts for groep "Test group behandelaars domein test 1"
    When I open the task "MultipleFormItemsSummary" of the zaak
    Then the form shows the values that I entered, with the resultaat "Verleend" and the status "Afgerond"
    When I confirm the summary
    Then the status is "Afgerond"
    And the zaak has the resultaat "Verleend"
