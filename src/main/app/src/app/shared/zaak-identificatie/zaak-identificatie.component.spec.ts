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
import { ZaakIdentificatieComponent } from "./zaak-identificatie.component";

describe(ZaakIdentificatieComponent.name, () => {
  const setup = async (
    identificatie: string | null,
    isZaakspecifiekGeautoriseerd: boolean,
  ) => {
    const { fixture } = await render(ZaakIdentificatieComponent, {
      inputs: { identificatie, isZaakspecifiekGeautoriseerd },
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

  it("shows the zaaknummer with a lock that screen readers announce as zaakspecifiek geautoriseerd", async () => {
    await setup("ZAAK-2026-0000000001", true);

    expect(screen.getByText("ZAAK-2026-0000000001")).toBeInTheDocument();
    expect(
      screen.getByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).toHaveTextContent("lock");
  });

  it("shows a zaakspecifiek geautoriseerd tooltip when hovering the lock", async () => {
    const loader = await setup("ZAAK-2026-0000000001", true);
    const tooltip = await loader.getHarness(MatTooltipHarness);

    await tooltip.show();

    expect(await tooltip.getTooltipText()).toBe("Zaakspecifiek geautoriseerd");
  });

  it("shows only the zaaknummer for a zaak that is not zaakspecifiek geautoriseerd", async () => {
    await setup("ZAAK-2026-0000000001", false);

    expect(screen.getByText("ZAAK-2026-0000000001")).toBeInTheDocument();
    expect(
      screen.queryByRole("img", { name: "Zaakspecifiek geautoriseerd" }),
    ).not.toBeInTheDocument();
  });

  it("shows a dash when there is no zaaknummer", async () => {
    await setup(null, false);

    expect(screen.getByText("-")).toBeInTheDocument();
  });
});
