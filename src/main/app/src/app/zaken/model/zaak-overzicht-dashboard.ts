/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { User } from "../../identity/model/user";
import { ZaakRechten } from "../../policy/model/zaak-rechten";
import { ZaakResultaat } from "./zaak-resultaat";

export class ZaakOverzichtDashboard {
  identificatie: string;
  omschrijving: string;
  startdatum: string;
  zaaktype: string;
}
