/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { provideZonelessChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MatDialog, MatDialogRef } from "@angular/material/dialog";
import { MatRowHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelValueDialogComponent } from "../referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";
import { ReferentieTabelItemComponent } from "./referentie-tabel-item.component";

const tabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  naam: "Tabel A",
  waarden: [
    { id: 10, naam: "Waarde A1", systemValue: false },
    { id: 11, naam: "Waarde A2", systemValue: true },
  ],
});

describe(ReferentieTabelItemComponent.name, () => {
  let fixture: ComponentFixture<ReferentieTabelItemComponent>;
  let component: ReferentieTabelItemComponent;
  let loader: ReturnType<typeof TestbedHarnessEnvironment.loader>;
  let dialogOpen: jest.SpyInstance;
  let openSnackbar: jest.SpyInstance;

  async function setup(
    referenceTable: GeneratedType<"RestReferenceTable"> = tabel,
    afterClosed: unknown = false,
  ) {
    await TestBed.configureTestingModule({
      imports: [
        ReferentieTabelItemComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReferentieTabelItemComponent);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);

    dialogOpen = jest.spyOn(TestBed.inject(MatDialog), "open").mockReturnValue(
      fromPartial<MatDialogRef<unknown>>({
        afterClosed: () => of(afterClosed),
      }),
    );
    openSnackbar = jest
      .spyOn(TestBed.inject(UtilService), "openSnackbar")
      .mockImplementation(() => undefined);

    fixture.componentRef.setInput("tabel", referenceTable);
    fixture.detectChanges();
  }

  it("renders a row per value", async () => {
    await setup();
    const rows = await loader.getAllHarnesses(MatRowHarness);
    expect(rows).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain("Waarde A1");
    expect(fixture.nativeElement.textContent).toContain("Waarde A2");
  });

  it("shows an empty message when there are no values", async () => {
    await setup(
      fromPartial<GeneratedType<"RestReferenceTable">>({
        ...tabel,
        waarden: [],
      }),
    );
    const rows = await loader.getAllHarnesses(MatRowHarness);
    expect(rows).toHaveLength(0);
    expect(fixture.nativeElement.textContent).toContain(
      "msg.geen.gegevens.gevonden",
    );
  });

  it("disables the edit and delete buttons for system values", async () => {
    await setup();
    const buttons = await loader.getAllHarnesses(MatButtonHarness);
    const disabled = await Promise.all(
      buttons.map((button) => button.isDisabled()),
    );
    // The single system value (Waarde A2) contributes a disabled edit + delete.
    expect(disabled.filter(Boolean)).toHaveLength(2);
  });

  it("opens the value dialog to add a value", async () => {
    await setup();
    component["addWaarde"]();
    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelValueDialogComponent,
      expect.objectContaining({ data: { tabel } }),
    );
  });

  it("opens the value dialog to edit the selected value", async () => {
    await setup();
    component["editWaarde"](tabel.waarden![0]);
    expect(dialogOpen).toHaveBeenCalledWith(
      ReferentieTabelValueDialogComponent,
      expect.objectContaining({ data: { tabel, waarde: tabel.waarden![0] } }),
    );
  });

  it("confirms deletion and shows a snackbar when confirmed", async () => {
    await setup(tabel, true);
    component["deleteWaarde"](tabel.waarden![0]);

    const dialogData = dialogOpen.mock.calls[0][1].data;
    expect(dialogData._melding.key).toBe(
      "msg.referentietabel.waarde-verwijderen-bevestigen",
    );
    expect(dialogData._melding.args).toEqual({ waarde: "Waarde A1" });
    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.waarde-verwijderd",
      { waarde: "Waarde A1" },
    );
  });

  it("does not show a snackbar when deletion is cancelled", async () => {
    await setup(tabel, false);
    component["deleteWaarde"](tabel.waarden![0]);
    expect(openSnackbar).not.toHaveBeenCalled();
  });
});
