/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatProgressSpinnerHarness } from "@angular/material/progress-spinner/testing";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakOntkoppelenDialogComponent } from "./zaak-ontkoppelen-dialog.component";

const dialogData: Omit<GeneratedType<"RestZaakUnlinkData">, "reden"> = {
  zaakUuid: "zaak-uuid-1",
  gekoppeldeZaakIdentificatie: "ZAAK-002",
  relatieType: "VERVOLG",
};

const setup = () => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [
      ZaakOntkoppelenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      { provide: MAT_DIALOG_DATA, useValue: dialogData },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const zakenService = TestBed.inject(ZakenService);
  const fixture: ComponentFixture<ZaakOntkoppelenDialogComponent> =
    TestBed.createComponent(ZaakOntkoppelenDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    component: fixture.componentInstance,
    zakenService,
    dialogRefMock,
  };
};

describe(ZaakOntkoppelenDialogComponent.name, () => {
  it("renders the dialog title", async () => {
    const { fixture } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "title.zaak.ontkoppelen",
    );
  });

  it("disables the submit button when the form is invalid", async () => {
    const { fixture } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "button[type='submit']" }),
    );
    expect(await button.isDisabled()).toBe(true);
  });

  it("enables the submit button when reden is filled in", async () => {
    const { fixture, component } = setup();
    component["form"].controls.reden.setValue("reden tekst");
    fixture.detectChanges();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "button[type='submit']" }),
    );
    expect(await button.isDisabled()).toBe(false);
  });

  it("disables the submit button when loading", async () => {
    const { fixture, component } = setup();
    component["loading"] = true;
    fixture.detectChanges();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "button[type='submit']" }),
    );
    expect(await button.isDisabled()).toBe(true);
  });

  it("shows spinner when loading", async () => {
    const { fixture, component } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    component["loading"] = true;
    fixture.detectChanges();
    const spinners = await loader.getAllHarnesses(MatProgressSpinnerHarness);
    expect(spinners.length).toBeGreaterThan(0);
  });

  it("calls ontkoppelZaak and closes dialog on submit", () => {
    const { component, zakenService, dialogRefMock } = setup();
    jest
      .spyOn(zakenService, "ontkoppelZaak")
      .mockReturnValue(of(undefined) as never);
    component["form"].controls.reden.setValue("mijn reden");
    component["ontkoppel"]();
    expect(zakenService.ontkoppelZaak).toHaveBeenCalledWith({
      ...dialogData,
      reden: "mijn reden",
    });
    expect(dialogRefMock.close).toHaveBeenCalledWith(true);
  });

  it("closes the dialog when cancel is clicked", async () => {
    const { fixture, dialogRefMock } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const cancelButton = await loader.getHarness(
      MatButtonHarness.with({ text: /actie.annuleren/i }),
    );
    await cancelButton.click();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });

  it("closes the dialog when the X button is clicked", async () => {
    const { fixture, dialogRefMock } = setup();
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const closeButton = await loader.getHarness(
      MatButtonHarness.with({ selector: "mat-toolbar button" }),
    );
    await closeButton.click();
    expect(dialogRefMock.close).toHaveBeenCalled();
  });
});
