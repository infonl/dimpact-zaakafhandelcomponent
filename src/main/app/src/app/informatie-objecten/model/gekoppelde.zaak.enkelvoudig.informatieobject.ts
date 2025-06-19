/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { InformatieobjectIndicatie } from "../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { FileFormat } from "./file-format";

/**
 * @deprecated - use the `GeneratedType`
 */
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
  ondertekening: GeneratedType<"RESTOndertekening">;
  indicatieGebruiksrecht: boolean;
  gelockedDoor: GeneratedType<"RestUser">;
  indicaties: InformatieobjectIndicatie[];
  rechten: GeneratedType<"RestDocumentRechten">;
  isBesluitDocument: boolean;
}
