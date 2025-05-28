/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Vertrouwelijkheidaanduiding } from "../../informatie-objecten/model/vertrouwelijkheidaanduiding.enum";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaaktypeRelatie } from "./zaaktype-relatie";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Zaaktype {
  uuid: string;
  identificatie: string;
  doel: string;
  omschrijving: string;
  referentieproces: string;
  servicenorm: boolean;
  versiedatum: string;
  beginGeldigheid: string;
  eindeGeldigheid: string;
  nuGeldig: boolean;
  vertrouwelijkheidaanduiding: Vertrouwelijkheidaanduiding;
  opschortingMogelijk: boolean;
  verlengingMogelijk: boolean;
  verlengingstermijn: number;
  zaaktypeRelaties: ZaaktypeRelatie[];
  informatieobjecttypes?: string[];
  zaakafhandelparameters: GeneratedType<"RestZaakafhandelParameters">;
}
