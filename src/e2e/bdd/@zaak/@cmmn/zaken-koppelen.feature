# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Zaken koppelen
  As a behandelaar
  I want to link a deelzaak to a hoofdzaak
  So that I see how the zaken relate and close them in the right order

  Background:
    Given a zaak "E2E hoofdzaak" in behandeling
    And a zaak "E2E deelzaak" in behandeling, of a zaaktype that the hoofdzaak allows as deelzaak

  Scenario: A deelzaak must close before its hoofdzaak
    When I open the zaak "E2E hoofdzaak"
    And I link the zaak "E2E deelzaak" as deelzaak
    Then the zaak "E2E hoofdzaak" shows "E2E deelzaak" under "Gerelateerde zaken"
    And the zaak "E2E deelzaak" shows "E2E hoofdzaak" under "Gerelateerde zaken"
    And I cannot close the zaak "E2E hoofdzaak" while "E2E deelzaak" is open
    When I close the zaak "E2E deelzaak" with a resultaat
    Then I can close the zaak "E2E hoofdzaak" with a resultaat
