# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar @live-env-only
Feature: SmartDocuments
  As a behandelaar
  I want to create a document from a template
  So that it has the house style and the data of the zaak

  Background:
    Given a zaak in intake

  Scenario: A behandelaar creates a document with the SmartDocuments wizard
    When I open the zaak
    And I start the SmartDocuments wizard for the document "E2E SmartDocuments"
    And I complete the SmartDocuments wizard
    Then the zaak lists the document "E2E SmartDocuments"
    And the document "E2E SmartDocuments" shows the titel, the beschrijving and the auteur that I entered
