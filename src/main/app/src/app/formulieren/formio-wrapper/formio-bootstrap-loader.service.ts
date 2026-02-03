/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

@Injectable({
  providedIn: "root",
})
export class FormioBootstrapLoaderService {
  private styleSheetPromise: Promise<CSSStyleSheet> | null = null;

  getBootstrapStyleSheet(): Promise<CSSStyleSheet> {
    if (!this.styleSheetPromise) {
      this.styleSheetPromise = this.loadStyleSheet();
    }
    return this.styleSheetPromise;
  }

  private async loadStyleSheet(): Promise<CSSStyleSheet> {
    const response = await fetch("/assets/vendor/bootstrap/bootstrap.min.css");
    const css = await response.text();
    const styleSheet = new CSSStyleSheet();
    await styleSheet.replace(css.replace(/:root\b/g, ":host"));
    return styleSheet;
  }
}
