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
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenVrijgevenDialogComponent } from "./taken-vrijgeven-dialog.component";

const makeTaak = (fields: Partial<TaakZoekObject> = {}) =>
  fromPartial<TaakZoekObject>({
    id: "fakeTaakId",
    zaakUuid: "fakeZaakUuid",
    behandelaarGebruikersnaam: "fakeBehandelaar",
    ...fields,
  });

describe(TakenVrijgevenDialogComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let dialogRef: MatDialogRef<TakenVrijgevenDialogComponent>;

  const user = userEvent.setup();

  async function setup(taken: TaakZoekObject[] = [makeTaak()]) {
    dialogRef = fromPartial<MatDialogRef<TakenVrijgevenDialogComponent>>({
      close: jest.fn(),
      disableClose: false,
    });

    await render(TakenVrijgevenDialogComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            taken,
            screenEventResourceId: "fakeScreenEventResourceId",
          },
        },
        { provide: MatDialogRef, useValue: dialogRef },
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
  }

  const redenInput = () => screen.getByRole("textbox", { name: /Reden/ });
  const vrijgevenButton = () =>
    screen.getByRole("button", { name: "actie.vrijgeven" });

  async function fillInRedenAndSubmit(reden = "fakeReden") {
    await user.type(redenInput(), reden);
    await user.click(vrijgevenButton());
    await sleep();
  }

  it("announces that a single taak will be released", async () => {
    await setup([makeTaak()]);

    expect(screen.getByText("msg.vrijgeven.taak")).toBeVisible();
    expect(screen.queryByText("msg.vrijgeven.taken")).toBeNull();
  });

  it("announces that multiple taken will be released", async () => {
    await setup([makeTaak({ id: "fakeTaakId1" }), makeTaak({ id: "t2" })]);

    expect(screen.getByText("msg.vrijgeven.taken")).toBeVisible();
    expect(screen.queryByText("msg.vrijgeven.taak")).toBeNull();
  });

  it("does not let you fill in a reden when there is nothing to release", async () => {
    await setup([]);

    expect(redenInput()).toBeDisabled();
    expect(vrijgevenButton()).toBeDisabled();
  });

  it("cannot be submitted before a reden is filled in", async () => {
    await setup();

    expect(vrijgevenButton()).toBeDisabled();

    await user.type(redenInput(), "fakeReden");

    expect(vrijgevenButton()).toBeEnabled();
  });

  it("closes the dialog without releasing when the close button is used", async () => {
    await setup();

    await user.click(screen.getByRole("button", { name: "actie.sluiten" }));

    expect(dialogRef.close).toHaveBeenCalledWith(false);
    httpTestingController.expectNone(() => true);
  });

  it("releases only the taken that have a behandelaar", async () => {
    await setup([
      makeTaak({
        id: "fakeTaakId1",
        zaakUuid: "fakeZaakUuid1",
        behandelaarGebruikersnaam: "fakeBehandelaar",
      }),
      makeTaak({
        id: "fakeTaakId2",
        zaakUuid: "fakeZaakUuid2",
        behandelaarGebruikersnaam: undefined,
      }),
    ]);

    await fillInRedenAndSubmit("fakeReden");

    const request = httpTestingController.expectOne(
      "/rest/taken/lijst/vrijgeven",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual(
      expect.objectContaining({
        reden: "fakeReden",
        screenEventResourceId: "fakeScreenEventResourceId",
        taken: [{ taakId: "fakeTaakId1", zaakUuid: "fakeZaakUuid1" }],
      }),
    );
    request.flush(null);
  });

  it("locks the dialog while the release is in flight", async () => {
    await setup();

    await fillInRedenAndSubmit();

    expect(dialogRef.disableClose).toBe(true);
    httpTestingController.expectOne("/rest/taken/lijst/vrijgeven").flush(null);
  });

  it("closes the dialog once the taken are released", async () => {
    await setup();

    await fillInRedenAndSubmit();
    httpTestingController.expectOne("/rest/taken/lijst/vrijgeven").flush(null);
    await sleep();

    expect(dialogRef.close).toHaveBeenCalledWith(true);
    expect(dialogRef.disableClose).toBe(false);
  });
});
