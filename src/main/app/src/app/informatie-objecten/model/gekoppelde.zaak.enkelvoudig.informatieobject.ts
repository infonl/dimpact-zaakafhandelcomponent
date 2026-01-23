/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DocumentRechten } from "../../policy/model/document-rechten";
import { InformatieobjectIndicatie } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { EnkelvoudigInformatieobjectOndertekening } from "./enkelvoudig-informatieobject-ondertekening";
import { FileFormat } from "./file-format";

export class GekoppeldeZaakEnkelvoudigInformatieobject
  implements GeneratedType<"RestEnkelvoudigInformatieobject">
{
  relatieType?: string;
  zaakIdentificatie?: string;
  zaakUUID?: string;

  // GeneratedType<'RestEnkelvoudigInformatieobject'>
  uuid: string;
  identificatie: string;
  titel: string;
  beschrijving: string;
  creatiedatum: string;
  registratiedatumTijd: string;
  ontvangstdatum: string;
  verzenddatum: string;
  bronorganisatie: string;
  vertrouwelijkheidaanduiding: string;
  auteur: string;
  status: GeneratedType<"StatusEnum">;
  formaat: FileFormat;
  taal: string;
  versie: number;
  informatieobjectTypeUUID: string;
  informatieobjectTypeOmschrijving: string;
  bestandsnaam: string;
  bestand: File;
  bestandsomvang: number;
  link: string;
  ondertekening: EnkelvoudigInformatieobjectOndertekening;
  indicatieGebruiksrecht: boolean;
  gelockedDoor: GeneratedType<"RestUser">;
  indicaties: InformatieobjectIndicatie[];
  rechten: DocumentRechten;
  isBesluitDocument: boolean;
}
