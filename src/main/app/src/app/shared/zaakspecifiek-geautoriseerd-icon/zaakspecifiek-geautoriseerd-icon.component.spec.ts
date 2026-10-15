/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { ZaakspecifiekGeautoriseerdIconComponent } from "./zaakspecifiek-geautoriseerd-icon.component";

describe(ZaakspecifiekGeautoriseerdIconComponent.name, () => {
  const setup = async (isZaakspecifiekGeautoriseerd: boolean) => {
    const { fixture } = await render(ZaakspecifiekGeautoriseerdIconComponent, {
      inputs: { isZaakspecifiekGeautoriseerd },
      imports: [TranslateModule.forRoot()],
    });

    const translateService = TestBed.inject(TranslateService);
    translateService.setTranslation("nl", {
      "zaak.is-zaakspecifiek-geautoriseerd": "Zaakspecifiek geautoriseerd",
    });
    translateService.use("nl");
    fixture.detectChanges();
  };

  it("shows a lock titled zaakspecifiek geautoriseerd for a zaakspecifiek geautoriseerde zaak", async () => {
    await setup(true);

    expect(screen.getByTitle("Zaakspecifiek geautoriseerd")).toHaveTextContent(
      "lock",
    );
  });

  it("shows no lock for a zaak that is not zaakspecifiek geautoriseerd", async () => {
    await setup(false);

    expect(
      screen.queryByTitle("Zaakspecifiek geautoriseerd"),
    ).not.toBeInTheDocument();
  });
});
