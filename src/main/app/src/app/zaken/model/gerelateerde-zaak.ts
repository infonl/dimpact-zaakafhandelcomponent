/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZaakRechten } from "../../policy/model/zaak-rechten";
import { ZaakRelatietype } from "./zaak-relatietype";

export class GerelateerdeZaak {
  identificatie: string;
  zaaktypeOmschrijving: string;
  statustypeOmschrijving: string;
  startdatum: string;
  relatieType: ZaakRelatietype;
  rechten: ZaakRechten;
}
