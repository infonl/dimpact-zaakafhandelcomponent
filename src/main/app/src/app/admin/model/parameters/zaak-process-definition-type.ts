/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export type ZaakProcessSelect = "CMMN" | "BPMN" | "SELECT-PROCESS-DEFINITION";

export type ZaakProcessDefinition = {
  type: ZaakProcessSelect;
  selectedIndexStart?: number | null;
};
