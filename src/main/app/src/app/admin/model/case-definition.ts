/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class CaseDefinition {
  key: string;
  naam: string;
  humanTaskDefinitions: GeneratedType<"RESTPlanItemDefinition">[];
  userEventListenerDefinitions: GeneratedType<"RESTPlanItemDefinition">[];
}
