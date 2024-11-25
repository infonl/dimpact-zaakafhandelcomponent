#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
Feature: Login

  Scenario: Bob wants to login to ZAC
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" logs in
    Then "Bob" sees the text: "Dashboard"

  Scenario: Bob wants to log out of the application after being logged in
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" clicks on element with accessibility label: "Gebruikers profiel"
    When "Bob" clicks on element with text: "Uitloggen"
    Then "Bob" sees the text: "Sign in to your account"

  Scenario: Bob logs back in
    Given "Bob" navigates to "zac" with path "/"
    Then "Bob" sees the text: "Sign in to your account"
    When "Bob" logs in
    Then "Bob" sees the text: "Dashboard"

  Scenario: Bob wants to log out of the application after being logged in
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" clicks on element with accessibility label: "Gebruikers profiel"
    When "Bob" clicks on element with text: "Uitloggen"
    Then "Bob" sees the text: "Sign in to your account"

  Scenario: Bob logs back in
    Given "Bob" navigates to "zac" with path "/"
    Then "Bob" sees the text: "Sign in to your account"
    When "Bob" logs in
    Then "Bob" sees the text: "Dashboard"
