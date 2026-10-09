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
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { provideZacDateAdapter } from "../../shared/form/date/provide-zac-date-adapter";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakOpschortenDialogComponent } from "./zaak-opschorten-dialog.component";

const makeZaak = (fields: Partial<GeneratedType<"RestZaak">> = {}) =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid-1",
    identificatie: "ZAAK-001",
    uiterlijkeEinddatumAfdoening: "2026-12-31",
    einddatumGepland: null,
    ...fields,
  });

describe(ZaakOpschortenDialogComponent.name, () => {
  let dialogRef: {
    close: jest.Mock;
    disableClose: boolean;
    afterOpened: jest.Mock;
  };
  let fixture: ComponentFixture<ZaakOpschortenDialogComponent>;
  let httpTestingController: HttpTestingController;

  async function setup(zaak = makeZaak()) {
    dialogRef = {
      close: jest.fn(),
      disableClose: false,
      afterOpened: jest.fn().mockReturnValue(of(undefined)),
    };

    ({ fixture } = await render(ZaakOpschortenDialogComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZacDateAdapter(),
        { provide: MAT_DIALOG_DATA, useValue: { zaak } },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    }));

    httpTestingController = TestBed.inject(HttpTestingController);
    jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation();
  }

  function opschortenButton() {
    return screen.getByRole("button", { name: /actie.zaak.opschorten/ });
  }

  async function opschorten(numberOfDays: string, reason: string) {
    const user = userEvent.setup();
    await user.type(
      screen.getByRole("spinbutton", { name: /opschortduur/i }),
      numberOfDays,
    );
    await user.type(screen.getByRole("textbox", { name: /reason/i }), reason);
    fixture.detectChanges();
    await user.click(opschortenButton());
    await sleep();
    fixture.detectChanges();
  }

  it("does not ask for an einddatum gepland when the zaak has none", async () => {
    await setup(makeZaak({ einddatumGepland: null }));

    expect(
      screen.queryByLabelText(/einddatum.?gepland/i),
    ).not.toBeInTheDocument();
  });

  it("asks for an einddatum gepland when the zaak has one", async () => {
    await setup(makeZaak({ einddatumGepland: "2026-06-30" }));

    expect(screen.getByLabelText(/einddatum.?gepland/i)).toBeInTheDocument();
  });

  it("does not offer to suspend before the form is filled in", async () => {
    await setup();

    expect(opschortenButton()).toBeDisabled();
  });

  it("dismisses the dialog without a result when cancelled", async () => {
    await setup();

    await userEvent
      .setup()
      .click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  it("suspends the zaak for the given number of days and reason", async () => {
    await setup(makeZaak({ uuid: "test-uuid" }));

    await opschorten("5", "Test reden voor opschorten");

    const request = httpTestingController.expectOne(
      "/rest/zaken/zaak/test-uuid/suspend",
    );
    expect(request.request.method).toBe("PATCH");
    expect(Number(request.request.body.numberOfDays)).toBe(5);
    expect(request.request.body.reason).toBe("Test reden voor opschorten");
  });

  it("refuses a second click while the suspension is in flight", async () => {
    await setup();

    await opschorten("3", "Reden");

    expect(opschortenButton()).toBeDisabled();
    httpTestingController.expectOne("/rest/zaken/zaak/zaak-uuid-1/suspend");
  });

  it("closes the dialog with the suspended zaak", async () => {
    const suspendedZaak = makeZaak({ identificatie: "ZAAK-RESULT" });
    await setup();

    await opschorten("3", "Reden");
    httpTestingController
      .expectOne("/rest/zaken/zaak/zaak-uuid-1/suspend")
      .flush(suspendedZaak);
    await sleep();

    expect(dialogRef.close).toHaveBeenCalledWith(suspendedZaak);
  });
});
