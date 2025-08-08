#language: en
# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: User Authentication
  As a ZAC user
  I want to securely log in and out of the system
  So that I can access my case management workspace and protect my session

  Scenario: Successful login
    Given I am on the ZAC login page
    When I am signing in as "testuser1"
    Then I should be redirected to the dashboard

  @auth
  Scenario: Successful logout
    Given I log out of the system
    Then I am on the ZAC login page
    And I should not have access to the dashboard

  Scenario: Failed login
    Given I am on the ZAC login page
    When I am signing in as "Hacker"
    Then I should see the message "Invalid username or password."
    And I am on the ZAC login page
    And I should not have access to the dashboard
