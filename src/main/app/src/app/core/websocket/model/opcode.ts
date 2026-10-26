/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * Note that the CREATED value is not available for subscription (reason: new objectIds will be unknown client side ;-)
 */
export type Opcode = GeneratedType<"Opcode">;

export const Opcode = {
  CREATED: "CREATED",
  UPDATED: "UPDATED",
  DELETED: "DELETED",
  ANY: "ANY",
  SKIPPED: "SKIPPED",
} as const satisfies { [Key in Opcode]: Key };
