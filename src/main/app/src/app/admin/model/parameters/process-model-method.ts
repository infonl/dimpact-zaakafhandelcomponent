/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

export type ProcessModelMethod = "CMMN" | "BPMN";

export type ProcessModelMethodSelection = {
  type: ProcessModelMethod | null;
  selectedIndexStart?: number | null;
};
