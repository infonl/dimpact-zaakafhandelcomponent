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
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { UtilService } from "../../../core/service/util.service";
import { ReferentieTabelCreateDialogComponent } from "./referentie-tabel-create-dialog.component";

async function setup() {
  const dialogRef = fromPartial<
    MatDialogRef<ReferentieTabelCreateDialogComponent, boolean>
  >({ close: jest.fn(), disableClose: false });

  await TestBed.configureTestingModule({
    imports: [
      ReferentieTabelCreateDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      { provide: MatDialogRef, useValue: dialogRef },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(ReferentieTabelCreateDialogComponent);
  const component = fixture.componentInstance;
  const httpTestingController = TestBed.inject(HttpTestingController);
  const openSnackbar = jest
    .spyOn(TestBed.inject(UtilService), "openSnackbar")
    .mockImplementation(() => undefined);

  fixture.detectChanges();

  return { fixture, component, httpTestingController, dialogRef, openSnackbar };
}

describe(ReferentieTabelCreateDialogComponent.name, () => {
  it("shows the add title", async () => {
    const { fixture } = await setup();
    expect(fixture.nativeElement.textContent).toContain(
      "referentietabel.toevoegen.titel",
    );
  });

  it("closes with false on cancel", async () => {
    const { component, dialogRef } = await setup();
    component["close"]();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });

  it("does not submit an empty (invalid) form", async () => {
    const { component, httpTestingController } = await setup();
    component["submit"]();
    httpTestingController.expectNone("/rest/referentietabellen");
  });

  it("creates the table, closes with true and shows a snackbar", async () => {
    const { component, httpTestingController, dialogRef, openSnackbar } =
      await setup();
    component["form"].setValue({ code: "NEW_CODE", naam: "New name" });
    component["form"].markAsDirty();

    component["submit"]();
    await new Promise(requestAnimationFrame);

    const request = httpTestingController.expectOne("/rest/referentietabellen");
    expect(request.request.method).toBe("POST");
    expect(request.request.body).toEqual({
      code: "NEW_CODE",
      naam: "New name",
      systeem: false,
      waarden: [],
    });
    request.flush({});
    await sleep();

    expect(openSnackbar).toHaveBeenCalledWith(
      "msg.referentietabel.toegevoegd",
      {
        tabel: "NEW_CODE",
      },
    );
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });
});
