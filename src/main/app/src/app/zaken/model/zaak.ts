/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { Geometry } from "./geometry";

/**
 * @deprecated - use the `GeneratedType`
 */
export class Zaak {
  uuid: string;
  identificatie: string;
  omschrijving: string;
  toelichting: string;
  zaaktype: GeneratedType<"RestZaaktype">;
  status: GeneratedType<"RestZaakStatus">;
  resultaat: GeneratedType<"RestZaakResultaat">;
  besluiten: GeneratedType<"RestDecision">[];
  bronorganisatie: string;
  verantwoordelijkeOrganisatie: string;
  registratiedatum: string;
  startdatum: string;
  einddatumGepland: string;
  einddatum: string;
  uiterlijkeEinddatumAfdoening: string;
  publicatiedatum: string;
  archiefActiedatum: string;
  archiefNominatie: string;
  communicatiekanaal: string;
  vertrouwelijkheidaanduiding: string;
  zaakgeometrie: Geometry;
  isOpgeschort: boolean;
  redenOpschorting: string;
  isVerlengd: boolean;
  isEerderOpgeschort: boolean;
  redenVerlenging: string;
  duurVerlenging: string;
  groep: GeneratedType<"RestGroup">;
  behandelaar?: GeneratedType<"RestUser">;
  gerelateerdeZaken: GeneratedType<"RestGerelateerdeZaak">[];
  kenmerken: GeneratedType<"RESTZaakKenmerk">[];
  initiatorIdentificatieType: GeneratedType<"IdentificatieType">;
  initiatorIdentificatie: string;
  isOpen: boolean;
  isHeropend: boolean;
  isHoofdzaak: boolean;
  isDeelzaak: boolean;
  isBesluittypeAanwezig: boolean;
  isInIntakeFase: boolean;
  isProcesGestuurd: boolean;
  rechten: GeneratedType<"RestZaakRechten">;
  indicaties: GeneratedType<"ZaakIndicatie">[];
  zaakdata: Record<string, unknown>;
}
