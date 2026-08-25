/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";

const STYLESHEET_URLS = [
  "/assets/vendor/bootstrap/bootstrap.min.css",
  "/assets/vendor/formio/formio.full.min.css",
];

/**
 * The builder drags elements into `document.body` and mounts its settings dialog outside the
 * component, so — unlike the renderer — its stylesheets cannot be contained in a shadow root.
 * They are linked on the document while the builder page is open and unlinked again on leaving.
 *
 * A `<link>` rather than an adopted stylesheet: the icon font is referenced relative to the
 * stylesheet, and an adopted stylesheet resolves that against the document instead.
 */
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
