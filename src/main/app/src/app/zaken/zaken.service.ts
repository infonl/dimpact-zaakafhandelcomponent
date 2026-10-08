/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { inject, Injectable } from "@angular/core";
import {
  type CreateMutationOptions,
  type MutationFunctionContext,
  mutationOptions,
  QueryClient,
  queryOptions,
} from "@tanstack/angular-query-experimental";
import { map } from "rxjs";
import { UtilService } from "../core/service/util.service";
import { PatchBody } from "../shared/http/http-client";
import { mergeMutationOptions } from "../shared/http/merge-mutation-options";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { ZacQueryClient } from "../shared/http/zac-query-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { toI18nKey } from "../shared/utils/i18n-key";

function withI18nKeySuffix(afzender: GeneratedType<"RestZaakAfzender">) {
  if (!afzender?.suffix) return afzender;
  return { ...afzender, suffix: toI18nKey(afzender.suffix) };
}

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
    | "isZaakspecifiekGeautoriseerd"
  >
>;

type ZaakDetailsUpdateVariables = {
  zaak: ZaakDetailsUpdate;
  reden: string;
};

@Injectable({
  providedIn: "root",
})
export class ZakenService {
  private readonly zacHttpClient = inject(ZacHttpClient);
  private readonly zacQueryClient = inject(ZacQueryClient);
  private readonly utilService = inject(UtilService);
  private readonly queryClient = inject(QueryClient);

  readZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}", {
      path: { uuid },
    });
  }

  readZaakQuery(uuid: string) {
    return this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}", {
      path: { uuid },
    });
  }

  cacheZaak(zaak: GeneratedType<"RestZaak">) {
    this.queryClient.setQueryData(this.readZaakQuery(zaak.uuid).queryKey, zaak);
  }

  invalidateZaak(uuid: string) {
    this.queryClient.invalidateQueries({
      queryKey: this.readZaakQuery(uuid).queryKey,
    });
  }

  /** Refetches the historie even when the zaak itself comes back unchanged. */
  private zaakMutation<TError, TVariables, TOnMutateResult>(
    base: CreateMutationOptions<
      GeneratedType<"RestZaak">,
      TError,
      TVariables,
      TOnMutateResult
    >,
  ) {
    return mergeMutationOptions(base, {
      onSuccess: (zaak) => {
        this.cacheZaak(zaak);
        this.invalidateHistorie(zaak.uuid);
      },
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

  updateZaak(uuid: string) {
    const patch = this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}", {
      path: { uuid },
    });
    // Endpoint accepts a partial zaak; the generated body type requires the
    // full RestZaakCreateData, so assert the partial here (one spot).
    const toBody = ({ zaak, reden }: ZaakDetailsUpdateVariables) => ({
      zaak: zaak as PatchBody<"/rest/zaken/zaak/{uuid}">["zaak"],
      reden,
    });

    return this.zaakMutation(
      mutationOptions<
        GeneratedType<"RestZaak">,
        HttpErrorResponse,
        ZaakDetailsUpdateVariables,
        void
      >({
        mutationKey: patch.mutationKey,
        mutationFn: (
          variables: ZaakDetailsUpdateVariables,
          context: MutationFunctionContext,
        ) => patch.mutationFn!(toBody(variables), context),
        onError: (error, variables, onMutateResult, context) =>
          patch.onError?.(error, toBody(variables), onMutateResult, context),
      }),
    );
  }

  readOpschortingZaak(uuid: string) {
    return this.zacHttpClient.GET("/rest/zaken/zaak/{uuid}/opschorting", {
      path: { uuid },
    });
  }

  suspendZaak(uuid: string) {
    return mergeMutationOptions(
      this.zaakMutation(
        this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/suspend", {
          path: { uuid },
        }),
      ),
      { onSuccess: () => this.utilService.openSnackbar("msg.zaak.opgeschort") },
    );
  }

  resumeZaak(uuid: string) {
    return mergeMutationOptions(
      this.zaakMutation(
        this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/resume", {
          path: { uuid },
        }),
      ),
      { onSuccess: () => this.utilService.openSnackbar("msg.zaak.hervat") },
    );
  }

  verlengenZaak(uuid: string) {
    return this.zaakMutation(
      this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/verlenging", {
        path: { uuid },
      }),
    );
  }

  listZaakWaarschuwingen() {
    return this.zacHttpClient.GET("/rest/zaken/waarschuwing");
  }

  listZaaktypesForCreation() {
    return this.zacHttpClient.GET("/rest/zaken/zaaktypes-for-creation");
  }

  listZaaktypesToLinkQuery() {
    return this.zacQueryClient.GET("/rest/zaken/gekoppelde-zaken/zaaktypen");
  }

  verdelenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/zaken/lijst/verdelen");
  }

  vrijgevenVanuitLijst() {
    return this.zacQueryClient.PUT("/rest/zaken/lijst/vrijgeven");
  }

  toekennenAanIngelogdeMedewerker() {
    return this.zacQueryClient.PUT("/rest/zaken/toekennen/mij");
  }

  updateInitiator() {
    return this.zaakMutation(
      this.zacQueryClient.PATCH("/rest/zaken/initiator"),
    );
  }

  deleteInitiator() {
    return this.zaakMutation(
      this.zacQueryClient.DELETE(
        "/rest/zaken/{uuid}/initiator",
        ({ zaakUuid, reden }: { zaakUuid: string; reden: string }) => ({
          parameters: { path: { uuid: zaakUuid } },
          body: { reden },
        }),
      ),
    );
  }

  createBetrokkene() {
    return this.zaakMutation(
      this.zacQueryClient.POST("/rest/zaken/betrokkene"),
    );
  }

  deleteBetrokkene() {
    return this.zaakMutation(
      this.zacQueryClient.DELETE(
        "/rest/zaken/betrokkene/{uuid}",
        ({ rolUuid, reden }: { rolUuid: string; reden: string }) => ({
          parameters: { path: { uuid: rolUuid } },
          body: { reden },
        }),
      ),
    );
  }

  updateZaakLocatie(uuid: string) {
    return this.zaakMutation(
      this.zacQueryClient.PATCH("/rest/zaken/{uuid}/zaaklocatie", {
        path: { uuid },
      }),
    );
  }

  ontkoppelInformatieObject(
    informatieobject: GeneratedType<"RestEnkelvoudigInformatieobject">,
  ) {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaken/zaakinformatieobjecten/ontkoppel"),
      {
        onSuccess: () =>
          this.utilService.openSnackbar("msg.document.ontkoppelen.uitgevoerd", {
            document: informatieobject.titel,
          }),
      },
    );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaken/lijst/toekennen/mij"),
      {
        onSuccess: (zaak) =>
          this.utilService.openSnackbar("msg.zaak.toegekend", {
            behandelaar: zaak.behandelaar?.naam,
          }),
      },
    );
  }

  listHistorieVoorZaakQuery(uuid: string) {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}/historie", {
        path: { uuid },
      }),
    });
  }

  invalidateHistorie(uuid: string) {
    this.queryClient.invalidateQueries(
      { queryKey: this.listHistorieVoorZaakQuery(uuid).queryKey },
      { cancelRefetch: false },
    );
  }

  listZaakspecifiekGeautoriseerdeMedewerkerKandidatenQuery(
    uuid: string,
    groepId: string,
  ) {
    return queryOptions({
      ...this.zacQueryClient.GET(
        "/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten",
        { path: { uuid }, query: { groepId } },
      ),
    });
  }

  addZaakspecifiekGeautoriseerdeMedewerker(uuid: string) {
    return mergeMutationOptions(
      this.zacQueryClient.POST(
        "/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers",
        { path: { uuid } },
      ),
      {
        onSuccess: () => {
          this.queryClient.invalidateQueries({
            queryKey: [
              "/rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten",
            ],
          });
          this.invalidateHistorie(uuid);
        },
      },
    );
  }

  listBetrokkenenVoorZaakQuery(uuid: string) {
    return queryOptions({
      ...this.zacQueryClient.GET("/rest/zaken/zaak/{uuid}/betrokkene", {
        path: { uuid },
      }),
    });
  }

  listAfzendersVoorZaak(uuid: string) {
    return this.zacHttpClient
      .GET("/rest/zaken/zaak/{uuid}/afzender", { path: { uuid } })
      .pipe(map((afzenders) => afzenders.map(withI18nKeySuffix)));
  }

  readDefaultAfzenderVoorZaak(uuid: string) {
    return this.zacHttpClient
      .GET("/rest/zaken/zaak/{uuid}/afzender/default", { path: { uuid } })
      .pipe(map(withI18nKeySuffix));
  }

  afbreken(uuid: string) {
    return mergeMutationOptions(
      this.zaakMutation(
        this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/afbreken", {
          path: { uuid },
        }),
      ),
      { onSuccess: () => this.utilService.openSnackbar("msg.zaak.afgebroken") },
    );
  }

  heropenen(uuid: string) {
    return mergeMutationOptions(
      this.zaakMutation(
        this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/heropenen", {
          path: { uuid },
        }),
      ),
      { onSuccess: () => this.utilService.openSnackbar("msg.zaak.heropend") },
    );
  }

  afsluitenMutation(uuid: string) {
    return this.zaakMutation(
      this.zacQueryClient.PATCH("/rest/zaken/zaak/{uuid}/afsluiten", {
        path: { uuid },
      }),
    );
  }

  createBesluit() {
    return mergeMutationOptions(
      this.zacQueryClient.POST("/rest/zaken/besluit"),
      {
        onSuccess: () =>
          this.utilService.openSnackbar("msg.besluit.vastgelegd"),
      },
    );
  }

  updateBesluit() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaken/besluit"),
      {
        onSuccess: () => this.utilService.openSnackbar("msg.besluit.gewijzigd"),
      },
    );
  }

  intrekkenBesluit() {
    return mergeMutationOptions(
      this.zacQueryClient.PUT("/rest/zaken/besluit/intrekken"),
      {
        onSuccess: () =>
          this.utilService.openSnackbar("msg.besluit.ingetrokken"),
      },
    );
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

  koppelZaakMutation() {
    return this.zacQueryClient.PATCH("/rest/zaken/zaak/koppel");
  }

  ontkoppelZaak() {
    return mergeMutationOptions(
      this.zacQueryClient.PATCH("/rest/zaken/zaak/ontkoppel"),
      {
        onSuccess: (_data, { zaakUuid }) => {
          this.invalidateZaak(zaakUuid);
          this.utilService.openSnackbar("msg.zaak.ontkoppelen.uitgevoerd");
        },
      },
    );
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
