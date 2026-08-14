/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../shared/utils/generated-types";

export function isRestZaak(value: unknown): value is GeneratedType<"RestZaak"> {
  return typeof value === "object" && value !== null && "uuid" in value;
}
