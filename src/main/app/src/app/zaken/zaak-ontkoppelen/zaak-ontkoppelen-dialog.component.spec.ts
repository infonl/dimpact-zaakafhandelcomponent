/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpTestingController } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { render, screen } from "@testing-library/angular";
import { userEvent } from "@testing-library/user-event";
import { sleep } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakOntkoppelenDialogComponent } from "./zaak-ontkoppelen-dialog.component";

const dialogData: Omit<GeneratedType<"RestZaakUnlinkData">, "reden"> = {
  zaakUuid: "zaak-uuid-1",
  gekoppeldeZaakIdentificatie: "ZAAK-002",
  relatieType: "GERELATEERD",
};

describe(ZaakOntkoppelenDialogComponent.name, () => {
  let dialogRef: { close: jest.Mock; disableClose: boolean };
  let httpTestingController: HttpTestingController;
  let fixture: ComponentFixture<ZaakOntkoppelenDialogComponent>;

  async function setup() {
    dialogRef = { close: jest.fn(), disableClose: false };

    ({ fixture } = await render(ZaakOntkoppelenDialogComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);
    jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation();
  }

  function ontkoppelenButton() {
    return screen.getByRole("button", { name: "actie.ontkoppelen" });
  }

  async function ontkoppel(reden: string) {
    const user = userEvent.setup();
    await user.type(screen.getByRole("textbox"), reden);
    await user.click(ontkoppelenButton());
    await sleep();
    fixture.detectChanges();
  }

  it("renders the dialog title", async () => {
    await setup();
    expect(
      screen.getByRole("heading", { name: /title.zaak.ontkoppelen/ }),
    ).toBeVisible();
  });

  it("does not offer to unlink before a reden is given", async () => {
    await setup();
    expect(ontkoppelenButton()).toBeDisabled();
  });

  it("offers to unlink once a reden is given", async () => {
    await setup();
    await userEvent.setup().type(screen.getByRole("textbox"), "reden tekst");

    expect(ontkoppelenButton()).toBeEnabled();
  });

  it("unlinks the zaak with the given reden", async () => {
    await setup();
    await ontkoppel("mijn reden");

    const request = httpTestingController.expectOne(
      "/rest/zaken/zaak/ontkoppel",
    );
    expect(request.request.method).toBe("PATCH");
    expect(request.request.body).toEqual({
      ...dialogData,
      reden: "mijn reden",
    });
  });

  it("refuses a second click while the unlinking is in flight", async () => {
    await setup();
    await ontkoppel("mijn reden");

    expect(ontkoppelenButton()).toBeDisabled();
    httpTestingController.expectOne("/rest/zaken/zaak/ontkoppel");
  });

  it("closes the dialog once the zaak is unlinked", async () => {
    await setup();
    await ontkoppel("mijn reden");

    httpTestingController
      .expectOne("/rest/zaken/zaak/ontkoppel")
      .flush(null, { status: 204, statusText: "No Content" });
    await sleep();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it("lets the user close the dialog again when unlinking fails", async () => {
    await setup();
    await ontkoppel("mijn reden");

    httpTestingController
      .expectOne("/rest/zaken/zaak/ontkoppel")
      .flush(null, { status: 500, statusText: "Server Error" });
    await sleep();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(dialogRef.disableClose).toBe(false);
  });

  it("closes the dialog when cancel is clicked", async () => {
    await setup();
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it("closes the dialog when the close button in the title is clicked", async () => {
    await setup();
    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(dialogRef.close).toHaveBeenCalled();
  });
});
