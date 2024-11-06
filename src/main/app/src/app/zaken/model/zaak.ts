/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Group } from "../../identity/model/group";
import { User } from "../../identity/model/user";
import { ZaakRechten } from "../../policy/model/zaak-rechten";
import { ZaakIndicatie } from "../../shared/indicaties/zaak-indicaties/zaak-indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { Besluit } from "./besluit";
import { Geometry } from "./geometry";
import { GerelateerdeZaak } from "./gerelateerde-zaak";
import { ZaakKenmerk } from "./zaak-kenmerk";
import { ZaakResultaat } from "./zaak-resultaat";
import { ZaakStatus } from "./zaak-status";
import { Zaaktype } from "./zaaktype";

export class Zaak {
  uuid: string;
  identificatie: string;
  omschrijving: string;
  toelichting: string;
  zaaktype: Zaaktype;
  status: ZaakStatus;
  resultaat: ZaakResultaat;
  besluiten: Besluit[];
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
  redenVerlenging: string;
  duurVerlenging: string;
  groep: Group;
  behandelaar: User;
  gerelateerdeZaken: GerelateerdeZaak[];
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
  zaakdata: {};
}
