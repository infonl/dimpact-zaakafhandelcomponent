/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HumanTaskReferentieTabel } from "./human-task-referentie-tabel";
import { PlanItemDefinition } from "./plan-item-definition";

export class HumanTaskParameter {
  actief: boolean;
  planItemDefinition: PlanItemDefinition;
  formulierDefinitieId: string;
  defaultGroepId: string;
  doorlooptijd: number;
  referentieTabellen: HumanTaskReferentieTabel[];
}
