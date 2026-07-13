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
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog.component";

const tabel = fromPartial<GeneratedType<"RestReferenceTable">>({
  id: 1,
  code: "TABEL_A",
  name: "Tabel A",
  values: [{ id: 10, name: "Waarde A1" }],
});

async function setup() {
  const dialogRef = fromPartial<
    MatDialogRef<ReferentieTabelEditDialogComponent, boolean>
  >({ close: jest.fn(), disableClose: false });

  await TestBed.configureTestingModule({
    imports: [
      ReferentieTabelEditDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      { provide: MAT_DIALOG_DATA, useValue: tabel },
      { provide: MatDialogRef, useValue: dialogRef },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(ReferentieTabelEditDialogComponent);
  const component = fixture.componentInstance;
  const httpTestingController = TestBed.inject(HttpTestingController);
  const openSnackbar = jest
    .spyOn(TestBed.inject(UtilService), "openSnackbar")
    .mockImplementation(() => undefined);

  fixture.detectChanges();

  return { fixture, component, httpTestingController, dialogRef, openSnackbar };
}

describe(ReferentieTabelEditDialogComponent.name, () => {
  it("pre-fills the name and keeps the code disabled", async () => {
    const { component } = await setup();
    expect(component["form"].getRawValue()).toEqual({
      code: "TABEL_A",
      name: "Tabel A",
    });
    expect(component["form"].controls.code.disabled).toBe(true);
  });

  it("closes with false on cancel", async () => {
    const { component, dialogRef } = await setup();
    component["close"]();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it("does not submit when the name is cleared (invalid)", async () => {
    const { component, httpTestingController } = await setup();
    component["form"].controls.name.setValue("");
    component["submit"]();
    httpTestingController.expectNone("/rest/referentietabellen/1");
  });

  it("accepts a name of 256 characters but does not submit a longer one", async () => {
    const { component, httpTestingController } = await setup();

    component["form"].controls.name.setValue("a".repeat(256));
    expect(component["form"].valid).toBe(true);

    component["form"].controls.name.setValue("a".repeat(257));
    expect(component["form"].controls.name.invalid).toBe(true);

    component["submit"]();
    httpTestingController.expectNone("/rest/referentietabellen/1");
  });

  it("updates the name (keeping code and values), closes with true and shows a snackbar", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup();
    component["form"].controls.name.setValue("Tabel A gewijzigd");
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne(
      "/rest/referentietabellen/1",
    );
    expect(request.request.method).toBe("PUT");
    expect(request.request.body).toEqual({
      code: "TABEL_A",
      name: "Tabel A gewijzigd",
      values: [{ id: 10, name: "Waarde A1" }],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith("msg.referentietabel.gewijzigd", {
      tabel: "TABEL_A",
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
