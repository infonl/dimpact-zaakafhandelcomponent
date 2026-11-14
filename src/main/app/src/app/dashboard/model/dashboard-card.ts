/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { DashboardCardId } from "./dashboard-card-id";
import { DashboardCardType } from "./dashboard-card-type";

export class DashboardCard {
  readonly id: DashboardCardId;
  readonly type: DashboardCardType;
  readonly signaleringType?: GeneratedType<"RestSignaleringInstellingen">["type"];

  constructor(
    id: DashboardCardId,
    type: DashboardCardType,
    signaleringType?: GeneratedType<"RestSignaleringInstellingen">["type"],
  ) {
    this.id = id;
    this.type = type;
    this.signaleringType = signaleringType;
  }
}
