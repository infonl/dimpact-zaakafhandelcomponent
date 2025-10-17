# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Personen

  Scenario: Bob wants to view a person
    Given "Bob" is logged in to zac
    Given "Bob" navigates to "zac" with path "/persoon/999993896"
    Then "Bob" sees the person
