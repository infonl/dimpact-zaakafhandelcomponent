/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { Router } from "@angular/router";
import { render, screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { BackButtonDirective, injectCanGoBack } from "./back-button.directive";

@Component({
  template: "<button zacBackButton>back</button>",
  imports: [BackButtonDirective],
})
class TestHostComponent {}

describe(BackButtonDirective.name, () => {
  beforeEach(() => {
    jest.spyOn(history, "back").mockImplementation(() => {});
  });

  it("goes back in the browser history when it holds a ZAC page before this one", async () => {
    Object.assign(window.navigation, { canGoBack: true });
    await render(TestHostComponent);

    await userEvent.setup().click(screen.getByRole("button", { name: "back" }));

    expect(history.back).toHaveBeenCalled();
  });

  it("goes to the dashboard when no ZAC page came before this one, so the user does not leave ZAC", async () => {
    await render(TestHostComponent);
    const router = TestBed.inject(Router);
    jest.spyOn(router, "navigateByUrl").mockResolvedValue(true);

    await userEvent.setup().click(screen.getByRole("button", { name: "back" }));

    expect(history.back).not.toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith("/");
  });
});

describe(injectCanGoBack.name, () => {
  it("follows the browser history as the user navigates", () => {
    const canGoBack = TestBed.runInInjectionContext(() => injectCanGoBack());
    expect(canGoBack()).toBe(false);

    Object.assign(window.navigation, { canGoBack: true });
    window.navigation.dispatchEvent(new Event("currententrychange"));

    expect(canGoBack()).toBe(true);
  });
});
