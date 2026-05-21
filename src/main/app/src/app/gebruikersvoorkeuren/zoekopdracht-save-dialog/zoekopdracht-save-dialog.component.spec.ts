/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { provideExperimentalZonelessChangeDetection } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekopdrachtSaveDialogComponent } from "./zoekopdracht-save-dialog.component";

const makeZoekopdracht = (
  naam: string,
  overrides: Partial<GeneratedType<"RESTZoekopdracht">> = {},
): GeneratedType<"RESTZoekopdracht"> => ({
  naam,
  json: "{}",
  lijstID: "TAKEN_WERKVOORRAAD" as GeneratedType<"Werklijst">,
  ...overrides,
});

async function setup(zoekopdrachten: GeneratedType<"RESTZoekopdracht">[] = []) {
  const dialogRef = {
    close: jest.fn(),
    disableClose: false,
  } as unknown as MatDialogRef<ZoekopdrachtSaveDialogComponent>;

  await TestBed.configureTestingModule({
    imports: [
      ZoekopdrachtSaveDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideExperimentalZonelessChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      {
        provide: MAT_DIALOG_DATA,
        useValue: {
          zoekopdrachten,
          lijstID: "TAKEN_WERKVOORRAAD" as GeneratedType<"Werklijst">,
          zoekopdracht: { filters: {} },
        },
      },
      { provide: MatDialogRef, useValue: dialogRef },
    ],
  }).compileComponents();

  const fixture = TestBed.createComponent(ZoekopdrachtSaveDialogComponent);
  const component = fixture.componentInstance;
  const loader = TestbedHarnessEnvironment.loader(fixture);
  const httpTestingController = TestBed.inject(HttpTestingController);

  fixture.detectChanges();

  return { fixture, component, loader, httpTestingController, dialogRef };
}

describe(ZoekopdrachtSaveDialogComponent.name, () => {
  describe("close()", () => {
    it("closes the dialog", async () => {
      const { component, dialogRef } = await setup();
      component["close"]();
      expect(dialogRef.close).toHaveBeenCalled();
    });
  });

  describe("submit button", () => {
    it("is disabled when form is empty", async () => {
      const { loader } = await setup();
      const button = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.toevoegen/i }),
      );
      expect(await button.isDisabled()).toBe(true);
    });

    it("shows the 'toevoegen' label when the name does not match an existing zoekopdracht", async () => {
      const { component, loader } = await setup([
        makeZoekopdracht("bestaande zoekopdracht"),
      ]);
      component["form"].patchValue({ naam: "nieuwe naam" });

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.toevoegen/i }),
      );
      expect(button).toBeTruthy();
    });

    it("shows the 'wijzigen' label when the name matches an existing zoekopdracht", async () => {
      const { component, loader } = await setup([
        makeZoekopdracht("bestaande zoekopdracht"),
      ]);
      component["form"].patchValue({ naam: "bestaande zoekopdracht" });

      const button = await loader.getHarness(
        MatButtonHarness.with({ text: /actie.wijzigen/i }),
      );
      expect(button).toBeTruthy();
    });
  });

  describe("opslaan() — new zoekopdracht", () => {
    it("posts a new zoekopdracht and closes the dialog with true", async () => {
      const { component, httpTestingController, dialogRef } = await setup();
      component["form"].patchValue({ naam: "nieuwe naam" });
      component["form"].markAsDirty();

      component["opslaan"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht",
      );
      expect(req.request.method).toBe("POST");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          naam: "nieuwe naam",
          lijstID: "TAKEN_WERKVOORRAAD",
        }),
      );
      req.flush({});
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });
  });

  describe("opslaan() — existing zoekopdracht", () => {
    it("posts the existing entry with updated json and closes the dialog with true", async () => {
      const existing = makeZoekopdracht("bestaande zoekopdracht", {
        id: 42,
      });
      const { component, httpTestingController, dialogRef } = await setup([
        existing,
      ]);
      component["form"].patchValue({ naam: "bestaande zoekopdracht" });
      component["form"].markAsDirty();

      component["opslaan"]();
      await new Promise(requestAnimationFrame);

      const req = httpTestingController.expectOne(
        "/rest/gebruikersvoorkeuren/zoekopdracht",
      );
      expect(req.request.method).toBe("POST");
      expect(req.request.body).toEqual(
        expect.objectContaining({
          id: 42,
          naam: "bestaande zoekopdracht",
          lijstID: "TAKEN_WERKVOORRAAD",
        }),
      );
      req.flush({});
      await sleep();

      expect(dialogRef.close).toHaveBeenCalledWith(true);
    });
  });
});
