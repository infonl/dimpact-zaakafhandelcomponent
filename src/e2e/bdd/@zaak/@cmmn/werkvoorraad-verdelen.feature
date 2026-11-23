# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Werkvoorraad verdelen en vrijgeven
  As a coordinator
  I want to assign and release many zaken and taken at once
  So that the work of my groep is divided without opening each item

  @coordinator
  Scenario: A coordinator assigns and releases zaken
    Given 3 zaken for groep "Test group behandelaars domein test 1" without a behandelaar
    When I open the zaken werkvoorraad
    And I select the 3 zaken
    And I assign them to groep "Test group behandelaars domein test 1" and to the behandelaar
    Then I see the message "3 zaken worden verdeeld..."
    And the 3 zaken show the behandelaar without a page reload
    When I select the 3 zaken
    And I release them
    Then I see the message "3 zaken worden vrijgegeven..."
    And the 3 zaken show no behandelaar without a page reload

  @coordinator
  Scenario: A coordinator assigns taken
    Given 3 open taken for groep "Test group behandelaars domein test 1" without a behandelaar
    When I open the taken werkvoorraad
    And I select the 3 taken
    And I assign them to the behandelaar
    Then the 3 taken show the behandelaar without a page reload

  @behandelaar
  Scenario: A behandelaar picks up a zaak of the groep
    Given a zaak for groep "Test group behandelaars domein test 1" without a behandelaar
    When I open the zaken werkvoorraad
    And I assign the zaak to myself
    Then the zaak shows me as behandelaar
    And the zaak appears in my zaken
