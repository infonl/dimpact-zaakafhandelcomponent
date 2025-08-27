/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import { TranslateLoader } from "@ngx-translate/core";
import { Observable, of } from "rxjs";
import { catchError, map } from "rxjs/operators";

export class CacheBustingTranslateLoader implements TranslateLoader {
  private cacheBuster: string;

  constructor(
    private readonly http: HttpClient,
    private readonly prefix: string = "./assets/i18n/",
    private readonly suffix: string = ".json"
  ) {
    // Generate a cache buster based on build time or version
    // This will be replaced during build time with the actual hash
    this.cacheBuster = this.generateCacheBuster();
  }

  /**
   * Gets the translations from the server
   */
  getTranslation(lang: string) {
    const url = `${this.prefix}${lang}${this.suffix}?v=${this.cacheBuster}`;

    return this.http.get(url).pipe(
      map((response: any) => {
        if (typeof response === "string") {
          return JSON.parse(response);
        }
        return response;
      }),
      catchError((error) => {
        console.error(
          `Failed to load translation file for language: ${lang}`,
          error
        );
        return of({});
      })
    );
  }

  /**
   * Generates a cache buster string
   * In production, this should be replaced with the actual build hash
   */
  private generateCacheBuster() {
    // In development, use timestamp
    if (
      typeof window !== "undefined" &&
      window.location.hostname === "localhost"
    ) {
      return Date.now().toString();
    }

    // In production, this will be replaced during build
    return "BUILD_HASH";
  }
}

/**
 * Factory function to create the cache busting translate loader
 */
export function createCacheBustingTranslateLoader(http: HttpClient) {
  return new CacheBustingTranslateLoader(http);
}
