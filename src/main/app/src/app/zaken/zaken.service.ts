/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient, HttpParams } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { ZaakbeeindigReden } from "../admin/model/zaakbeeindig-reden";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { Klant } from "../klanten/model/klanten/klant";
import { TableRequest } from "../shared/dynamic-table/datasource/table-request";
import { HistorieRegel } from "../shared/historie/model/historie-regel";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZaakZoekObject } from "../zoeken/model/zaken/zaak-zoek-object";
import { Zaak } from "./model/zaak";
import { ZaakAfbrekenGegevens } from "./model/zaak-afbreken-gegevens";
import { ZaakAfsluitenGegevens } from "./model/zaak-afsluiten-gegevens";
import { ZaakBetrokkene } from "./model/zaak-betrokkene";
import { ZaakHeropenenGegevens } from "./model/zaak-heropenen-gegevens";

@Injectable({
  providedIn: "root",
})
export class ZakenService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  private basepath = "/rest/zaken";

  private static getTableParams(request: TableRequest): HttpParams {
    return new HttpParams().set("tableRequest", JSON.stringify(request));
  }

  readZaak(uuid: string) {
    return this.zacHttpClient
      .GET("/rest/zaken/zaak/{uuid}", {
        pathParams: { path: { uuid } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readZaakByID(identificatie: string) {
    return this.zacHttpClient
      .GET("/rest/zaken/zaak/id/{identificatie}", {
        pathParams: { path: { identificatie } },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createZaak(data: GeneratedType<"RESTZaakAanmaakGegevens">) {
    return this.zacHttpClient
      .POST("/rest/zaken/zaak", data)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
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
    return this.zacHttpClient
      .PATCH(
        "/rest/zaken/zaak/{uuid}",
        {
          zaak: update.zaak as GeneratedType<"RestZaak">,
          reden: update.reden ?? "",
        },
        {
          pathParams: { path: { uuid } },
        },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readOpschortingZaak(
    uuid: string,
  ): Observable<GeneratedType<"RESTZaakOpschorting">> {
    return this.http
      .get<
        GeneratedType<"RESTZaakOpschorting">
      >(`${this.basepath}/zaak/${uuid}/opschorting`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  opschortenZaak(
    uuid: string,
    zaakOpschortGegevens: GeneratedType<"RESTZaakOpschortGegevens">,
  ): Observable<Zaak> {
    return this.http
      .patch<Zaak>(
        `${this.basepath}/zaak/${uuid}/opschorting`,
        zaakOpschortGegevens,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  verlengenZaak(
    zaakUUID: string,
    zaakVerlengGegevens: GeneratedType<"RESTZaakVerlengGegevens">,
  ): Observable<Zaak> {
    return this.http
      .patch<Zaak>(
        `${this.basepath}/zaak/${zaakUUID}/verlenging`,
        zaakVerlengGegevens,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZaakWaarschuwingen() {
    return this.http
      .get<
        GeneratedType<"RestZaakOverzicht">[]
      >(`${this.basepath}/waarschuwing`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZaaktypes() {
    return this.zacHttpClient
      .GET("/rest/zaken/zaaktypes")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateZaakdata(zaak: GeneratedType<"RestZaak">) {
    return this.zacHttpClient
      .PUT("/rest/zaken/zaakdata", zaak)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennen(
    zaakUUID: string,
    options?: { behandelaarId?: string; groupId?: string; reason?: string },
  ) {
    return this.zacHttpClient.PATCH("/rest/zaken/toekennen", {
      zaakUUID,
      groepId: options?.groupId || "", // TODO: check this interface
      behandelaarGebruikersnaam: options?.behandelaarId,
      reden: options?.reason,
    });
  }

  verdelenVanuitLijst(
    uuids: string[],
    screenEventResourceId: string,
    groep?: GeneratedType<"RestGroup">,
    medewerker?: GeneratedType<"RestUser">,
    reden?: string,
  ) {
    return this.zacHttpClient
      .PUT("/rest/zaken/lijst/verdelen", {
        uuids,
        groepId: groep?.id ?? "", // TODO: check generated interface
        reden,
        screenEventResourceId,
        behandelaarGebruikersnaam: medewerker?.id,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  vrijgevenVanuitLijst(uuids: string[], reden?: string): Observable<void> {
    return this.zacHttpClient
      .PUT("/rest/zaken/lijst/vrijgeven", {
        uuids,
        reden,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennenAanIngelogdeMedewerker(zaakUUID: string, reden?: string) {
    return this.zacHttpClient
      .PUT("/rest/zaken/toekennen/mij", {
        zaakUUID,
        reden,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateInitiator(
    zaak: GeneratedType<"RestZaak">,
    initiator: GeneratedType<"RestPersoon">,
    reason?: string,
  ) {
    return this.zacHttpClient
      .PATCH("/rest/zaken/initiator", {
        zaakUUID: zaak.uuid,
        betrokkeneIdentificatieType: initiator.identificatieType!,
        betrokkeneIdentificatie: initiator.identificatie!,
        roltypeUUID: undefined as unknown as string, // TODO: check this interface
        roltoelichting: reason,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteInitiator(zaak: GeneratedType<"RestZaak">, reden: string) {
    return this.zacHttpClient
      .DELETE("/rest/zaken/{uuid}/initiator", {
        pathParams: { path: { uuid: zaak.uuid } },
        body: { reden },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createBetrokkene(
    zaak: GeneratedType<"RestZaak">,
    betrokkene: Klant,
    roltype: GeneratedType<"RestRoltype">,
    roltoelichting: string,
  ) {
    return this.zacHttpClient
      .POST("/rest/zaken/betrokkene", {
        zaakUUID: zaak.uuid,
        roltypeUUID: roltype.uuid!,
        roltoelichting,
        betrokkeneIdentificatieType: betrokkene.identificatieType,
        betrokkeneIdentificatie: betrokkene.identificatie,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  deleteBetrokkene(rolUUID: string, reden: string): Observable<Zaak> {
    return this.http
      .delete<Zaak>(`${this.basepath}/betrokkene/${rolUUID}`, {
        body: { reden },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateZaakLocatie(
    uuid: string,
    reden = "",
    geometrie?: GeneratedType<"RestGeometry">,
  ) {
    return this.zacHttpClient
      .PATCH(
        "/rest/zaken/{uuid}/zaaklocatie",
        { geometrie, reden },
        { pathParams: { path: { uuid } } },
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  ontkoppelInformatieObject(
    gegevens: GeneratedType<"RESTDocumentOntkoppelGegevens">,
  ): Observable<void> {
    return this.http
      .put<void>(`${this.basepath}/zaakinformatieobjecten/ontkoppel`, gegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  toekennenAanIngelogdeMedewerkerVanuitLijst(
    zaak: ZaakZoekObject,
    reden?: string,
  ) {
    return this.zacHttpClient
      .PUT("/rest/zaken/lijst/toekennen/mij", {
        zaakUUID: zaak.id,
        reden,
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listHistorieVoorZaak(uuid: string): Observable<HistorieRegel[]> {
    return this.http
      .get<HistorieRegel[]>(`${this.basepath}/zaak/${uuid}/historie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBetrokkenenVoorZaak(uuid: string): Observable<ZaakBetrokkene[]> {
    return this.http
      .get<ZaakBetrokkene[]>(`${this.basepath}/zaak/${uuid}/betrokkene`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listAfzendersVoorZaak(zaakUuid: string) {
    return this.http
      .get<
        GeneratedType<"RESTZaakAfzender">[]
      >(`${this.basepath}/zaak/${zaakUuid}/afzender`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readDefaultAfzenderVoorZaak(zaakUuid: string) {
    return this.http
      .get<
        GeneratedType<"RESTZaakAfzender">
      >(`${this.basepath}/zaak/${zaakUuid}/afzender/default`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  afbreken(uuid: string, beeindigReden: ZaakbeeindigReden): Observable<void> {
    return this.http
      .patch<void>(
        `${this.basepath}/zaak/${uuid}/afbreken`,
        new ZaakAfbrekenGegevens(beeindigReden.id),
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  heropenen(uuid: string, heropenReden: string): Observable<void> {
    return this.http
      .patch<void>(
        `${this.basepath}/zaak/${uuid}/heropenen`,
        new ZaakHeropenenGegevens(heropenReden),
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  afsluiten(
    uuid: string,
    afsluitenReden: string,
    resultaattypeUuid: string,
  ): Observable<void> {
    return this.http
      .patch<void>(
        `${this.basepath}/zaak/${uuid}/afsluiten`,
        new ZaakAfsluitenGegevens(afsluitenReden, resultaattypeUuid),
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  createBesluit(
    besluitVestleggenGegevens: GeneratedType<"RestDecisionCreateData">,
  ) {
    return this.http
      .post<
        GeneratedType<"RestDecision">
      >(`${this.basepath}/besluit`, besluitVestleggenGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateBesluit(
    besluitWijzigenGegevens: GeneratedType<"RestDecisionChangeData">,
  ) {
    return this.http
      .put<
        GeneratedType<"RestDecision">
      >(`${this.basepath}/besluit`, besluitWijzigenGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  intrekkenBesluit(
    besluitIntrekkenGegevens: GeneratedType<"RestDecisionWithdrawalData">,
  ) {
    return this.http
      .put<
        GeneratedType<"RestDecision">
      >(`${this.basepath}/besluit/intrekken`, besluitIntrekkenGegevens)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBesluittypes(zaaktypeUuid: string) {
    return this.http
      .get<
        GeneratedType<"RestDecisionType">[]
      >(`${this.basepath}/besluittypes/${zaaktypeUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listResultaattypes(zaaktypeUuid: string) {
    return this.http
      .get<
        GeneratedType<"RestResultaattype">[]
      >(`${this.basepath}/resultaattypes/${zaaktypeUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  koppelZaak(
    restZaakLinkData: GeneratedType<"RestZaakLinkData">,
  ): Observable<void> {
    return this.http
      .patch<void>(`${this.basepath}/zaak/koppel`, restZaakLinkData)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  ontkoppelZaak(
    restZaakUnlinkData: GeneratedType<"RestZaakUnlinkData">,
  ): Observable<void> {
    return this.http
      .patch<void>(`${this.basepath}/zaak/ontkoppel`, restZaakUnlinkData)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBesluitInformatieobjecten(besluitUuid: string) {
    return this.http
      .get<
        GeneratedType<"RestEnkelvoudigInformatieobject">[]
      >(`${this.basepath}/listBesluitInformatieobjecten/${besluitUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBesluitenForZaak(zaakUuid: string) {
    return this.http
      .get<
        GeneratedType<"RestDecision">[]
      >(`${this.basepath}/besluit/zaakUuid/${zaakUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listBesluitHistorie(uuid: string): Observable<HistorieRegel[]> {
    return this.http
      .get<HistorieRegel[]>(`${this.basepath}/besluit/${uuid}/historie`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listProcesVariabelen(): Observable<string[]> {
    return this.http
      .get<string[]>(`${this.basepath}/procesvariabelen`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
