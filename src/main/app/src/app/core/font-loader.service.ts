/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { FontCacheBustingService } from "./font-cache-busting.service";

@Injectable({
  providedIn: "root",
})
export class FontLoaderService {
  private loadedFonts = new Set<string>();

  constructor(
    private readonly fontCacheBustingService: FontCacheBustingService,
  ) {}

  /**
   * Loads fonts with cache busting
   */
  loadFonts() {
    this.loadMaterialSymbolsFont();
    this.loadRobotoFonts();
  }

  /**
   * Loads Material Symbols font
   */
  private loadMaterialSymbolsFont() {
    const fontFamily = "material-symbols-outlined";
    if (this.loadedFonts.has(fontFamily)) return;

    const fontUrl = this.fontCacheBustingService.getMaterialSymbolsFontUrl();
    const css = `
      @font-face {
        font-family: 'Material Symbols Outlined';
        font-style: normal;
        src: url('${fontUrl}') format('woff2');
      }
    `;

    this.injectFontCSS(css, fontFamily);
    this.loadedFonts.add(fontFamily);
  }

  /**
   * Loads Roboto fonts
   */
  private loadRobotoFonts() {
    const fontFamily = "Roboto";
    if (this.loadedFonts.has(fontFamily)) return;

    const weights = ["300", "400", "500"] as const;
    let css = "";

    weights.forEach((weight) => {
      const fontUrl = this.fontCacheBustingService.getRobotoFontUrl(weight);
      css += `
        @font-face {
          font-family: 'Roboto';
          font-style: normal;
          font-weight: ${weight};
          font-display: swap;
          src: url('${fontUrl}') format('woff2');
          unicode-range: U+0000-00FF, U+0131, U+0152-0153, U+02BB-02BC, U+02C6, U+02DA,
                         U+02DC, U+0304, U+0308, U+0329, U+2000-206F, U+2074, U+20AC, U+2122, U+2191,
                         U+2193, U+2212, U+2215, U+FEFF, U+FFFD;
        }
      `;
    });

    this.injectFontCSS(css, fontFamily);
    this.loadedFonts.add(fontFamily);
  }

  /**
   * Injects font CSS into the document head
   */
  private injectFontCSS(css: string, fontFamily: string) {
    const styleId = `font-${fontFamily.replace(/\s+/g, "-").toLowerCase()}`;

    // Remove existing style if it exists
    const existingStyle = document.getElementById(styleId);
    existingStyle?.remove();

    // Create new style element
    const style = document.createElement("style");
    style.id = styleId;
    style.textContent = css;
    document.head.appendChild(style);
  }
}
