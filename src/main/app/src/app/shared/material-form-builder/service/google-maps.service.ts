/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { Inject, Injectable } from "@angular/core";
import { BehaviorSubject, Observable, of } from "rxjs";
import { catchError, map } from "rxjs/operators";
import { environment } from "src/environments/environment";
import {
  BUILDER_CONFIG,
  MaterialFormBuilderConfig,
} from "../material-form-builder-config";

/**
 * Singleton service voor het 1-malig inladen van de googlemaps api
 */
@Injectable({
  providedIn: "root",
})
export class GoogleMapsService {
  private $loaded: BehaviorSubject<boolean> = new BehaviorSubject<boolean>(
    false,
  );

  constructor(
    @Inject(BUILDER_CONFIG) private config: MaterialFormBuilderConfig,
    private httpClient: HttpClient,
  ) {}

  /**
   * Het laden van de googlemaps api
   */
  public load(): void {
    if (!this.config.googleMapsApiKey) {
      throw new Error("A googlemaps apikey needs to be provided");
    }

    const urlParams = new URLSearchParams({
      key: this.config.googleMapsApiKey,
      libraries: "places",
    });

    const fullUrl = `${environment.GOOGLE_API_URL}?${urlParams.toString()}`;

    const loading = this.httpClient.jsonp(fullUrl, "callback").pipe(
      map(() => true),
      catchError(() => of(false)),
    );

    loading.subscribe(() => {
      this.$loaded.next(true);
    });
  }

  public loadCurrentPosition(): Promise<google.maps.LatLng> {
    return new Promise<google.maps.LatLng>((resolve, reject) => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) =>
            resolve(
              new google.maps.LatLng(
                position.coords.latitude,
                position.coords.longitude,
              ),
            ),
          (error) => reject(error),
          {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 1000,
          },
        );
      } else {
        reject("Locatie niet beschikbaar");
      }
    });
  }

  get loaded(): Observable<boolean> {
    return this.$loaded;
  }
}
