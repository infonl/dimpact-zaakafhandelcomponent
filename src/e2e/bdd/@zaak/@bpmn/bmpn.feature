# 
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@auth
Feature: BPMN zaken
  As a ZAC user
  I want to be able to handle BPMN zaken
  So that I can organize my work

  Background:
    Given the case type "BPMN test zaaktype" exists

  Scenario: Add a new BPMN case
    Given I am on the "zaken/create" page
    When I add a new "BPMN test zaaktype" case
    Then the case gets created
    And I see the case in my overview
