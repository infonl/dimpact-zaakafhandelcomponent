/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { catchError, mergeMap } from "rxjs/operators";
import { environment } from "src/environments/environment";
import { FoutAfhandelingService } from "../../fout-afhandeling/fout-afhandeling.service";

@Injectable({
  providedIn: "root",
})
export class LocationService {
  private readonly flSuggest: string = "type,weergavenaam,id";

  private readonly flLookup: string = "id,weergavenaam,centroide_ll,type";

  private readonly typeSuggest: string = "type:adres";

  private baseUrl = environment.LOCATION_SERVER_API_URL;

  constructor(
    private http: HttpClient,
    private foutAfhandelingService: FoutAfhandelingService,
  ) {}

  addressSuggest(
    zoekOpdracht: string,
  ): Observable<GeoDataResponse<SuggestResult>> {
    const urlParams = new URLSearchParams({
      wt: "json",
      q: zoekOpdracht,
      fl: this.flSuggest,
      fq: this.typeSuggest,
      rows: "5",
    });
    const url = `${this.baseUrl}/suggest?${urlParams.toString()}`;
    return this.http
      .get<GeoDataResponse<SuggestResult>>(url)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  geolocationAddressSuggest(
    coordinates: number[],
  ): Observable<GeoDataResponse<SuggestResult>> {
    const urlParams = new URLSearchParams({
      lon: coordinates[0].toString(),
      lat: coordinates[1].toString(),
      type: "adres",
      rows: "1",
      fl: this.flSuggest,
    });

    const url = `${this.baseUrl}/reverse?${urlParams.toString()}`;
    return this.http
      .get<GeoDataResponse<SuggestResult>>(url)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  addressLookup(objectId: string): Observable<GeoDataResponse<AddressResult>> {
    const urlParams = new URLSearchParams({
      wt: "json",
      id: objectId,
      fl: this.flLookup,
    });

    const url = `${this.baseUrl}/lookup?${urlParams.toString()}`;
    return this.http
      .get<GeoDataResponse<AddressResult>>(url)
      .pipe(
        catchError((err) => this.foutAfhandelingService.foutAfhandelen(err)),
      );
  }

  coordinateToAddress(
    coordinates: number[],
  ): Observable<GeoDataResponse<AddressResult>> {
    return this.geolocationAddressSuggest(coordinates).pipe(
      mergeMap((data) => this.addressLookup(data.response.docs[0].id)),
    );
  }
}

export interface SuggestResult {
  id: string;
  weergavenaam: string;
  type: string;
}

export interface AddressResult {
  id: string;
  weergavenaam: string;
  centroide_ll: string;
  type: string;
}

interface GeoDataResponse<TYPE> {
  response: {
    numFound: number;
    start: number;
    docs: [TYPE];
  };
}
