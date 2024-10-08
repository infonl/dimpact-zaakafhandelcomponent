/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MailGegevens } from "src/app/mail/model/mail-gegevens";
import { UserEventListenerActie } from "./user-event-listener-actie-enum";

export class UserEventListenerData {
  zaakUuid: string;
  planItemInstanceId: string;
  actie: UserEventListenerActie;
  zaakOntvankelijk: boolean;
  resultaatToelichting: string;
  resultaattypeUuid: string;
  restMailGegevens: MailGegevens;

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
