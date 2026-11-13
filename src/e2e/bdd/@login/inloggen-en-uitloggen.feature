# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Inloggen en uitloggen
  As a ZAC user
  I want to sign in through Keycloak and sign out again
  So that only I can use my session

  Background:
    Given I am signed out
    When I open ZAC
    Then I see the Keycloak login page

  Scenario: A user signs in and signs out
    When I sign in as a behandelaar
    Then I see my dashboard
    And the header shows that I am signed in as the behandelaar
    When I sign out
    Then I see the Keycloak login page
    When I open ZAC again
    Then I see the Keycloak login page
    And I do not see my dashboard

  Scenario: A wrong password is refused
    When I sign in as a behandelaar with a wrong password
    Then I see the message "Invalid username or password."
    And I stay on the Keycloak login page
