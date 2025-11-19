/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { FontCacheBustingService } from "./font-cache-busting.service";

@Injectable({
  providedIn: "root",
})
export class FontPreloadInjectorService {
  private injected = false;

  constructor(
    private readonly fontCacheBustingService: FontCacheBustingService,
  ) {}

  /**
   * Injects font preloading links into the document head
   */
  injectFontPreloads() {
    if (this.injected) return; // Already injected

    const fontUrls = [
      this.fontCacheBustingService.getRobotoFontUrl("300"),
      this.fontCacheBustingService.getRobotoFontUrl("400"),
      this.fontCacheBustingService.getRobotoFontUrl("500"),
      this.fontCacheBustingService.getMaterialSymbolsFontUrl(),
    ];

    fontUrls.forEach((fontUrl) => {
      this.createPreloadLink(fontUrl);
    });

    this.injected = true;
  }

  /**
   * Creates a preload link element for a font
   */
  private createPreloadLink(href: string) {
    // Check if link already exists
    const existingLink = document.querySelector(`link[href="${href}"]`);
    if (existingLink) return;

    const link = document.createElement("link");
    link.rel = "preload";
    link.href = href;
    link.as = "font";
    link.type = "font/woff2";
    link.crossOrigin = "";

    // Insert at the beginning of head for optimal loading
    document.head.insertBefore(link, document.head.firstChild);
  }

  /**
   * Removes all font preload links (useful for testing)
   */
  removeFontPreloads() {
    const preloadLinks = document.querySelectorAll(
      'link[rel="preload"][as="font"]',
    );
    preloadLinks.forEach((link) => {
      if (link.getAttribute("href")?.includes("/assets/fonts/")) {
        link.remove();
      }
    });
    this.injected = false;
  }
}
