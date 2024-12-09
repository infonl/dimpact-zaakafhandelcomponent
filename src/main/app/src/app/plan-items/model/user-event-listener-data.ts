/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { UserEventListenerActie } from "./user-event-listener-actie-enum";

export class UserEventListenerData {
  zaakUuid: string;
  planItemInstanceId: string;
  actie: UserEventListenerActie;
  zaakOntvankelijk: boolean;
  resultaatToelichting: string;
  resultaattypeUuid: string;
  restMailGegevens: GeneratedType<"RESTMailGegevens">;

  constructor(
    actie: UserEventListenerActie,
    planItemInstanceId: string,
    zaakUuid: string,
  ) {
    this.zaakUuid = zaakUuid;
    this.planItemInstanceId = planItemInstanceId;
    this.actie = actie;
  }
}
