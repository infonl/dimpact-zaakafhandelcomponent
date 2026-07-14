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
  name: "Tabel A",
  values: [
    { id: 10, name: "Waarde A1" },
    { id: 11, name: "Waarde A2" },
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
      "referentietabel.waarde-toevoegen",
    );
    expect(fixture.nativeElement.textContent).toContain("add_circle");
  });

  it("shows the edit title and pre-fills the name when a value is passed", async () => {
    const { component, fixture } = await setup({
      tabel,
      value: tabel.values![0],
    });
    expect(fixture.nativeElement.textContent).toContain(
      "referentietabel.waarde-titel-wijzigen",
    );
    expect(component["form"].getRawValue().name).toBe("Waarde A1");
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

  it("accepts a value of 1000 characters but does not submit a longer one", async () => {
    const { component, httpTestingController } = await setup({ tabel });

    component["form"].setValue({ name: "a".repeat(1000) });
    expect(component["form"].valid).toBe(true);

    component["form"].setValue({ name: "a".repeat(1001) });
    expect(component["form"].invalid).toBe(true);

    component["submit"]();
    httpTestingController.expectNone("/rest/referentietabellen/1");
  });

  it("appends the new value, closes with true and shows a snackbar when adding", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup({ tabel });
    component["form"].setValue({ name: "Waarde A3" });
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      name: "Tabel A",
      values: [
        { id: 10, name: "Waarde A1" },
        { id: 11, name: "Waarde A2" },
        { name: "Waarde A3" },
      ],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.waarde-toegevoegd",
      { value: "Waarde A3" },
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it("renames the selected value when editing", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup({ tabel, value: tabel.values![0] });
    component["form"].setValue({ name: "Waarde A1 gewijzigd" });
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      name: "Tabel A",
      values: [
        { id: 10, name: "Waarde A1 gewijzigd" },
        { id: 11, name: "Waarde A2" },
      ],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.waarde-gewijzigd",
      { value: "Waarde A1 gewijzigd" },
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
