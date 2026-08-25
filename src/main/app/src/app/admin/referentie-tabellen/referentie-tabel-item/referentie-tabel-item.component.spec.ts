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
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelItemComponent } from "./referentie-tabel-item.component";
import { ReferentieTabelValueDialogComponent } from "./referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";

const tabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  name: "Tabel A",
  values: [
    { id: 10, name: "Waarde A1", systemValue: false },
    { id: 11, name: "Waarde A2", systemValue: true },
  ],
});

describe(ReferentieTabelItemComponent.name, () => {
  let dialogOpen: jest.SpyInstance;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  async function setup(
    referenceTable: GeneratedType<"RestReferenceTable"> = tabel,
  ) {
    await render(ReferentieTabelItemComponent, {
      inputs: { tabel: referenceTable },
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    });

    httpTestingController = TestBed.inject(HttpTestingController);
    dialogOpen = jest.spyOn(TestBed.inject(MatDialog), "open").mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({
        afterClosed: () => of(false),
      }),
    );
    jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);
  }

  function buttonInRowOf(value: string, action: string) {
    const row = screen.getByRole("row", { name: new RegExp(value) });
    return within(row).getByRole("button", { name: action });
  }

  it("renders a row per value", async () => {
    await setup();

    expect(screen.getByRole("row", { name: /Waarde A1/ })).toBeVisible();
    expect(screen.getByRole("row", { name: /Waarde A2/ })).toBeVisible();
  });

  it("shows an empty message when there are no values", async () => {
    await setup(
      fromPartial<GeneratedType<"RestReferenceTable">>({
        ...tabel,
        values: [],
      }),
    );

    expect(screen.queryAllByRole("row", { name: /Waarde/ })).toHaveLength(0);
    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("disables the edit and delete buttons for system values", async () => {
    await setup();

    expect(buttonInRowOf("Waarde A2", "actie.bewerken")).toBeDisabled();
    expect(buttonInRowOf("Waarde A2", "actie.verwijderen")).toBeDisabled();
    expect(buttonInRowOf("Waarde A1", "actie.bewerken")).toBeEnabled();
    expect(buttonInRowOf("Waarde A1", "actie.verwijderen")).toBeEnabled();
  });

  it("opens the value dialog to add a value", async () => {
    await setup();

    await user.click(
      screen.getByRole("button", { name: "referentietabel.waarde-toevoegen" }),
    );

    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelValueDialogComponent,
      expect.objectContaining({ data: { tabel } }),
    );
  });

  it("opens the value dialog to edit the value of the row", async () => {
    await setup();

    await user.click(buttonInRowOf("Waarde A1", "actie.bewerken"));

    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelValueDialogComponent,
      expect.objectContaining({ data: { tabel, value: tabel.values![0] } }),
    );
  });

  it("asks for confirmation naming the value to delete", async () => {
    await setup();

    await user.click(buttonInRowOf("Waarde A1", "actie.verwijderen"));

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe(
      "msg.referentietabel.waarde-verwijderen-bevestigen",
    );
    expect(dialogData._melding.args).toEqual({ value: "Waarde A1" });
  });

  it("does not send the request until the confirmation dialog subscribes", async () => {
    await setup();

    await user.click(buttonInRowOf("Waarde A1", "actie.verwijderen"));

    httpTestingController.expectNone(() => true);
  });

  it("puts the table back without the deleted value once confirmed", async () => {
    await setup();

    await user.click(buttonInRowOf("Waarde A1", "actie.verwijderen"));
    dialogOpen.mock.calls[0][1].data.observable.subscribe();
    await sleep();

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      name: "Tabel A",
      values: [{ id: 11, name: "Waarde A2", systemValue: true }],
    });
  });
});
