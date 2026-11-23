# 
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
# 
@behandelaar
Feature: Taak afhandelen
  As a behandelaar
  I want to start a task on a zaak and complete it
  So that the work of the zaak moves forward and the zaak records the result

  Background:
    Given a zaak in behandeling

  Scenario: A behandelaar starts and completes a Goedkeuren task
    When I open the zaak
    And I start the task "Goedkeuren" and assign it to myself
    Then the task "Goedkeuren" appears in my takenlijst
    When I open the task "Goedkeuren" from my takenlijst
    And I complete the task with the decision "Akkoord" and the toelichting "E2E akkoord"
    Then the task "Goedkeuren" is gone from my takenlijst
    And the zaak lists the task "Goedkeuren" as afgerond
