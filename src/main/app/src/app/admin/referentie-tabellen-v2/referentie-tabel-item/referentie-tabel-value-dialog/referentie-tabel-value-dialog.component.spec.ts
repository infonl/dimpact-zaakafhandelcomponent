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
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../../setupJest";
import { UtilService } from "../../../../core/service/util.service";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import {
  ReferentieTabelValueDialogComponent,
  ReferentieTabelValueDialogData,
} from "./referentie-tabel-value-dialog.component";

const tabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  naam: "Tabel A",
  waarden: [
    { id: 10, naam: "Waarde A1" },
    { id: 11, naam: "Waarde A2" },
  ],
});

async function setup(data: ReferentieTabelValueDialogData) {
  const dialogRef = fromPartial<
    MatDialogRef<ReferentieTabelValueDialogComponent, boolean>
  >({ close: jest.fn(), disableClose: false });

  await TestBed.configureTestingModule({
    imports: [
      ReferentieTabelValueDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialogRef },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(ReferentieTabelValueDialogComponent);
  const component = fixture.componentInstance;
  const httpTestingController = TestBed.inject(HttpTestingController);
  const utilService = TestBed.inject(UtilService);
  const openSnackbar = jest
    .spyOn(utilService, "openSnackbar")
    .mockImplementation(() => undefined);

  fixture.detectChanges();

  return { fixture, component, httpTestingController, dialogRef, openSnackbar };
}

describe(ReferentieTabelValueDialogComponent.name, () => {
  it("shows the add title and icon when no value is passed", async () => {
    const { fixture } = await setup({ tabel });
    expect(fixture.nativeElement.textContent).toContain(
      "referentietabel.waarde.toevoegen.titel",
    );
    expect(fixture.nativeElement.textContent).toContain("add_circle");
  });

  it("shows the edit title and pre-fills the name when a value is passed", async () => {
    const { component, fixture } = await setup({
      tabel,
      waarde: tabel.waarden![0],
    });
    expect(fixture.nativeElement.textContent).toContain(
      "referentietabel.waarde.wijzigen.titel",
    );
    expect(component["form"].getRawValue().naam).toBe("Waarde A1");
  });

  it("closes with false on cancel", async () => {
    const { component, dialogRef } = await setup({ tabel });
    component["close"]();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it("does not submit an empty (invalid) form", async () => {
    const { component, httpTestingController } = await setup({ tabel });
    component["submit"]();
    httpTestingController.expectNone("/rest/referentietabellen/1");
  });

  it("appends the new value, closes with true and shows a snackbar when adding", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup({ tabel });
    component["form"].setValue({ naam: "Waarde A3" });
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      naam: "Tabel A",
      waarden: [
        { id: 10, naam: "Waarde A1" },
        { id: 11, naam: "Waarde A2" },
        { naam: "Waarde A3" },
      ],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.waarde-toegevoegd",
      { waarde: "Waarde A3" },
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it("renames the selected value when editing", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup({ tabel, waarde: tabel.waarden![0] });
    component["form"].setValue({ naam: "Waarde A1 gewijzigd" });
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      naam: "Tabel A",
      waarden: [
        { id: 10, naam: "Waarde A1 gewijzigd" },
        { id: 11, naam: "Waarde A2" },
      ],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.waarde-gewijzigd",
      { waarde: "Waarde A1 gewijzigd" },
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
