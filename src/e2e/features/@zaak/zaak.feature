# 
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
Feature: Zaken
    As a ZAC user
    I want to add new cases
    So that I can organize my work

    Background:
        Given I am signed in as "testuser1"
        And I am on the "/zaken/werkvoorraad" page
    
    Scenario: Add a new case
        When I add a new "Indienen aansprakelijkstelling door derden behandelen" case
        Then the case gets created
        And I see the case in my overview