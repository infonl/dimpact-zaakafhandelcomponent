/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

@Injectable({
  providedIn: "root",
})
export class FontCacheBustingService {
  private readonly fontHash: string;

  constructor() {
    // This will be replaced during build time with the actual font hash
    // For local development (and testing), generate a consistent hash
    if (
      typeof window !== "undefined" &&
      window.location.hostname === "localhost"
    ) {
      this.fontHash = "test1234";
    } else {
      this.fontHash = "FONT_HASH"; // This will be replaced during build time
    }
  }

  /**
   * Adds cache busting parameter to font URLs
   */
  getFontUrl(fontPath: string) {
    const separator = fontPath.includes("?") ? "&" : "?";
    return `${fontPath}${separator}v=${this.fontHash}`;
  }

  /**
   * Gets the Material Symbols font URL with cache busting
   */
  getMaterialSymbolsFontUrl() {
    return this.getFontUrl("./assets/MaterialSymbolsOutlined.woff2");
  }

  /**
   * Gets a Roboto font URL with cache busting
   */
  getRobotoFontUrl(weight: "300" | "400" | "500") {
    return this.getFontUrl(`./assets/fonts/Roboto/${weight}.woff2`);
  }
}
