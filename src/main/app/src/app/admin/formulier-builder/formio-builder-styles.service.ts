/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

const STYLESHEET_URLS = [
  "/assets/vendor/bootstrap/bootstrap.min.css",
  "/assets/vendor/formio/formio.full.min.css",
];

@Injectable({
  providedIn: "root",
})
export class FormioBuilderStylesService {
  private links: HTMLLinkElement[] = [];

  link(): Promise<void[]> {
    this.links = STYLESHEET_URLS.map((url) => {
      const link = document.createElement("link");
      link.rel = "stylesheet";
      link.href = url;
      document.head.appendChild(link);
      return link;
    });
    return Promise.all(
      this.links.map(
        (link) =>
          new Promise<void>((resolve) => {
            link.addEventListener("load", () => resolve());
            link.addEventListener("error", () => resolve());
          }),
      ),
    );
  }

  unlink() {
    this.links.forEach((link) => link.remove());
    this.links = [];
  }
}
