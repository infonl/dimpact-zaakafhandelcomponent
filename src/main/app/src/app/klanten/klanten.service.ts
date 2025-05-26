/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { PutBody, ZacHttpClient } from "../shared/http/zac-http-client";
import { Resultaat } from "../shared/model/resultaat";
import { BSN_LENGTH } from "../shared/utils/constants";
import { GeneratedType } from "../shared/utils/generated-types";
import { Bedrijf } from "./model/bedrijven/bedrijf";
import { ListBedrijvenParameters } from "./model/bedrijven/list-bedrijven-parameters";
import { Vestigingsprofiel } from "./model/bedrijven/vestigingsprofiel";
import { ContactGegevens } from "./model/klanten/contact-gegevens";

@Injectable({
  providedIn: "root",
})
export class KlantenService {
  constructor(
    private readonly http: HttpClient,
    private readonly foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  private basepath = "/rest/klanten";

  /* istanbul ignore next */
  readPersoon(bsn: string): Observable<GeneratedType<"RestPersoon">> {
    return this.http
      .get<GeneratedType<"RestPersoon">>(`${this.basepath}/persoon/${bsn}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readBedrijf(rsinOfVestigingsnummer: string): Observable<Bedrijf> {
    return rsinOfVestigingsnummer.length === BSN_LENGTH
      ? this.readRechtspersoon(rsinOfVestigingsnummer)
      : this.readVestiging(rsinOfVestigingsnummer);
  }

  /* istanbul ignore next */
  readVestiging(vestigingsnummer: string): Observable<Bedrijf> {
    return this.http
      .get<Bedrijf>(`${this.basepath}/vestiging/${vestigingsnummer}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  readVestigingsprofiel(
    vestigingsnummer: string,
  ): Observable<Vestigingsprofiel> {
    return this.http
      .get<Vestigingsprofiel>(
        `${this.basepath}/vestigingsprofiel/${vestigingsnummer}`,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  readRechtspersoon(rsin: string): Observable<Bedrijf> {
    return this.http
      .get<Bedrijf>(`${this.basepath}/rechtspersoon/${rsin}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  getPersonenParameters() {
    return this.zacHttpClient.GET("/rest/klanten/personen/parameters", {});
  }

  /* istanbul ignore next */
  listPersonen(body: PutBody<"/rest/klanten/personen">) {
    return this.zacHttpClient.PUT("/rest/klanten/personen", body, {});
  }

  /* istanbul ignore next */
  listBedrijven(
    listBedrijvenParameters: ListBedrijvenParameters,
  ): Observable<Resultaat<Bedrijf>> {
    return this.http
      .put<
        Resultaat<Bedrijf>
      >(`${this.basepath}/bedrijven`, listBedrijvenParameters)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  listBetrokkeneRoltypen(zaaktypeUuid: string) {
    return this.http
      .get<
        GeneratedType<"RestRoltype">[]
      >(`${this.basepath}/roltype/${zaaktypeUuid}/betrokkene`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  listRoltypen() {
    return this.http
      .get<GeneratedType<"RestRoltype">[]>(`${this.basepath}/roltype`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  /* istanbul ignore next */
  ophalenContactGegevens(
    initiatorIdentificatie: string,
  ): Observable<ContactGegevens> {
    return this.http
      .get<ContactGegevens>(
        `${this.basepath}/contactgegevens/${initiatorIdentificatie}`,
      )
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
