/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import moment from "moment";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import {
  DeleteBody,
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { createFormData } from "../shared/utils/form-data";
import { GeneratedType } from "../shared/utils/generated-types";

const formatDateForFormData = ([k, v]: [string, string]) =>
  [k, v && moment(v).format("YYYY-MM-DDThh:mmZ")] as const;

@Injectable({
  providedIn: "root",
})
export class InformatieObjectenService {
  private basepath = "/rest/informatieobjecten";

  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  // Het EnkelvoudigInformatieobject kan opgehaald worden binnen de context van een specifieke zaak.
  readEnkelvoudigInformatieobject(uuid: string, zaakUuid?: string) {
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >(InformatieObjectenService.addZaakParameter(`${this.basepath}/informatieobject/${uuid}`, zaakUuid))
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readEnkelvoudigInformatieobjectVersie(uuid: string, versie: number) {
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >(`${this.basepath}/informatieobject/versie/${uuid}/${versie}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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

  listInformatieobjecttypesForZaak(zaakUUID: string) {
    return this.http
      .get<
        GeneratedType<"RestInformatieobjecttype">[]
      >(`${this.basepath}/informatieobjecttypes/zaak/${zaakUUID}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createEnkelvoudigInformatieobject(
    zaakUuid: string,
    documentReferentieId: string,
    infoObject: GeneratedType<"RestEnkelvoudigInformatieobject"> & {
      bestand: File;
    },
    taakObject: boolean,
  ) {
    const formData = createFormData(infoObject, {
      bestandsnaam: true,
      titel: true,
      bestandsomvang: true,
      formaat: true,
      informatieobjectTypeUUID: true,
      vertrouwelijkheidaanduiding: true,
      status: true,
      creatiedatum: formatDateForFormData,
      ontvangstdatum: formatDateForFormData,
      verzenddatum: formatDateForFormData,
      auteur: true,
      taal: true,
      beschrijving: true,
      bestand: ([, value]) => ["file", value, infoObject.bestandsnaam],
    });

    return this.http
      .post<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        `${this.basepath}/informatieobject/${zaakUuid}/${documentReferentieId}`,
        formData,
        {
          headers: {
            Accept: "application/json",
          },
          params: {
            taakObject,
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createDocumentAttended(
    documentCreationData: GeneratedType<"RestDocumentCreationAttendedData">,
  ) {
    return this.zacHttpClient
      .POST(
        "/rest/document-creation/create-document-attended",
        documentCreationData,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readHuidigeVersieEnkelvoudigInformatieObject(uuid: string) {
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">
      >(`${this.basepath}/informatieobject/${uuid}/huidigeversie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateEnkelvoudigInformatieobject(
    uuid: string,
    zaakUuid: string,
    infoObject: GeneratedType<"RestEnkelvoudigInformatieObjectVersieGegevens">,
  ) {
    const formData = createFormData(
      { ...infoObject, uuid, zaakUuid },
      {
        uuid: true,
        zaakUuid: true,
        titel: true,
        vertrouwelijkheidaanduiding: true,
        auteur: true,
        status: true,
        taal: ([k, v]) => [k, JSON.stringify(v)],
        bestandsnaam: true,
        formaat: true,
        file: ([k, v]) => [k, v as unknown as Blob, infoObject.bestandsnaam],
        beschrijving: true,
        verzenddatum: formatDateForFormData,
        ontvangstdatum: formatDateForFormData,
        toelichting: true,
        informatieobjectTypeUUID: true,
      },
    );

    return this.http
      .post<GeneratedType<"RestEnkelvoudigInformatieobject">>(
        `${this.basepath}/informatieobject/update`,
        formData,
        {
          headers: {
            Accept: "application/json",
          },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">
      >(`${this.basepath}/zaakinformatieobject/${uuid}/informatieobject`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">[]
      >(`${this.basepath}/informatieobjecten/zaak/${zaakUuid}/teVerzenden`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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
    return this.http
      .post<void>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}/lock`,
          zaakUuid,
        ),
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  unlockInformatieObject(uuid: string, zaakUuid: string) {
    return this.http
      .post<void>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}/unlock`,
          zaakUuid,
        ),
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  ondertekenInformatieObject(uuid: string, zaakUuid: string) {
    return this.http
      .post<void>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}/onderteken`,
          zaakUuid,
        ),
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getDownloadURL(uuid: string, versie?: number): string {
    if (versie) {
      return `${this.basepath}/informatieobject/${uuid}/${versie}/download`;
    }
    return `${this.basepath}/informatieobject/${uuid}/download`;
  }

  getZIPDownload(uuids: string[]): Observable<Blob> {
    return this.http
      .post(`${this.basepath}/download/zip`, uuids, { responseType: "blob" })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  getUploadURL(documentReferentieId: string): string {
    return `${this.basepath}/informatieobject/upload/${documentReferentieId}`;
  }

  getPreviewUrl(uuid: string, versie?: number): string {
    let url = `${this.basepath}/informatieobject/${uuid}/preview`;
    if (versie) {
      url = `${this.basepath}/informatieobject/${uuid}/${versie}/preview`;
    }
    return url;
  }

  editEnkelvoudigInformatieObjectInhoud(
    uuid: string,
    zaakUuid: string,
  ): Observable<string> {
    return this.http
      .get<string>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}/edit`,
          zaakUuid,
        ),
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  linkDocumentToCase(
    linkDocumentDetails: GeneratedType<"RESTDocumentVerplaatsGegevens">,
  ): Observable<void> {
    return this.http
      .post<void>(
        `${this.basepath}/informatieobject/verplaats`,
        linkDocumentDetails,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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

  listZaakIdentificatiesForInformatieobject(
    documentUUID: string,
  ): Observable<string[]> {
    return this.http
      .get<
        string[]
      >(`${this.basepath}/informatieobject/${documentUUID}/zaakidentificaties`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  convertInformatieObjectToPDF(uuid: string, zaakUuid: string) {
    return this.http
      .post<void>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}/convert`,
          zaakUuid,
        ),
        null,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  private static addZaakParameter(url: string, zaakUuid?: string): string {
    if (zaakUuid) {
      return url.concat(`?zaak=${zaakUuid}`);
    } else {
      return url;
    }
  }
}
