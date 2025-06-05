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
import { BSN_LENGTH } from "../shared/utils/constants";
import { GeneratedType } from "../shared/utils/generated-types";
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
  readPersoon(bsn: string, audit: { context: string; action: string }) {
    return this.zacHttpClient
      .GET("/rest/klanten/persoon/{bsn}", {
        path: { bsn },
        header: {
          "X-Verwerking": `${audit.context}@${audit.action}`,
        },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readBedrijf(rsinOfVestigingsnummer: string) {
    return rsinOfVestigingsnummer.length === BSN_LENGTH
      ? this.readRechtspersoon(rsinOfVestigingsnummer)
      : this.readVestiging(rsinOfVestigingsnummer);
  }

  /* istanbul ignore next */
  readVestiging(vestigingsnummer: string) {
    return this.zacHttpClient.GET(
      "/rest/klanten/vestiging/{vestigingsnummer}",
      {
        path: { vestigingsnummer },
      },
    );
  }

  /* istanbul ignore next */
  readVestigingsprofiel(vestigingsnummer: string) {
    return this.zacHttpClient.GET(
      "/rest/klanten/vestigingsprofiel/{vestigingsnummer}",
      {
        path: { vestigingsnummer },
      },
    );
  }

  /* istanbul ignore next */
  readRechtspersoon(rsin: string) {
    return this.zacHttpClient.GET("/rest/klanten/rechtspersoon/{rsin}", {
      path: { rsin },
    });
  }

  /* istanbul ignore next */
  getPersonenParameters() {
    return this.zacHttpClient.GET("/rest/klanten/personen/parameters", {});
  }

  /* istanbul ignore next */
  listPersonen(
    body: PutBody<"/rest/klanten/personen">,
    audit: { context: string; action: string },
  ) {
    return this.zacHttpClient.PUT("/rest/klanten/personen", body, {
      header: {
        "X-Verwerking": `${audit.context}@${audit.action}`,
      },
    });
  }

  /* istanbul ignore next */
  listBedrijven(body: PutBody<"/rest/klanten/bedrijven">) {
    return this.zacHttpClient.PUT("/rest/klanten/bedrijven", body, {});
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
