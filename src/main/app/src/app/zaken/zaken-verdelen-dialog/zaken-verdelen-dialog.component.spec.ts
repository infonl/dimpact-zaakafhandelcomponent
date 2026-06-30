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
import { provideZoneChangeDetection } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonHarness } from "@angular/material/button/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { FormHelper } from "../../shared/form/helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenVerdelenDialogComponent } from "./zaken-verdelen-dialog.component";

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({
    id: "uuid-1",
    identificatie: "ZAAK-001",
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    ...fields,
  });

const mockGroups: GeneratedType<"RestGroup">[] = [
  { id: "groep-1", naam: "Groep Een" },
];

const setup = (
  data: ZaakZoekObject[],
  groups: GeneratedType<"RestGroup">[] = mockGroups,
) => {
  testQueryClient.clear();
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [
      ZakenVerdelenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideZoneChangeDetection(),
      provideHttpClient(withInterceptorsFromDi()),
      provideHttpClientTesting(),
      provideTanStackQuery(testQueryClient),
      provideRouter([]),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const identityService = TestBed.inject(IdentityService);
  const httpTestingController = TestBed.inject(HttpTestingController);
  testQueryClient.setQueryData(
    identityService.listBehandelaarGroupsForZaaktypes(
      data.map(({ zaaktypeOmschrijving }) => zaaktypeOmschrijving),
    ).queryKey,
    groups,
  );
  jest.spyOn(identityService, "listUsersInGroup").mockReturnValue(of([]));
  const fixture: ComponentFixture<ZakenVerdelenDialogComponent> =
    TestBed.createComponent(ZakenVerdelenDialogComponent);
  fixture.detectChanges();
  return {
    fixture,
    component: fixture.componentInstance,
    httpTestingController,
    dialogRefMock,
  };
};

describe(ZakenVerdelenDialogComponent.name, () => {
  it("shows singular title when one zaak is selected", async () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "title.zaak.verdelen",
    );
  });

  it("shows plural title when multiple zaken are selected", async () => {
    const { fixture } = setup([
      makeZaakZoekObject({ id: "a" }),
      makeZaakZoekObject({ id: "b" }),
    ]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "title.zaken.verdelen",
    );
  });

  it("disables verdelen button when form is invalid (no groep selected)", async () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "#zakenVerdelen_button" }),
    );
    expect(await button.isDisabled()).toBe(true);
  });

  it("disables verdelen button when data is empty", async () => {
    const { fixture } = setup([]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const button = await loader.getHarness(
      MatButtonHarness.with({ selector: "#zakenVerdelen_button" }),
    );
    expect(await button.isDisabled()).toBe(true);
  });

  it("disables verdelen button when loading", async () => {
    const { component, httpTestingController } = setup([makeZaakZoekObject()]);
    component["form"].controls.groep.setValue(mockGroups[0]);
    component["verdeel"]();
    await sleep();
    expect(component["isDisabled"]()).toBe(true);
    httpTestingController.expectOne("/rest/zaken/lijst/verdelen").flush({});
  });

  it("shows spinner when loading", async () => {
    const { fixture, component, httpTestingController } = setup([
      makeZaakZoekObject(),
    ]);
    component["form"].controls.groep.setValue(mockGroups[0]);
    component["verdeel"]();
    await sleep();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector("mat-spinner")).not.toBeNull();
    httpTestingController.expectOne("/rest/zaken/lijst/verdelen").flush({});
  });

  it("closes dialog with false when cancel is clicked", () => {
    const { component, dialogRefMock } = setup([makeZaakZoekObject()]);
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it("sends PUT request with correct args and closes dialog on success", async () => {
    const { component, dialogRefMock, httpTestingController } = setup([
      makeZaakZoekObject({ id: "uuid-1" }),
    ]);
    component["form"].controls.groep.setValue(mockGroups[0]);
    component["verdeel"]();
    await sleep();
    const req = httpTestingController.expectOne("/rest/zaken/lijst/verdelen");
    expect(req.request.method).toBe("PUT");
    expect(req.request.body).toEqual(
      expect.objectContaining({ uuids: ["uuid-1"], groepId: "groep-1" }),
    );
    req.flush({});
    await sleep();
    expect(dialogRefMock.close).toHaveBeenCalledWith(component["form"].value);
  });

  describe("when no authorised groups are found", () => {
    it("sets error on groep control", () => {
      const { component } = setup([makeZaakZoekObject()], []);
      expect(component["form"].controls.groep.errors).toEqual(
        FormHelper.CustomErrorMessage(
          "msg.error.group.no.authorised.group.for.zaken",
        ),
      );
    });

    it("marks groep control as touched so the error is shown immediately", () => {
      const { component } = setup([makeZaakZoekObject()], []);
      expect(component["form"].controls.groep.touched).toBe(true);
    });
  });
});
