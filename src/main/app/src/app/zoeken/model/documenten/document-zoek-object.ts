/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DocumentRechten } from "../../../policy/model/document-rechten";
import { InformatieobjectIndicatie } from "../../../shared/indicaties/informatie-object-indicaties/informatie-object-indicaties.component";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ZoekObject } from "../zoek-object";

/**
 * @deprecated - use the `GeneratedType`
 */
export class DocumentZoekObject implements ZoekObject {
  id: string;
  type: GeneratedType<"ZoekObjectType">;
  identificatie: string;
  titel: string;
  beschrijving: string;
  zaaktypeUuid: string;
  zaaktypeIdentificatie: string;
  zaaktypeOmschrijving: string;
  zaakIdentificatie: string;
  zaakUuid: string;
  zaakRelatie: string;
  creatiedatum: string;
  registratiedatum: string;
  ontvangstdatum: string;
  verzenddatum: string;
  ondertekeningDatum: string;
  ondertekeningSoort: string;
  vertrouwelijkheidaanduiding: string;
  auteur: string;
  status: string;
  formaat: string;
  versie: number;
  bestandsnaam: string;
  bestandsomvang: number;
  documentType: string;
  indicatieOndertekend: boolean;
  inhoudUrl: string;
  indicatieVergrendeld: boolean;
  indicatieGebruiksrecht: boolean;
  vergrendeldDoor: string;
  indicaties: InformatieobjectIndicatie[];
  rechten: DocumentRechten;
}
