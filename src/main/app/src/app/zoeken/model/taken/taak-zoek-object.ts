/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../../shared/utils/generated-types";

/**
 * @deprecated - use the `GeneratedType`
 */
export class TaakZoekObject
  implements
    GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
{
  id: string;
  type: GeneratedType<"ZoekObjectType">;
  naam: string;
  toelichting: string;
  status: string;
  zaaktypeUuid: string;
  zaaktypeIdentificatie: string;
  zaaktypeOmschrijving: string;
  zaakUuid: string;
  zaakIdentificatie: string;
  zaakOmschrijving: string;
  zaakToelichting: string;
  creatiedatum: string;
  toekenningsdatum: string;
  fataledatum: string;
  groepID: string;
  groepNaam: string;
  behandelaarNaam?: string;
  behandelaarGebruikersnaam?: string;
  taakData: string[];
  taakInformatie: string[];
  rechten: GeneratedType<"RestTaakRechten">;
}
