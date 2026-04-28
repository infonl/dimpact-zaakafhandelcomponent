/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatProgressSpinnerHarness } from "@angular/material/progress-spinner/testing";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { IdentityService } from "../../identity/identity.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";
import { ZakenVerdelenDialogComponent } from "./zaken-verdelen-dialog.component";

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({ id: "uuid-1", identificatie: "ZAAK-001", ...fields });

const mockGroups: GeneratedType<"RestGroup">[] = [{ id: "groep-1", naam: "Groep Een" }];

const setup = (data: ZaakZoekObject[]) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.configureTestingModule({
    imports: [ZakenVerdelenDialogComponent, NoopAnimationsModule, TranslateModule.forRoot()],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const identityService = TestBed.inject(IdentityService);
  const zakenService = TestBed.inject(ZakenService);
  jest.spyOn(identityService, "listGroups").mockReturnValue(of(mockGroups));
  jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));
  const fixture: ComponentFixture<ZakenVerdelenDialogComponent> =
    TestBed.createComponent(ZakenVerdelenDialogComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, zakenService, dialogRefMock };
};

describe(ZakenVerdelenDialogComponent.name, () => {
  it("shows singular title when one zaak is selected", async () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain("title.zaak.verdelen");
  });

  it("shows plural title when multiple zaken are selected", async () => {
    const { fixture } = setup([makeZaakZoekObject({ id: "a" }), makeZaakZoekObject({ id: "b" })]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain("title.zaken.verdelen");
  });

  it("disables verdelen button when form is invalid (no groep selected)", () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector("#zakenVerdelen_button");
    expect(button.disabled).toBe(true);
  });

  it("disables verdelen button when data is empty", () => {
    const { fixture } = setup([]);
    const button: HTMLButtonElement = fixture.nativeElement.querySelector("#zakenVerdelen_button");
    expect(button.disabled).toBe(true);
  });

  it("disables verdelen button when loading", () => {
    const { fixture, component } = setup([makeZaakZoekObject()]);
    component["loading"] = true;
    component["form"].controls.groep.setValue(mockGroups[0]);
    fixture.detectChanges();
    const button: HTMLButtonElement = fixture.nativeElement.querySelector("#zakenVerdelen_button");
    expect(button.disabled).toBe(true);
  });

  it("shows spinner when loading", async () => {
    const { fixture, component } = setup([makeZaakZoekObject()]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    component["loading"] = true;
    fixture.detectChanges();
    const spinners = await loader.getAllHarnesses(MatProgressSpinnerHarness);
    expect(spinners.length).toBeGreaterThan(0);
  });

  it("closes dialog with false when cancel is clicked", () => {
    const { component, dialogRefMock } = setup([makeZaakZoekObject()]);
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it("calls verdelenVanuitLijst with correct args and closes dialog on verdeel", () => {
    const { component, zakenService, dialogRefMock } = setup([makeZaakZoekObject({ id: "uuid-1" })]);
    jest.spyOn(zakenService, "verdelenVanuitLijst").mockReturnValue(of(undefined) as never);
    component["form"].controls.groep.setValue(mockGroups[0]);
    component["verdeel"]();
    expect(zakenService.verdelenVanuitLijst).toHaveBeenCalledWith(
      expect.objectContaining({ uuids: ["uuid-1"], groepId: "groep-1" }),
    );
    expect(dialogRefMock.close).toHaveBeenCalledWith(component["form"].value);
  });
});
