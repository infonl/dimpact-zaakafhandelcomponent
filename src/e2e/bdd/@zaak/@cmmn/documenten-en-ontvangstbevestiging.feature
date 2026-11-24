# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Documenten en ontvangstbevestiging
  As a behandelaar
  I want to add documents to a zaak and confirm receipt to the initiator
  So that the zaak holds every document and the initiator knows we received the request

  Background:
    Given a zaak in intake with a person as initiator

  Scenario: A behandelaar adds a document and confirms receipt
    When I open the zaak
    And I add the document "brief.pdf" with the titel "E2E brief" and the type "brief"
    Then the document "E2E brief" is listed at the zaak
    And the document "E2E brief" shows the titel, the taal, the auteur and the creatiedatum
    When I send the ontvangstbevestiging to the initiator
    Then the ontvangstbevestiging is stored at the zaak as a PDF document
    And the zaak shows that the ontvangstbevestiging is sent
    When I record that the document "E2E brief" was sent by post today
    Then the document "E2E brief" shows the indicatie "is verzonden"
