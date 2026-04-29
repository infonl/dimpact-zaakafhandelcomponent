/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { By } from "@angular/platform-browser";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatButtonHarness } from "@angular/material/button/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZacDate } from "../../shared/form/date/date";
import { ZakenService } from "../zaken.service";
import { ZaakOpschortenDialogComponent } from "./zaak-opschorten-dialog.component";

const makeZaak = (fields: Partial<GeneratedType<"RestZaak">> = {}) =>
  fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid-1",
    identificatie: "ZAAK-001",
    uiterlijkeEinddatumAfdoening: "2026-12-31",
    einddatumGepland: null,
    ...fields,
  });

const setup = (zaak = makeZaak()) => {
  const dialogRefMock = {
    close: jest.fn(),
    disableClose: false,
    afterOpened: jest.fn().mockReturnValue(of(undefined)),
  };

  TestBed.configureTestingModule({
    imports: [
      ZaakOpschortenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      provideNativeDateAdapter(),
      { provide: MAT_DIALOG_DATA, useValue: { zaak } },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });

  const zakenService = TestBed.inject(ZakenService);
  jest
    .spyOn(zakenService, "suspendZaak")
    .mockReturnValue(of(makeZaak()) as never);

  const fixture: ComponentFixture<ZaakOpschortenDialogComponent> =
    TestBed.createComponent(ZaakOpschortenDialogComponent);
  fixture.detectChanges();

  return { fixture, component: fixture.componentInstance, dialogRefMock, zakenService };
};

describe(ZaakOpschortenDialogComponent.name, () => {
  it("does not show einddatumGepland field when zaak has no einddatumGepland", () => {
    const { fixture } = setup(makeZaak({ einddatumGepland: null }));
    const dateFields = fixture.debugElement.queryAll(By.directive(ZacDate));
    expect(dateFields.length).toBe(1);
  });

  it("shows einddatumGepland field when zaak has einddatumGepland", () => {
    const { fixture } = setup(makeZaak({ einddatumGepland: "2026-06-30" }));
    const dateFields = fixture.debugElement.queryAll(By.directive(ZacDate));
    expect(dateFields.length).toBe(2);
  });

  it("submit button is disabled when form is invalid", async () => {
    const { fixture } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const submitButton = await loader.getHarness(
      MatButtonHarness.with({ selector: "#zaakOpschorten_button" }),
    );
    expect(await submitButton.isDisabled()).toBe(true);
  });

  it("close() dismisses the dialog without a result", () => {
    const { component, dialogRefMock } = setup();
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith();
  });

  it("opschorten() calls suspendZaak with correct uuid and form values", () => {
    const { component, zakenService } = setup(makeZaak({ uuid: "test-uuid" }));
    component["form"].setValue(
      {
        numberOfDays: 5,
        einddatumGepland: null,
        uiterlijkeEinddatumAfdoening: null,
        reason: "Test reden voor opschorten",
      },
      { emitEvent: false },
    );

    component["opschorten"]();

    expect(zakenService.suspendZaak).toHaveBeenCalledWith(
      "test-uuid",
      expect.objectContaining({ numberOfDays: 5, reason: "Test reden voor opschorten" }),
    );
  });

  it("closes dialog with zaak result after successful suspension", () => {
    const mockResult = makeZaak({ identificatie: "ZAAK-RESULT" });
    const { component, zakenService, dialogRefMock } = setup();
    jest
      .spyOn(zakenService, "suspendZaak")
      .mockReturnValue(of(mockResult) as never);

    component["form"].setValue(
      {
        numberOfDays: 3,
        einddatumGepland: null,
        uiterlijkeEinddatumAfdoening: null,
        reason: "Reden",
      },
      { emitEvent: false },
    );
    component["opschorten"]();

    expect(dialogRefMock.close).toHaveBeenCalledWith(mockResult);
  });
});
