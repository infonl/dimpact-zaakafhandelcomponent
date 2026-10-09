/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideRouter, Router } from "@angular/router";
import { findOrigin, openedFromSearchState } from "./navigation-origin";

@Component({ template: "" })
class EmptyComponent {}

describe(findOrigin.name, () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([{ path: "**", component: EmptyComponent }])],
    });
    router = TestBed.inject(Router);
  });

  it("is the page the current page was opened from", async () => {
    await router.navigateByUrl("/zaken/ZAAK-1");
    await router.navigateByUrl("/taken/1");

    expect(findOrigin(router)?.toString()).toBe("/zaken/ZAAK-1");
  });

  it("is absent when the current page was opened from search, because search overlays an unrelated page", async () => {
    await router.navigateByUrl("/zaken/ZAAK-1");
    await router.navigateByUrl("/taken/1", { state: openedFromSearchState });

    expect(findOrigin(router)).toBeNull();
  });

  it("is absent when no page was opened before the current page", async () => {
    await router.navigateByUrl("/taken/1");

    expect(findOrigin(router)).toBeNull();
  });
});
