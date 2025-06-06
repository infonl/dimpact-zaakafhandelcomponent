/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class ZaakInformatieobject {
  zaakIdentificatie: string;
  zaakStatus: GeneratedType<"RestZaakStatus">;
  zaakStartDatum: string;
  zaakEinddatumGepland: string;
  zaaktypeOmschrijving: string;
  zaakRechten: GeneratedType<"RestZaakRechten">;
}
