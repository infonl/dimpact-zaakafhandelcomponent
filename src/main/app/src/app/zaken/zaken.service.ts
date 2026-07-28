/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import {
  mutationOptions,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { lastValueFrom } from "rxjs";
import { PatchBody, PostBody, PutBody } from "../shared/http/http-client";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";

/** Fields the "zaakgegevens bewerken" form may update; all optional (partial PATCH). */
type ZaakDetailsUpdate = Partial<
  Pick<
    GeneratedType<"RestZaakCreateData">,
    | "groep"
    | "behandelaar"
    | "communicatiekanaal"
    | "startdatum"
    | "einddatumGepland"
    | "uiterlijkeEinddatumAfdoening"
    | "vertrouwelijkheidaanduiding"
    | "omschrijving"
    | "toelichting"
  >
>;

@Injectable({
  providedIn: "root",
})
export class ZakenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);

  readZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}", {
      path: { uuid },
    });
  }

  readZaakByID(identificatie: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/id/{identificatie}", {
      path: { identificatie },
    });
  }

  createZaak() {
    return this.zacQueryClient.POST("/rest/zaken/zaak");
  }

  updateMutation() {
    return mutationOptions({
      mutationKey: ["/rest/zaken/zaak/{uuid}"],
      mutationFn: (variables: {
        uuid: string;
        zaak: ZaakDetailsUpdate;
        reden: string;
      }) =>
        lastValueFrom(
          this.zacHttpClient.PATCH(
            "/rest/zaken/zaak/{uuid}",
            // Endpoint accepts a partial zaak; the generated body type requires
            // the full RestZaakCreateData, so assert the partial here (one spot).
            {
              zaak: variables.zaak as PatchBody<"/rest/zaken/zaak/{uuid}">["zaak"],
              reden: variables.reden,
            },
            { path: { uuid: variables.uuid } },
          ),
        ),
    });
  }

  readOpschortingZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/opschorting", {
      path: { uuid },
    });
  }

  suspendZaak(
    uuid: string,
    body: PatchBody<"/rest/zaken/zaak/{uuid}/suspend">,
  ) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/{uuid}/suspend", body, {
      path: { uuid },
    });
  }

  resumeZaak(uuid: string, body: PatchBody<"/rest/zaken/zaak/{uuid}/resume">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/{uuid}/resume", body, {
      path: { uuid },
    });
  }

  verlengenZaak(uuid: string) {
    return this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/verlenging", {
      path: { uuid },
    });
  }

  listZaakWaarschuwingen() {
    return this.zacHttpClient.GET("/rest/zaken/waarschuwing");
  }

  listZaaktypesForCreation() {
    return this.zacHttpClient.GET("/rest/zaken/zaaktypes-for-creation");
  }

  listZaaktypesToLink() {
    return this.zacHttpClient.GET("/rest/zaken/gekoppelde-zaken/zaaktypen");
  }

  updateZaakdata() {
    return this.zacQueryClient.PUT("/rest/zaken/zaakdata");
  }

  toekennen(body: PatchBody<"/rest/zaken/toekennen">) {
    return this.zacHttpClient.PATCH("/rest/zaken/toekennen", body);
  }

  verdelenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/zaken/lijst/verdelen");
  }

  vrijgevenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/zaken/lijst/vrijgeven");
  }

  toekennenAanIngelogdeMedewerker(body: PutBody<"/rest/zaken/toekennen/mij">) {
    return this.zacHttpClient.PUT("/rest/zaken/toekennen/mij", body);
  }

  updateInitiator(body: PatchBody<"/rest/zaken/initiator">) {
    return this.zacHttpClient.PATCH("/rest/zaken/initiator", body, {});
  }

  deleteInitiator(zaakUuid: string, reden: string) {
    return this.zacHttpClient.DELETE(
      "/rest/zaken/{uuid}/initiator",
      { path: { uuid: zaakUuid } },
      { reden },
    );
  }

  createBetrokkene(body: PostBody<"/rest/zaken/betrokkene">) {
    return this.zacHttpClient.POST("/rest/zaken/betrokkene", body);
  }

  deleteBetrokkene(rolUUID: string, reden: string) {
    return this.zacHttpClient.DELETE(
      "/rest/zaken/betrokkene/{uuid}",
      { path: { uuid: rolUUID } },
      { reden },
    );
  }

  updateZaakLocatie(uuid: string) {
    return this.zacQueryClient.PATCH("/rest/zaken/{uuid}/zaaklocatie", {
      path: { uuid },
    });
  }

  ontkoppelInformatieObject(
    body: PutBody<"/rest/zaken/zaakinformatieobjecten/ontkoppel">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/zaken/zaakinformatieobjecten/ontkoppel",
      body,
    );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst(
    zaakUUID: string,
    groepId: string,
    reden?: string,
  ) {
    return this.zacHttpClient.PUT("/rest/zaken/lijst/toekennen/mij", {
      zaakUUID,
      groepId,
      reden,
    });
  }

  listHistorieVoorZaakQuery(uuid: string) {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}/historie", {
        path: { uuid },
      }),
    });
  }

  listBetrokkenenVoorZaakQuery(uuid: string) {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}/betrokkene", {
        path: { uuid },
      }),
    });
  }

  listAfzendersVoorZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/afzender", {
      path: { uuid },
    });
  }

  readDefaultAfzenderVoorZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/afzender/default", {
      path: { uuid },
    });
  }

  afbreken(uuid: string, body: PatchBody<"/rest/zaken/zaak/{uuid}/afbreken">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/{uuid}/afbreken", body, {
      path: { uuid },
    });
  }

  heropenen(
    uuid: string,
    body: PatchBody<"/rest/zaken/zaak/{uuid}/heropenen">,
  ) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/{uuid}/heropenen", body, {
      path: { uuid },
    });
  }

  afsluiten(
    uuid: string,
    body: PatchBody<"/rest/zaken/zaak/{uuid}/afsluiten">,
  ) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/{uuid}/afsluiten", body, {
      path: { uuid },
    });
  }

  createBesluit() {
    return this.zacQueryClient.POST("/rest/zaken/besluit");
  }

  updateBesluit() {
    return this.zacQueryClient.PUT("/rest/zaken/besluit");
  }

  intrekkenBesluit() {
    return this.zacQueryClient.PUT("/rest/zaken/besluit/intrekken");
  }

  listBesluittypes(zaaktypeUUID: string) {
    return this.zacHttpClient.GET("/rest/zaken/besluittypes/{zaaktypeUUID}", {
      path: { zaaktypeUUID },
    });
  }

  listResultaattypes(zaaktypeUUID: string) {
    return this.zacHttpClient.GET("/rest/zaken/resultaattypes/{zaaktypeUUID}", {
      path: { zaaktypeUUID },
    });
  }

  listStatustypes(zaaktypeUUID: string) {
    return this.zacQueryClient.GET("/rest/zaken/statustypes/{zaaktypeUUID}", {
      path: { zaaktypeUUID },
    });
  }

  koppelZaak(body: PatchBody<"/rest/zaken/zaak/koppel">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/koppel", body);
  }

  ontkoppelZaak(body: PatchBody<"/rest/zaken/zaak/ontkoppel">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/ontkoppel", body);
  }

  listBesluitenForZaak(zaakUuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/besluit/zaakUuid/{zaakUuid}", {
      path: { zaakUuid },
    });
  }

  listBesluitHistorie(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/besluit/{uuid}/historie", {
      path: { uuid },
    });
  }

  listProcesVariabelen() {
    return this.zacQueryClient.GET("/rest/zaken/procesvariabelen");
  }
}
