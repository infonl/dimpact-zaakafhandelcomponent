/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { components } from "../../../generated/types/zac-openapi-types";

export type GeneratedType<T extends keyof components["schemas"]> =
  components["schemas"][T];
