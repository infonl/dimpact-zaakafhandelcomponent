/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MAT_DIALOG_DATA, MatDialogRef } from "@angular/material/dialog";
import { MatToolbarHarness } from "@angular/material/toolbar/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenService } from "../zaken.service";
import { ZakenVrijgevenDialogComponent } from "./zaken-vrijgeven-dialog.component";

const makeZaakZoekObject = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({
    id: "uuid-1",
    identificatie: "ZAAK-001",
    behandelaarGebruikersnaam: "user1",
    ...fields,
  });

const setup = (data: ZaakZoekObject[]) => {
  const dialogRefMock = { close: jest.fn(), disableClose: false };
  const mutationFn = jest.fn().mockResolvedValue(undefined);
  TestBed.configureTestingModule({
    imports: [
      ZakenVrijgevenDialogComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      provideQueryClient(testQueryClient),
      { provide: MAT_DIALOG_DATA, useValue: data },
      { provide: MatDialogRef, useValue: dialogRefMock },
    ],
  });
  const zakenService = TestBed.inject(ZakenService);
  jest.spyOn(zakenService, "vrijgevenVanuitLijst").mockReturnValue({
    mutationKey: ["vrijgeven"],
    mutationFn,
  } as ReturnType<ZakenService["vrijgevenVanuitLijst"]>);
  const fixture: ComponentFixture<ZakenVrijgevenDialogComponent> =
    TestBed.createComponent(ZakenVrijgevenDialogComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance, dialogRefMock };
};

describe(ZakenVrijgevenDialogComponent.name, () => {
  beforeEach(() => notifyManager.setScheduler((fn) => fn()));
  afterEach(() => notifyManager.setScheduler((fn) => setTimeout(fn, 0)));

  it("shows singular title when one zaak is selected", async () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const loader = TestbedHarnessEnvironment.loader(fixture);
    const toolbar = await loader.getHarness(MatToolbarHarness);
    expect(await (await toolbar.host()).text()).toContain(
      "title.zaak.vrijgeven",
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
      "title.zaken.vrijgeven",
    );
  });

  it("closes dialog with false when close is called", () => {
    const { component, dialogRefMock } = setup([makeZaakZoekObject()]);
    component["close"]();
    expect(dialogRefMock.close).toHaveBeenCalledWith(false);
  });

  it("disables the form when no zaken are provided", () => {
    const { component } = setup([]);
    expect(component["form"].disabled).toBe(true);
  });

  it("shows singular info message when one zaak is selected", () => {
    const { fixture } = setup([makeZaakZoekObject()]);
    const paragraphs: NodeListOf<HTMLParagraphElement> =
      fixture.nativeElement.querySelectorAll("p");
    expect(
      Array.from(paragraphs).some((p) =>
        p.textContent?.includes("msg.vrijgeven.zaak"),
      ),
    ).toBe(true);
  });

  it("shows plural info message when multiple zaken are selected", () => {
    const { fixture } = setup([
      makeZaakZoekObject({ id: "a" }),
      makeZaakZoekObject({ id: "b" }),
    ]);
    const paragraphs: NodeListOf<HTMLParagraphElement> =
      fixture.nativeElement.querySelectorAll("p");
    expect(
      Array.from(paragraphs).some((p) =>
        p.textContent?.includes("msg.vrijgeven.zaken"),
      ),
    ).toBe(true);
  });

  it("vrijgeven filters zaken without behandelaar", () => {
    const { component } = setup([
      makeZaakZoekObject({ id: "a", behandelaarGebruikersnaam: "user1" }),
      makeZaakZoekObject({ id: "b", behandelaarGebruikersnaam: undefined }),
    ]);
    component["vrijgeven"]();
    const variables = (
      component["mutation"] as unknown as { variables: () => unknown }
    ).variables();
    expect(variables).toMatchObject({ uuids: ["a"] });
  });
});
