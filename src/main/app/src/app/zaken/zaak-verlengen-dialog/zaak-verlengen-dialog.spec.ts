/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatInputHarness } from "@angular/material/input/testing";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  provideQueryClient,
  provideTanStackQuery,
} from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { testQueryClient } from "../../../../setupJest";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakVerlengenDialogComponent } from "./zaak-verlengen-dialog.component";

describe("ZaakVerlengenDialogComponent", () => {
  let component: ZaakVerlengenDialogComponent;
  let fixture: ComponentFixture<ZaakVerlengenDialogComponent>;
  let dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>;
  let loader: HarnessLoader;
  let httpTestingController: HttpTestingController;

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "b7e8f9a2-4c3d-11ee-bb2f-0242ac130003",
    zaaktype: { verlengingstermijn: 10 },
    einddatumGepland: null,
    uiterlijkeEinddatumAfdoening: new Date().toISOString(),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZaakVerlengenDialogComponent],
      imports: [
        ReactiveFormsModule,
        TranslateModule.forRoot(),
        BrowserAnimationsModule,
        MaterialModule,
        MaterialFormBuilderModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
        provideQueryClient(testQueryClient),
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: { ...mockZaak },
          },
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakVerlengenDialogComponent);
    dialogRef = TestBed.inject(MatDialogRef);
    loader = TestbedHarnessEnvironment.loader(fixture);
    httpTestingController = TestBed.inject(HttpTestingController);

    component = fixture.componentInstance;

    jest.spyOn(component, "close");
    jest.spyOn(component, "verlengen");
    fixture.detectChanges();
  });

  it("should call verlengen() when submit button is clicked", async () => {
    const inputs = await loader.getAllHarnesses(MatInputHarness);
    const [verlengingsDuur, , redenVerlenging] = inputs;

    await verlengingsDuur.setValue("5");
    await redenVerlenging.setValue("Reden verlenging");

    const submitButton: HTMLButtonElement = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    );
    submitButton.click();

    await fixture.whenStable();

    expect(component.verlengen).toHaveBeenCalled();

    await fixture.whenStable();
    const req = httpTestingController.expectOne(
      `/rest/zaken/zaak/${mockZaak.uuid}/verlenging`,
    );
    expect(req.request.method).toEqual("PATCH");
    expect(req.request.body).toEqual(
      expect.objectContaining({
        duurDagen: "5",
        redenVerlenging: "Reden verlenging",
      }),
    );
  });

  it("should call close() when annuleren button is clicked", async () => {
    const dialogRefSpy = jest.spyOn(dialogRef, "close");

    const cancelButton = await loader.getHarness(
      MatButtonHarness.with({ text: /annuleren/i }),
    );
    await cancelButton.click();

    fixture.detectChanges();
    fixture.whenStable();

    expect(component.close).toHaveBeenCalled();
    expect(dialogRefSpy).toHaveBeenCalled();
  });
});
