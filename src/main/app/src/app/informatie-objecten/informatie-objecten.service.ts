/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import moment from "moment";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { HistorieRegel } from "../shared/historie/model/historie-regel";
import { createFormData } from "../shared/utils/form-data";
import { DocumentCreationData } from "./model/document-creation-data";
import { DocumentCreationResponse } from "./model/document-creation-response";
import { DocumentVerplaatsGegevens } from "./model/document-verplaats-gegevens";
import { DocumentVerwijderenGegevens } from "./model/document-verwijderen-gegevens";
import { DocumentVerzendGegevens } from "./model/document-verzend-gegevens";
import { EnkelvoudigInformatieObjectVersieGegevens } from "./model/enkelvoudig-informatie-object-versie-gegevens";
import { EnkelvoudigInformatieobject } from "./model/enkelvoudig-informatieobject";
import { InformatieobjectZoekParameters } from "./model/informatieobject-zoek-parameters";
import { Informatieobjecttype } from "./model/informatieobjecttype";
import { ZaakInformatieobject } from "./model/zaak-informatieobject";

const formatDateForFormData = ([k, v]: [string, string]) =>
  [k, v && moment(v).format("YYYY-MM-DDThh:mmZ")] as const;

@Injectable({
  providedIn: "root",
})
export class InformatieObjectenService {
  private basepath = "/rest/informatieobjecten";

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  // Het EnkelvoudigInformatieobject kan opgehaald worden binnen de context van een specifieke zaak.
  readEnkelvoudigInformatieobject(
    uuid: string,
    zaakUuid?: string,
  ): Observable<EnkelvoudigInformatieobject> {
    return this.http
      .get<EnkelvoudigInformatieobject>(
        InformatieObjectenService.addZaakParameter(
          `${this.basepath}/informatieobject/${uuid}`,
          zaakUuid,
        ),
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readEnkelvoudigInformatieobjectVersie(
    uuid: string,
    versie: number,
  ): Observable<EnkelvoudigInformatieobject> {
    return this.http
      .get<EnkelvoudigInformatieobject>(
        `${this.basepath}/informatieobject/versie/${uuid}/${versie}`,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listInformatieobjecttypes(zaakTypeID): Observable<Informatieobjecttype[]> {
    return this.http
      .get<
        Informatieobjecttype[]
      >(`${this.basepath}/informatieobjecttypes/${zaakTypeID}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listInformatieobjecttypesForZaak(
    zaakUUID,
  ): Observable<Informatieobjecttype[]> {
    return this.http
      .get<
        Informatieobjecttype[]
      >(`${this.basepath}/informatieobjecttypes/zaak/${zaakUUID}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createEnkelvoudigInformatieobject(
    zaakUuid: string,
    documentReferentieId: string,
    infoObject: EnkelvoudigInformatieobject,
    taakObject: boolean,
  ): Observable<EnkelvoudigInformatieobject> {
    const formData = createFormData(infoObject, {
      bestandsnaam: true,
      titel: true,
      bestandsomvang: true,
      formaat: true,
      informatieobjectTypeUUID: true,
      vertrouwelijkheidaanduiding: true,
      status: true,
      creatiedatum: formatDateForFormData,
      auteur: true,
      taal: true,
      beschrijving: true,
      bestand: ([, value]) => ["file", value, infoObject.bestandsnaam],
    });

    return this.http
      .post<EnkelvoudigInformatieobject>(
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
    documentCreationData: DocumentCreationData,
  ): Observable<DocumentCreationResponse> {
    return this.http
      .post<DocumentCreationResponse>(
        `rest/document-creation/create-document-attended`,
        documentCreationData,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readHuidigeVersieEnkelvoudigInformatieObject(
    uuid: string,
  ): Observable<EnkelvoudigInformatieObjectVersieGegevens> {
    return this.http
      .get<EnkelvoudigInformatieObjectVersieGegevens>(
        `${this.basepath}/informatieobject/${uuid}/huidigeversie`,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateEnkelvoudigInformatieobject(
    uuid: string,
    zaakUuid: string,
    infoObject: EnkelvoudigInformatieObjectVersieGegevens,
  ): Observable<EnkelvoudigInformatieobject> {
    const mergedInfoObject = {
      ...infoObject,
      uuid,
      zaakUuid,
    };
    const formData = createFormData(mergedInfoObject, {
      uuid: true,
      zaakUuid: true,
      titel: true,
      vertrouwelijkheidaanduiding: true,
      auteur: true,
      status: true,
      taal: ([k, v]) => [k, JSON.stringify(v)],
      bestandsnaam: true,
      formaat: true,
      file: ([k, v]) => [k, v, infoObject.bestandsnaam],
      beschrijving: true,
      verzenddatum: formatDateForFormData,
      ontvangstdatum: formatDateForFormData,
      toelichting: true,
      informatieobjectTypeUUID: true,
    });

    return this.http
      .post<EnkelvoudigInformatieobject>(
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
    zoekParameters: InformatieobjectZoekParameters,
  ): Observable<EnkelvoudigInformatieobject[]> {
    return this.http
      .put<
        EnkelvoudigInformatieobject[]
      >(`${this.basepath}/informatieobjectenList`, zoekParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readEnkelvoudigInformatieobjectByZaakInformatieobjectUUID(
    uuid: string,
  ): Observable<EnkelvoudigInformatieobject> {
    return this.http
      .get<EnkelvoudigInformatieobject>(
        `${this.basepath}/zaakinformatieobject/${uuid}/informatieobject`,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZaakInformatieobjecten(uuid: string): Observable<ZaakInformatieobject[]> {
    return this.http
      .get<
        ZaakInformatieobject[]
      >(`${this.basepath}/informatieobject/${uuid}/zaakinformatieobjecten`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listInformatieobjectenVoorVerzenden(
    zaakUuid: string,
  ): Observable<EnkelvoudigInformatieobject[]> {
    return this.http
      .get<
        EnkelvoudigInformatieobject[]
      >(`${this.basepath}/informatieobjecten/zaak/${zaakUuid}/teVerzenden`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  verzenden(gegevens: DocumentVerzendGegevens): Observable<void> {
    return this.http
      .post<DocumentCreationResponse>(
        `${this.basepath}/informatieobjecten/verzenden`,
        gegevens,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      ) as Observable<void>;
  }

  listHistorie(uuid: string): Observable<HistorieRegel[]> {
    return this.http
      .get<
        HistorieRegel[]
      >(`${this.basepath}/informatieobject/${uuid}/historie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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

  postVerplaatsDocument(
    documentVerplaatsGegevens: DocumentVerplaatsGegevens,
    nieuweZaakID: string,
  ): Observable<void> {
    documentVerplaatsGegevens.nieuweZaakID = nieuweZaakID;
    return this.http
      .post<void>(
        `${this.basepath}/informatieobject/verplaats`,
        documentVerplaatsGegevens,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteEnkelvoudigInformatieObject(
    uuid: string,
    zaakUuid: string,
    reden: string,
  ): Observable<void> {
    return this.http
      .delete<void>(`${this.basepath}/informatieobject/${uuid}`, {
        body: new DocumentVerwijderenGegevens(zaakUuid, reden),
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
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

  private static addZaakParameter(url: string, zaakUuid: string): string {
    if (zaakUuid) {
      return url.concat(`?zaak=${zaakUuid}`);
    } else {
      return url;
    }
  }
}
