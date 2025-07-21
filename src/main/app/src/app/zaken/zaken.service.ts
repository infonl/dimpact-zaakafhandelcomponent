/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import {
  PatchBody,
  PostBody,
  PutBody,
  ZacHttpClient,
} from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";

@Injectable({
  providedIn: "root",
})
export class ZakenService {
  constructor(private readonly zacHttpClient: ZacHttpClient) {}

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

  createZaak(body: PostBody<"/rest/zaken/zaak">) {
    return this.zacHttpClient.POST("/rest/zaken/zaak", body);
  }

  updateZaak(
    uuid: string,
    update: {
      zaak: Omit<
        Partial<GeneratedType<"RestZaak">>,
        "zaakgeometrie" | "behandelaar"
      >;
      reden?: string;
    },
  ) {
    return this.zacHttpClient.PATCH(
      "/rest/zaken/zaak/{uuid}",
      {
        zaak: update.zaak as GeneratedType<"RestZaak">,
        reden: update.reden ?? "",
      },
      { path: { uuid } },
    );
  }

  readOpschortingZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/opschorting", {
      path: { uuid },
    });
  }

  opschortenZaak(
    uuid: string,
    body: PatchBody<"/rest/zaken/zaak/{uuid}/opschorting">,
  ) {
    return this.zacHttpClient.PATCH(
      "/rest/zaken/zaak/{uuid}/opschorting",
      body,
      {
        path: { uuid },
      },
    );
  }

  verlengenZaak(
    uuid: string,
    body: PatchBody<"/rest/zaken/zaak/{uuid}/verlenging">,
  ) {
    return this.zacHttpClient.PATCH(
      "/rest/zaken/zaak/{uuid}/verlenging",
      body,
      {
        path: { uuid },
      },
    );
  }

  listZaakWaarschuwingen() {
    return this.zacHttpClient.GET("/rest/zaken/waarschuwing", {});
  }

  listZaaktypes() {
    return this.zacHttpClient.GET("/rest/zaken/zaaktypes", {});
  }

  updateZaakdata(zaak: GeneratedType<"RestZaak">) {
    return this.zacHttpClient.PUT("/rest/zaken/zaakdata", zaak, {});
  }

  toekennen(body: PatchBody<"/rest/zaken/toekennen">) {
    return this.zacHttpClient.PATCH("/rest/zaken/toekennen", body, {});
  }

  verdelenVanuitLijst(body: PutBody<"/rest/zaken/lijst/verdelen">) {
    return this.zacHttpClient.PUT("/rest/zaken/lijst/verdelen", body, {});
  }

  vrijgevenVanuitLijst(body: PutBody<"/rest/zaken/lijst/vrijgeven">) {
    return this.zacHttpClient.PUT("/rest/zaken/lijst/vrijgeven", body, {});
  }

  toekennenAanIngelogdeMedewerker(body: PutBody<"/rest/zaken/toekennen/mij">) {
    return this.zacHttpClient.PUT("/rest/zaken/toekennen/mij", body, {});
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
    return this.zacHttpClient.POST("/rest/zaken/betrokkene", body, {});
  }

  deleteBetrokkene(rolUUID: string, reden: string) {
    return this.zacHttpClient.DELETE(
      "/rest/zaken/betrokkene/{uuid}",
      {
        path: { uuid: rolUUID },
      },
      { reden },
    );
  }

  updateZaakLocatie(
    uuid: string,
    reden: PatchBody<"/rest/zaken/{uuid}/zaaklocatie">["reden"] = "",
    geometrie?: PatchBody<"/rest/zaken/{uuid}/zaaklocatie">["geometrie"],
  ) {
    return this.zacHttpClient.PATCH(
      "/rest/zaken/{uuid}/zaaklocatie",
      { geometrie, reden },
      { path: { uuid } },
    );
  }

  ontkoppelInformatieObject(
    body: PutBody<"/rest/zaken/zaakinformatieobjecten/ontkoppel">,
  ) {
    return this.zacHttpClient.PUT(
      "/rest/zaken/zaakinformatieobjecten/ontkoppel",
      body,
      {},
    );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst(zaakUUID: string, reden?: string) {
    return this.zacHttpClient.PUT(
      "/rest/zaken/lijst/toekennen/mij",
      { zaakUUID, reden },
      {},
    );
  }

  listHistorieVoorZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/historie", {
      path: { uuid },
    });
  }

  listBetrokkenenVoorZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/betrokkene", {
      path: { uuid },
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

  createBesluit(body: PostBody<"/rest/zaken/besluit">) {
    return this.zacHttpClient.POST("/rest/zaken/besluit", body, {});
  }

  updateBesluit(body: PutBody<"/rest/zaken/besluit">) {
    return this.zacHttpClient.PUT("/rest/zaken/besluit", body, {});
  }

  intrekkenBesluit(body: PutBody<"/rest/zaken/besluit/intrekken">) {
    return this.zacHttpClient.PUT("/rest/zaken/besluit/intrekken", body, {});
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

  koppelZaak(body: PatchBody<"/rest/zaken/zaak/koppel">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/koppel", body, {});
  }

  ontkoppelZaak(body: PatchBody<"/rest/zaken/zaak/ontkoppel">) {
    return this.zacHttpClient.PATCH("/rest/zaken/zaak/ontkoppel", body, {});
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
    return this.zacHttpClient.GET("/rest/zaken/procesvariabelen", {});
  }
}
