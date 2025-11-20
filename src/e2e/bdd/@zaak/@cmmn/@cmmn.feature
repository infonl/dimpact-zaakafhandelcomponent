# 
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@auth
Feature: CMMN zaken
  As a ZAC user
  I want to be able to handle CMMN zaken
  So that I can organize my work

  Background:
    Given the case type "CMMN" exists

  Scenario: Add a new case
    Given I am on the "zaken/create" page
    When I add a new case
    Then the case gets created
    And I see the case in my overview
