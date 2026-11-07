/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCardId } from "./dashboard-card-id";

export class DashboardCardInstelling {
  id: number;
  cardId: DashboardCardId;
  signaleringType?: GeneratedType<"RestSignaleringInstellingen">["type"];
  column: number;
  row: number;
}
