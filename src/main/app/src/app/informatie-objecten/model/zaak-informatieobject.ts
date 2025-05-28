/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZaakRechten } from "../../policy/model/zaak-rechten";
import { ZaakStatus } from "../../zaken/model/zaak-status";

/**
 * @deprecated - use the `GeneratedType`
 */
export class ZaakInformatieobject {
  zaakIdentificatie: string;
  zaakStatus: ZaakStatus;
  zaakStartDatum: string;
  zaakEinddatumGepland: string;
  zaaktypeOmschrijving: string;
  zaakRechten: ZaakRechten;
}
