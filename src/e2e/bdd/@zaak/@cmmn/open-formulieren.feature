# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Open Formulieren
  As a behandelaar
  I want a form that a resident submits to become a zaak
  So that I can handle the request without copying it

  Scenario: A submitted form becomes a zaak with its documents
    When a resident submits the form about aansprakelijkstelling with a photo
    Then the submission becomes a zaak
    And the zaak shows the PDF of the submission and the photo
