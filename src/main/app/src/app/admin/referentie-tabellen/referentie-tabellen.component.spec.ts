/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { createMutationOptions, fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabelCreateDialogComponent } from "./referentie-tabel-create-dialog/referentie-tabel-create-dialog.component";
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog/referentie-tabel-edit-dialog.component";
import { ReferentieTabellenComponent } from "./referentie-tabellen.component";

const tabellen = fromPartial<GeneratedType<"RestReferenceTable">[]>([
  {
    id: 1,
    code: "TABEL_A",
    name: "Tabel A",
    systemTable: false,
    valuesCount: 2,
  },
  {
    id: 2,
    code: "TABEL_B",
    name: "Tabel B",
    systemTable: true,
    valuesCount: 1,
  },
]);

const geladenTabelA = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  name: "Tabel A",
  systemTable: false,
  values: [
    { id: 10, name: "Waarde A1" },
    { id: 11, name: "Waarde A2" },
  ],
});

describe(ReferentieTabellenComponent.name, () => {
  let fixture: ComponentFixture<ReferentieTabellenComponent>;
  let service: ReferentieTabelService;
  let setTitle: jest.SpyInstance;
  let dialogOpen: jest.SpyInstance;

  const user = userEvent.setup();

  // jsdom has no scrollIntoView; stub it per test and restore to avoid leaking.
  const originalScrollIntoView = Element.prototype.scrollIntoView;

  async function flushRendering() {
    fixture.detectChanges();
    await fixture.whenStable();
    await sleep();
    fixture.detectChanges();
    await fixture.whenStable();
  }

  async function setup({ seedDetail = false } = {}) {
    const rendered = await render(ReferentieTabellenComponent, {
      detectChangesOnRender: false,
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        provideRouter([]),
        { provide: ConfiguratieService, useValue: {} },
      ],
    });
    fixture = rendered.fixture;

    service = TestBed.inject(ReferentieTabelService);
    jest
      .spyOn(service, "deleteReferentieTabel")
      .mockReturnValue(createMutationOptions(undefined) as never);

    const utilService = TestBed.inject(UtilService);
    setTitle = jest
      .spyOn(utilService, "setTitle")
      .mockImplementation(() => undefined);
    jest.spyOn(utilService, "openSnackbar").mockImplementation(() => undefined);

    dialogOpen = jest
      .spyOn(TestBed.inject(MatDialog), "open")
      .mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(false) }),
      );

    testQueryClient.setQueryData(
      service.listReferentieTabellenQuery().queryKey,
      tabellen,
    );
    if (seedDetail) {
      testQueryClient.setQueryData(
        service.readReferentieTabelQuery(1).queryKey,
        geladenTabelA,
      );
    }

    await flushRendering();
  }

  beforeEach(() => {
    Element.prototype.scrollIntoView = jest.fn();
  });

  afterEach(() => {
    Element.prototype.scrollIntoView = originalScrollIntoView;
  });

  it("sets the title and shows a row per reference table", async () => {
    await setup();

    expect(setTitle).toHaveBeenCalledWith(
      "title.referentietabellen",
      undefined,
    );
    expect(screen.getByRole("button", { name: "TABEL_A" })).toBeVisible();
    expect(screen.getByRole("button", { name: "TABEL_B" })).toBeVisible();
    expect(screen.getByText("Tabel A")).toBeVisible();
    expect(screen.getByText("Tabel B")).toBeVisible();
  });

  it("shows the values of a table once its row is expanded", async () => {
    await setup({ seedDetail: true });

    expect(screen.queryByText("Waarde A1")).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "TABEL_A" }));
    await flushRendering();

    expect(screen.getByText("Waarde A1")).toBeVisible();
  });

  it("opens the create dialog from the add button", async () => {
    await setup();

    await user.click(
      screen.getByRole("button", { name: "referentietabel.toevoegen" }),
    );

    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelCreateDialogComponent,
      expect.objectContaining({ width: "500px" }),
    );
  });

  it("expands and scrolls to the table returned by the create dialog", async () => {
    await setup({ seedDetail: true });
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(1) }),
    );

    await user.click(
      screen.getByRole("button", { name: "referentietabel.toevoegen" }),
    );
    await flushRendering();

    expect(screen.getByText("Waarde A1")).toBeVisible();
    expect(Element.prototype.scrollIntoView).toHaveBeenCalled();
  });

  it("asks for confirmation naming the table and deletes it once confirmed", async () => {
    await setup();
    dialogOpen.mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({ afterClosed: () => of(true) }),
    );

    await user.click(
      screen.getByRole("button", { name: "actie.verwijderen TABEL_A" }),
    );
    await sleep();

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe("msg.tabel.verwijderen-bevestigen");
    expect(dialogData._melding.args).toEqual({ tabel: "TABEL_A" });
    expect(service.deleteReferentieTabel).toHaveBeenCalledWith(tabellen[0]);
  });

  it("does not offer deletion of a system table", async () => {
    await setup();

    expect(
      screen.getByRole("button", { name: "actie.verwijderen TABEL_B" }),
    ).toBeDisabled();
  });

  it("loads the full table before opening the rename dialog", async () => {
    await setup({ seedDetail: true });

    await user.click(
      screen.getByRole("button", { name: "actie.bewerken TABEL_A" }),
    );
    await sleep();

    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelEditDialogComponent,
      expect.objectContaining({ data: geladenTabelA }),
    );
  });
});
