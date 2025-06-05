/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { PlanItemDefinition } from "./plan-item-definition";

/**
 * @deprecated - use the `GeneratedType`
 */
export class CaseDefinition {
  key: string;
  naam: string;
  humanTaskDefinitions: PlanItemDefinition[];
  userEventListenerDefinitions: PlanItemDefinition[];
}
