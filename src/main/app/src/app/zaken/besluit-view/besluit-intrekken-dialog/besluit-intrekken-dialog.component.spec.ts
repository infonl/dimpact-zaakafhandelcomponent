/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { EMPTY } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { FoutAfhandelingService } from "../../../fout-afhandeling/fout-afhandeling.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BesluitIntrekkenDialogComponent } from "./besluit-intrekken-dialog.component";

const INTREKKEN_URL = "/rest/zaken/besluit/intrekken";

const makeBesluit = (fields: Partial<GeneratedType<"RestBesluit">> = {}) =>
  fromPartial<GeneratedType<"RestBesluit">>({
    uuid: "besluit-uuid-1",
    ingangsdatum: "2026-01-01",
    vervaldatum: "2026-12-31",
    informatieobjecten: [],
    ...fields,
  });

describe(BesluitIntrekkenDialogComponent.name, () => {
  const user = userEvent.setup();

  const setup = async (besluit = makeBesluit()) => {
    const dialogRefMock = { close: jest.fn(), disableClose: false };

    await render(BesluitIntrekkenDialogComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        provideMomentDateAdapter({
          parse: { dateInput: "yyyy-MM-DD" },
          display: {
            dateInput: "yyyy-MM-DD",
            monthYearLabel: "MMMM YYYY",
            dateA11yLabel: "LL",
            monthYearA11yLabel: "MMMM YYYY",
          },
        }),
        { provide: MAT_DIALOG_DATA, useValue: besluit },
        { provide: MatDialogRef, useValue: dialogRefMock },
      ],
    });

    const foutAfhandelingService = TestBed.inject(FoutAfhandelingService);
    jest.spyOn(foutAfhandelingService, "foutAfhandelen").mockReturnValue(EMPTY);
    const utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "openSnackbar").mockImplementation();

    return {
      httpTestingController: TestBed.inject(HttpTestingController),
      dialogRefMock,
      foutAfhandelingService,
      utilService,
    };
  };

  const submitButton = () =>
    screen.getByRole("button", { name: "actie.besluit.intrekken" });

  const fillVervaldatum = (vervaldatum: string) =>
    user.type(screen.getByLabelText("Vervaldatum"), vervaldatum);

  const fillToelichting = (toelichting: string) =>
    user.type(screen.getByLabelText("Toelichting"), toelichting);

  const pickVervalreden = async () => {
    await user.click(screen.getByLabelText("Besluit.vervalreden"));
    await user.click(
      screen.getByRole("option", {
        name: "besluit.vervalreden.ingetrokken_overheid",
      }),
    );
  };

  const fillValidForm = async () => {
    await fillVervaldatum("2026-12-31");
    await pickVervalreden();
    await fillToelichting("Reden van intrekken");
  };

  it("starts with an empty vervaldatum", async () => {
    await setup();

    expect(screen.getByLabelText("Vervaldatum")).toHaveValue("");
  });

  it("offers all vervalredenen except tijdelijk", async () => {
    await setup();

    await user.click(screen.getByLabelText("Besluit.vervalreden"));

    expect(
      screen.getAllByRole("option").map((option) => option.textContent?.trim()),
    ).toEqual([
      "besluit.vervalreden.ingetrokken_overheid",
      "besluit.vervalreden.ingetrokken_belanghebbende",
    ]);
  });

  it("hides the documenten warning when no document has been sent", async () => {
    await setup();

    expect(screen.queryByText("msg.besluit.documenten.verstuurd")).toBeNull();
  });

  it("warns when a linked document has already been sent", async () => {
    await setup(
      makeBesluit({
        informatieobjecten: [
          fromPartial<GeneratedType<"RestEnkelvoudigInformatieobject">>({
            verzenddatum: "2026-02-01",
          }),
        ],
      }),
    );

    expect(screen.getByText("msg.besluit.documenten.verstuurd")).toBeVisible();
  });

  it("cannot be submitted without a toelichting", async () => {
    await setup();

    await fillVervaldatum("2026-12-31");
    await pickVervalreden();

    expect(submitButton()).toBeDisabled();
  });

  it("cannot be submitted without a vervalreden", async () => {
    await setup();

    await fillVervaldatum("2026-12-31");
    await fillToelichting("Reden van intrekken");

    expect(submitButton()).toBeDisabled();
  });

  it("cannot be submitted with a vervaldatum before the ingangsdatum", async () => {
    await setup();

    await fillVervaldatum("2025-01-01");
    await pickVervalreden();
    await fillToelichting("Reden van intrekken");

    expect(submitButton()).toBeDisabled();
  });

  it("withdraws the besluit with the values from the form", async () => {
    const { httpTestingController } = await setup();
    await fillValidForm();

    await user.click(submitButton());
    await sleep();

    const request = httpTestingController.expectOne(INTREKKEN_URL);
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual(
      expect.objectContaining({
        besluitUuid: "besluit-uuid-1",
        reden: "Reden van intrekken",
        vervalreden: "INGETROKKEN_OVERHEID",
      }),
    );
    request.flush(null);
  });

  it("shows a snackbar and closes the dialog once the besluit is withdrawn", async () => {
    const { httpTestingController, dialogRefMock, utilService } = await setup();
    await fillValidForm();

    await user.click(submitButton());
    await sleep();
    httpTestingController.expectOne(INTREKKEN_URL).flush(null);
    await sleep(100);

    expect(utilService.openSnackbar).toHaveBeenCalledWith(
      "msg.besluit.ingetrokken",
    );
    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("shows an error and keeps the dialog open when withdrawing fails", async () => {
    const { httpTestingController, dialogRefMock, foutAfhandelingService } =
      await setup();
    await fillValidForm();

    await user.click(submitButton());
    await sleep();
    httpTestingController
      .expectOne(INTREKKEN_URL)
      .flush(null, { status: 500, statusText: "Internal Server Error" });
    await sleep(100);

    expect(foutAfhandelingService.foutAfhandelen).toHaveBeenCalled();
    expect(dialogRefMock.close).not.toHaveBeenCalledWith(true);
  });

  it("disables closing the dialog while the besluit is being withdrawn", async () => {
    const { httpTestingController, dialogRefMock } = await setup();
    await fillValidForm();

    await user.click(submitButton());
    await sleep();

    expect(dialogRefMock.disableClose).toBe(true);
    httpTestingController.expectOne(INTREKKEN_URL).flush(null);
  });

  it("closes the dialog without withdrawing when the close button is used", async () => {
    const { dialogRefMock } = await setup();

    await user.click(
      screen.getByRole("button", { name: "actie.paneel.sluiten" }),
    );

    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });
});
