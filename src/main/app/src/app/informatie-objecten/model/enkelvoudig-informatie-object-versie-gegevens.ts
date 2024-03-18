/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Taal } from "../../configuratie/model/taal";
import { InformatieobjectStatus } from "./informatieobject-status.enum";

export class EnkelvoudigInformatieObjectVersieGegevens {
  uuid: string;
  zaakUuid: string;
  titel: string;
  vertrouwelijkheidaanduiding: string;
  auteur: string;
  status: InformatieobjectStatus;
  taal: Taal;
  bestandsnaam: string;
  formaat: string;
  file: File;
  beschrijving: string;
  verzenddatum: string;
  ontvangstdatum: string;
  toelichting: string;
}
