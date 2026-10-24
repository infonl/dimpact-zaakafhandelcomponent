/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { BAGObject } from "../../bag/model/bagobject";
import { InboxProductaanvraag } from "../../productaanvragen/model/inbox-productaanvraag";
import { Zaak } from "./zaak";

export class ZaakAanmaakGegevens {
  constructor(
    public zaak: Zaak,
    public inboxProductaanvraag?: InboxProductaanvraag,
    public bagObjecten?: BAGObject[],
  ) {}
}
