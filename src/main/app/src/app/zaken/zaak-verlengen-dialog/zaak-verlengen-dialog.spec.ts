/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NO_ERRORS_SCHEMA } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatInputHarness } from "@angular/material/input/testing";
import { BrowserAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { ZakenService } from "../zaken.service";
import { ZaakVerlengenDialogComponent } from "./zaak-verlengen-dialog.component";

describe("ZaakVerlengenDialogComponent", () => {
  let component: ZaakVerlengenDialogComponent;
  let fixture: ComponentFixture<ZaakVerlengenDialogComponent>;
  let zakenService: ZakenService;
  let dialogRef: MatDialogRef<ZaakVerlengenDialogComponent>;
  let loader: HarnessLoader;

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
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            zaak: {
              uuid: "1",
              zaaktype: { verlengingstermijn: 10 },
              einddatumGepland: null,
              uiterlijkeEinddatumAfdoening: new Date(),
            },
          },
        },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakVerlengenDialogComponent);
    zakenService = TestBed.inject(ZakenService);
    dialogRef = TestBed.inject(MatDialogRef);
    loader = TestbedHarnessEnvironment.loader(fixture);

    component = fixture.componentInstance;

    jest.spyOn(component, "close");
    jest.spyOn(component, "verlengen");
    fixture.detectChanges();
  });

  it("should call verlengen() when submit button is clicked", async () => {
    const zakenServiceSpy = jest.spyOn(zakenService, "verlengenZaak");

    const inputs = await loader.getAllHarnesses(MatInputHarness);
    const [verlengingsDuur, , redenVerlenging] = inputs;

    await verlengingsDuur.setValue("5");

    await redenVerlenging.setValue("Reden verlenging");

    fixture.detectChanges();

    const submitBtn: HTMLButtonElement = fixture.nativeElement.querySelector(
      'button[type="submit"]',
    );
    submitBtn.click();

    expect(component.verlengen).toHaveBeenCalled();
    expect(zakenServiceSpy).toHaveBeenCalled();
  });

  it("should call close() when annuleren button is clicked", async () => {
    const dialogRefSpy = jest.spyOn(dialogRef, "close");

    const annulerenBtnHarness = await loader.getHarness(
      MatButtonHarness.with({ text: /annuleren/i }),
    );
    await annulerenBtnHarness.click();

    expect(component.close).toHaveBeenCalled();
    expect(dialogRefSpy).toHaveBeenCalled();
  });
});
