/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
import { HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import type { CreateMutationOptions } from "@tanstack/angular-query-experimental";
import { UtilService } from "../core/service/util.service";
import { DeleteBody, PostBody, PutBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";

@Injectable({
  providedIn: "root",
})
export class InformatieObjectenService {
  private basepath = "/rest/informatieobjecten";
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);

  readEnkelvoudigInformatieobject(uuid: string) {
    return this.zacHttpClient.GET(
      "/rest/informatieobjecten/informatieobject/{uuid}",
      {
        path: { uuid },
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
    taakObject: boolean,
  ) {
    return mergeMutationOptions(
      this.zacQueryClient.POST_WITH_PROGRESS(
        "/rest/informatieobjecten/informatieobject/{zaakUuid}/{documentReferenceId}",
        {
          path: { zaakUuid, documentReferenceId },
          query: { taakObject },
        },
      ),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.document.nieuwe.versie.toegevoegd",
          ),
      },
    );
  }

  createDocumentAttendedMutation() {
    return this.zacQueryClient.POST(
      "/rest/document-creation/create-document-attended",
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

  updateEnkelvoudigInformatieobject(uuid: string, zaakUuid: string) {
    return mergeMutationOptions(
      this.zacQueryClient.PUT_WITH_PROGRESS(
        "/rest/informatieobjecten/informatieobject/{uuid}",
        {
          path: { uuid },
          query: { zaak: zaakUuid },
        },
      ),
      {
        onSuccess: () =>
          this.utilService.openSnackbar(
            "msg.document.nieuwe.versie.toegevoegd",
          ),
      },
    );
  }

  listEnkelvoudigInformatieobjecten(
    body: PutBody<"/rest/informatieobjecten/informatieobjectenList">,
  ) {
    return this.zacQueryClient.PUT_QUERY(
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

  listInformatieobjectenVoorVerzendenQuery(zaakUuid: string) {
    return this.zacQueryClient.GET(
      "/rest/informatieobjecten/informatieobjecten/zaak/{zaakUuid}/teVerzenden",
      {
        path: { zaakUuid },
      },
    );
  }

  verzenden() {
    return mergeMutationOptions(
      this.zacQueryClient.POST(
        "/rest/informatieobjecten/informatieobjecten/verzenden",
      ),
      {
        onSuccess: (_data, { informatieobjecten }) =>
          this.utilService.openSnackbar(
            informatieobjecten.length > 1
              ? "msg.documenten.verzenden.uitgevoerd"
              : "msg.document.verzenden.uitgevoerd",
          ),
      },
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
    return this.zacQueryClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/lock",
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  unlockInformatieObject(uuid: string, zaakUuid?: string) {
    return this.zacQueryClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/unlock",
      {
        path: { uuid },
        query: { zaak: zaakUuid ?? null },
      },
    );
  }

  ondertekenInformatieObject(uuid: string, zaakUuid: string) {
    return this.zacQueryClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/onderteken",
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }

  getDownloadURL(uuid: string, versie?: number | null): string {
    if (versie) {
      return `${this.basepath}/informatieobject/${uuid}/${versie}/download`;
    }
    return `${this.basepath}/informatieobject/${uuid}/download`;
  }

  /**
   * The zip is a file rather than JSON, so `responseType` rides along with the
   * parameters to reach the Angular `HttpClient`, and the response is typed as
   * the `Blob` it is at runtime.
   */
  getZIPDownload() {
    return this.zacQueryClient.POST("/rest/informatieobjecten/download/zip", {
      responseType: "blob",
    } as Record<string, unknown>) as unknown as CreateMutationOptions<
      Blob,
      HttpErrorResponse,
      PostBody<"/rest/informatieobjecten/download/zip">,
      void
    >;
  }

  getPreviewUrl(uuid: string, versie?: number | null): string {
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

  linkDocumentToCaseMutation() {
    return this.zacQueryClient.POST(
      "/rest/informatieobjecten/informatieobject/verplaats",
    );
  }

  deleteEnkelvoudigInformatieObject() {
    return this.zacQueryClient.DELETE(
      "/rest/informatieobjecten/informatieobject/{uuid}",
      ({
        uuid,
        ...body
      }: {
        uuid: string;
      } & DeleteBody<"/rest/informatieobjecten/informatieobject/{uuid}">) => ({
        parameters: { path: { uuid } },
        body,
      }),
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
    return this.zacQueryClient.POST(
      "/rest/informatieobjecten/informatieobject/{uuid}/convert",
      {
        path: { uuid },
        query: { zaak: zaakUuid },
      },
    );
  }
}
