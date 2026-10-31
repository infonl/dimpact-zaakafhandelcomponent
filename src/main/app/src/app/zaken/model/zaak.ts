/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ZaakRechten } from "../../policy/model/zaak-rechten";
import { ZaakIndicatie } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Geometry } from "./geometry";
import { ZaakKenmerk } from "./zaak-kenmerk";
import { ZaakStatus } from "./zaak-status";
import { Zaaktype } from "./zaaktype";

export class Zaak {
  uuid: string;
  identificatie: string;
  omschrijving: string;
  toelichting: string;
  zaaktype: Zaaktype;
  status: ZaakStatus;
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
  kenmerken: ZaakKenmerk[];
  initiatorIdentificatieType: GeneratedType<"IdentificatieType">;
  initiatorIdentificatie: string;
  isOpen: boolean;
  isHeropend: boolean;
  isHoofdzaak: boolean;
  isDeelzaak: boolean;
  isOntvangstbevestigingVerstuurd: boolean;
  isBesluittypeAanwezig: boolean;
  isInIntakeFase: boolean;
  isProcesGestuurd: boolean;
  rechten: ZaakRechten;
  indicaties: ZaakIndicatie[];
  zaakdata: Record<string, any>;
}
