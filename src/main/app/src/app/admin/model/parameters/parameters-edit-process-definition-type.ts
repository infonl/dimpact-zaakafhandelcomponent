/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export type ZaakProcessDefinition = {
  type: "CMMN" | "BPMN" | "SELECT-PROCESS-DEFINITION";
  stepperStart?: number | null;
};
