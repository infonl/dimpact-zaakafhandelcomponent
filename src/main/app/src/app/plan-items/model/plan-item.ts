/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { FormulierDefinitieID } from "../../admin/model/formulier-definitie";
import { PlanItemType } from "./plan-item-type.enum";
import { UserEventListenerActie } from "./user-event-listener-actie-enum";

/**
 * @deprecated - use the `GeneratedType`
 */
export class PlanItem {
  id: string;
  naam: string;
  actief: boolean;
  type: PlanItemType;
  groepId: string;
  formulierDefinitie: FormulierDefinitieID;
  tabellen: { [key: string]: string[] };
  zaakUuid: string;
  userEventListenerActie: UserEventListenerActie;
  toelichting: string;
  fataleDatum: string;
}
