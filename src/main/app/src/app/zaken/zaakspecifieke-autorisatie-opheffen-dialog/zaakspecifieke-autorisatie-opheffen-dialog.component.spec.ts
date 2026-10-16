/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { ZaakspecifiekeAutorisatieOpheffenDialogComponent } from "./zaakspecifieke-autorisatie-opheffen-dialog.component";

describe(ZaakspecifiekeAutorisatieOpheffenDialogComponent.name, () => {
  let dialogRef: { close: jest.Mock };

  async function setup() {
    dialogRef = { close: jest.fn() };
    await render(ZaakspecifiekeAutorisatieOpheffenDialogComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [{ provide: MatDialogRef, useValue: dialogRef }],
    });
  }

  it("asks whether to lift the zaakspecifieke autorisatie, without a reden field", async () => {
    await setup();

    expect(
      screen.getByRole("heading", {
        name: /title.zaakspecifieke-autorisatie.opheffen/,
      }),
    ).toBeVisible();
    expect(
      screen.getByText("msg.zaakspecifieke-autorisatie.opheffen.bevestigen"),
    ).toBeVisible();
    expect(screen.queryByRole("textbox")).not.toBeInTheDocument();
  });

  it("closes with a confirmation when the employee chooses to lift", async () => {
    await setup();

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.opheffen" }));

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it("closes without a confirmation when the employee chooses to close", async () => {
    await setup();

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});
