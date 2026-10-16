/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { TestBed } from "@angular/core/testing";
import { MatTooltipHarness } from "@angular/material/tooltip/testing";
import { provideNoopAnimations } from "@angular/platform-browser/animations";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { ZaakspecifiekGeautoriseerdIconComponent } from "./zaakspecifiek-geautoriseerd-icon.component";

describe(ZaakspecifiekGeautoriseerdIconComponent.name, () => {
  const setup = async (isZaakspecifiekGeautoriseerd: boolean) => {
    const { fixture } = await render(ZaakspecifiekGeautoriseerdIconComponent, {
      inputs: { isZaakspecifiekGeautoriseerd },
      imports: [TranslateModule.forRoot()],
      providers: [provideNoopAnimations()],
    });

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      "zaak.is-zaakspecifiek-geautoriseerd": "Zaakspecifiek geautoriseerd",
    });
    translateService.use("nl");
    fixture.detectChanges();

    return TestbedHarnessEnvironment.loader(fixture);
  };

  it("shows a lock that screen readers announce as zaakspecifiek geautoriseerd for a zaakspecifiek geautoriseerde zaak", async () => {
    await setup(true);

    expect(
      screen.getByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).toHaveTextContent("lock");
  });

  it("shows a zaakspecifiek geautoriseerd tooltip when hovering the lock", async () => {
    const loader = await setup(true);
    const tooltip = await loader.getHarness(MatTooltipHarness);

    await tooltip.show();

    expect(await tooltip.getTooltipText()).toBe("Zaakspecifiek geautoriseerd");
  });

  it("shows no lock for a zaak that is not zaakspecifiek geautoriseerd", async () => {
    await setup(false);

    expect(
      screen.queryByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).not.toBeInTheDocument();
  });
});
