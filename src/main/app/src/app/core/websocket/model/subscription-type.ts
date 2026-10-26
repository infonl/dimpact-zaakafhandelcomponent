/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

export type SubscriptionType = GeneratedType<"SubscriptionType">;

export const SubscriptionType = {
  CREATE: "CREATE",
  DELETE: "DELETE",
  DELETE_ALL: "DELETE_ALL",
} as const satisfies { [Key in SubscriptionType]: Key };
