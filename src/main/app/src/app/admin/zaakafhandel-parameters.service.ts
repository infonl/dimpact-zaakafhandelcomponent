/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import {Observable, of, throwError} from "rxjs";
import { catchError } from "rxjs/operators";
import { FoutAfhandelingService } from "../fout-afhandeling/fout-afhandeling.service";
import { ZacHttpClient } from "../shared/http/zac-http-client";
import { Api } from "../shared/utils/generated-types";
import { CaseDefinition } from "./model/case-definition";
import { ZaakbeeindigReden } from "./model/zaakbeeindig-reden";

@Injectable({
  providedIn: "root",
})
export class ZaakafhandelParametersService {
  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
    private readonly zacHttpClient: ZacHttpClient,
  ) {}

  private basepath = "/rest/zaakafhandelparameters";

  listZaakafhandelParameters() {
    return this.zacHttpClient
      .GET("/rest/zaakafhandelparameters")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readZaakafhandelparameters(zaaktypeUUID: string) {
    return this.zacHttpClient
      .GET("/rest/zaakafhandelparameters/{zaaktypeUUID}", {
        pathParams: {
          path: { zaaktypeUUID },
        },
      })
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZaakbeeindigRedenen(): Observable<ZaakbeeindigReden[]> {
    return this.http
      .get<ZaakbeeindigReden[]>(`${this.basepath}/zaakbeeindigredenen`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listZaakbeeindigRedenenForZaaktype(
    zaaktypeUuid: string,
  ): Observable<ZaakbeeindigReden[]> {
    return this.http
      .get<
        ZaakbeeindigReden[]
      >(`${this.basepath}/zaakbeeindigredenen/${zaaktypeUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listResultaattypes(zaaktypeUuid: string) {
    return this.http
      .get<
        Api<"RestResultaattype">[]
      >(`${this.basepath}/resultaattypes/${zaaktypeUuid}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listCaseDefinitions() {
    return this.zacHttpClient
      .GET("/rest/zaakafhandelparameters/case-definitions")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  readCaseDefinition(key: string): Observable<CaseDefinition> {
    return this.http
      .get<CaseDefinition>(`${this.basepath}/case-definitions/${key}`)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  updateZaakafhandelparameters(
    zaakafhandelparameters: Api<"RestZaakafhandelParameters">,
  ) {
    return this.zacHttpClient
      .PUT("/rest/zaakafhandelparameters", zaakafhandelparameters)
      .pipe(
        catchError((err) => {
          this.foutAfhandelingService.foutAfhandelen(err);
          return throwError(() => err);
        }),
      );
  }

  listFormulierDefinities() {
    return this.zacHttpClient
      .GET("/rest/zaakafhandelparameters/formulierdefinities")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  listReplyTos() {
    return of([
      {
        "mail": "GEMEENTE",
        "speciaal": true
      },
      {
        "mail": "MEDEWERKER",
        "speciaal": true
      },
      {
        "mail": "Nieuwe waarde",
        "speciaal": false
      },
      {
        "mail": "Nieuwe waarde 2",
        "speciaal": false
      },
      {
        "mail": "Noreply@groningen.nl",
        "speciaal": false
      },
      {
        "mail": "behandelaar@team-dimpact.info.nl",
        "speciaal": false
      },
      {
        "mail": "commonground@groningen.nl",
        "speciaal": false
      },
      {
        "mail": "hilbrand.hurtak@groningen.nl",
        "speciaal": false
      },
      {
        "mail": "hop@info.nl",
        "speciaal": false
      },
      {
        "mail": "karin.masselink@dimpact.nl",
        "speciaal": false
      }
    ])

    return this.zacHttpClient
      .GET("/rest/zaakafhandelparameters/replyTo")
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }
}
