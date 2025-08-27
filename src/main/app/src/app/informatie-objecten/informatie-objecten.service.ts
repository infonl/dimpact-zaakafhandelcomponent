/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import moment from "moment";
import {
  DeleteBody,
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class InformatieObjectenService {
  private basepath = "/rest/informatieobjecten";

  constructor(private readonly zacHttpClient: ZacHttpClient) {}

  /**
   * Het EnkelvoudigInformatieobject kan opgehaald worden binnen de context van een specifieke zaak.
   */
  readEnkelvoudigInformatieobject(uuid: string, zaakUuid?: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}",
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  readEnkelvoudigInformatieobjectVersie(uuid: string, version: number) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/versie/{uuid}/{version}",
      {
        path: { uuid, version },
      },
    );
  }

  listInformatieobjecttypes(zaakTypeUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobjecttypes/{zaakTypeUuid}",
      {
        path: { zaakTypeUuid },
      },
    );
  }

  listInformatieobjecttypesForZaak(zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobjecttypes/zaak/{zaakUuid}",
      {
        path: { zaakUuid },
      },
    );
  }

  createEnkelvoudigInformatieobject(
    zaakUuid: string,
    documentReferenceId: string,
    infoObject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      bestand: File;
    },
    taakObject: boolean,
  ) {
    const formData = new FormData();
    for (const [key, value] of Object.entries(infoObject)) {
      if (value === undefined || value === null) continue;
      switch (key) {
        case "creatiedatum":
        case "ontvangstdatum":
        case "verzenddatum":
          formData.append(
            key,
            moment(value.toString()).format("YYYY-MM-DDThh:mmZ"),
          );
          break;
        case "bestand":
          formData.append("file", value as Blob, infoObject.bestandsnaam);
          break;
        default:
          formData.append(key, value.toString());
          break;
      }
    }

    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}",
      formData as unknown as PostBody<"/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}">,
      {
        path: { zaakUuid, documentReferenceId },
        query: { taakObject },
      },
    );
  }

  createDocumentAttended(
    documentCreationData: GeneratedType<"RestDocumentCreationAttendedData">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/document-creation/create-document-attended",
      documentCreationData,
    );
  }

  readHuidigeVersieEnkelvoudigInformatieObject(uuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}/huidigeversie",
      {
        path: { uuid },
      },
    );
  }

  updateEnkelvoudigInformatieobject(
    uuid: string,
    zaakUuid: string,
    infoObject: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">,
  ) {
    const formData = new FormData();
    const data = { ...infoObject, uuid, zaakUuid };
    for (const [key, value] of Object.entries(data)) {
      if (value === undefined || value === null) continue;
      switch (key) {
        case "ontvangstdatum":
        case "verzenddatum":
          formData.append(
            key,
            moment(value.toString()).format("YYYY-MM-DDThh:mmZ"),
          );
          break;
        case "file":
          formData.append(
            "file",
            value as unknown as Blob,
            infoObject.bestandsnaam,
          );
          break;
        case "taal":
          formData.append(key, JSON.stringify(value));
          break;
        default:
          formData.append(key, value.toString());
          break;
      }
    }

    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/update",
      formData as PostBody<"/rest/informatieobjecten/informatieobject/update">,
    );
  }

  listEnkelvoudigInformatieobjecten(
    body: PutBody<"/rest/informatieobjecten/informatieobjectenList">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/informatieobjecten/informatieobjectenList",
      body,
    );
  }

  readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(uuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/zaakinformatieobject/{uuid}/informatieobject",
      {
        path: { uuid },
      },
    );
  }

  listZaakInformatieobjecten(uuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}/zaakinformatieobjecten",
      {
        path: { uuid },
      },
    );
  }

  listInformatieobjectenVoorVerzenden(zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobjecten/zaak/{zaakUuid}/teVerzenden",
      {
        path: { zaakUuid },
      },
    );
  }

  verzenden(
    body: PostBody<"/rest/informatieobjecten/informatieobjecten/verzenden">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobjecten/verzenden",
      body,
    );
  }

  listHistorie(uuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}/historie",
      {
        path: { uuid },
      },
    );
  }

  lockInformatieObject(uuid: string, zaakUuid: string) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/lock",
      undefined as never,
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  unlockInformatieObject(uuid: string, zaakUuid: string) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/unlock",
      undefined as never,
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  ondertekenInformatieObject(uuid: string, zaakUuid: string) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/onderteken",
      undefined as never,
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  getDownloadURL(uuid: string, versie?: number): string {
    if (versie) {
      return `${this.basepath}/informatieobject/${uuid}/${versie}/download`;
    }
    return `${this.basepath}/informatieobject/${uuid}/download`;
  }

  getZIPDownload(body: PostBody<"/rest/informatieobjecten/download/zip">) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/download/zip",
      body,
      {
        responseType: "blob",
      } as Record<string, unknown>,
    );
  }

  getPreviewUrl(uuid: string, versie?: number): string {
    if (versie) {
      return `${this.basepath}/informatieobject/${uuid}/${versie}/preview`;
    }
    return `${this.basepath}/informatieobject/${uuid}/preview`;
  }

  editEnkelvoudigInformatieObjectInhoud(uuid: string, zaakUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}/edit",
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  linkDocumentToCase(
    body: PostBody<"/rest/informatieobjecten/informatieobject/verplaats">,
  ) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/verplaats",
      body,
    );
  }

  deleteEnkelvoudigInformatieObject(
    uuid: string,
    body: DeleteBody<"/rest/informatieobjecten/informatieobject/{uuid}">,
  ) {
    return this.zacHttpClient.DELETE(
      "/rest/informatieobjecten/informatieobject/{uuid}",
      { path: { uuid } },
      body,
    );
  }

  listZaakIdentificatiesForInformatieobject(informatieObjectUuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{informatieObjectUuid}/zaakidentificaties",
      {
        path: { informatieObjectUuid },
      },
    );
  }

  convertInformatieObjectToPDF(uuid: string, zaakUuid: string) {
    return this.zacHttpClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/convert",
      undefined as never,
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }
}
