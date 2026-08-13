# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@open-formulieren
Feature: Open Formulieren

  @live-env-only
  Scenario: Resident fills in indienen-aansprakelijkheid-behandelen open-forms form
    Given Resident "Alice" fills in the indienen-aansprakelijkheid-behandelen open-forms form
    When Resident "Alice" submits the open-forms form
    And "Bob" is logged in to zac
    And Employee "Bob" opens the zaak that was created from the open-forms submission
    Then Employee "Bob" sees the zaak that "Alice" created in open-forms
