/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";
import { ZaakZoekObjectComponent } from "./zaak-zoek-object.component";

const makeZaak = (fields: Partial<ZaakZoekObject> = {}) =>
  fromPartial<ZaakZoekObject>({
    type: "ZAAK",
    id: "fakeZaakUuid",
    identificatie: "fakeZaakIdentificatie",
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    statustypeOmschrijving: "fakeStatustypeOmschrijving",
    groepNaam: "fakeGroepNaam",
    behandelaarNaam: "fakeBehandelaarNaam",
    resultaattypeOmschrijving: "fakeResultaattypeOmschrijving",
    startdatum: "2026-01-01",
    einddatumGepland: "2026-03-01",
    uiterlijkeEinddatumAfdoening: "2026-04-01",
    einddatum: "2026-02-15",
    omschrijving: "fakeOmschrijving",
    toelichting: "fakeToelichting",
    indicaties: [],
    ...fields,
  });

describe(ZaakZoekObjectComponent.name, () => {
  let fixture: ComponentFixture<ZaakZoekObjectComponent>;
  let sideNav: MatSidenav;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakZoekObjectComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([{ path: "**", children: [] }])],
    }).compileComponents();

    sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
    fixture = TestBed.createComponent(ZaakZoekObjectComponent);
    fixture.componentRef.setInput("zaak", makeZaak());
    fixture.componentRef.setInput("sideNav", sideNav);
    fixture.detectChanges();
  });

  it.each([
    ["zaaktype", "fakeZaaktypeOmschrijving"],
    ["status", "fakeStatustypeOmschrijving"],
    ["groep", "fakeGroepNaam"],
    ["behandelaar", "fakeBehandelaarNaam"],
    ["resultaat", "fakeResultaattypeOmschrijving"],
    ["startdatum", "01/01/2026"],
    ["einddatumGepland", "03/01/2026"],
    ["uiterlijkeEinddatumAfdoening", "04/01/2026"],
    ["einddatum", "02/15/2026"],
    ["omschrijving", "fakeOmschrijving"],
    ["toelichting", "fakeToelichting"],
  ])("shows the %s of the zaak under its label", (label, value) => {
    expect(screen.getByText(label)).toBeVisible();
    expect(fixture.nativeElement).toHaveTextContent(`${label} ${value}`);
  });

  it("links to the zaak", () => {
    expect(
      screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
    ).toHaveAttribute("href", "/zaken/fakeZaakIdentificatie");
  });

  it("closes the side nav when the link to the zaak is followed", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(
      screen.getByRole("link", { name: "fakeZaakIdentificatie" }),
    );

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  it("shows the zaak that replaced the previous one", () => {
    fixture.componentRef.setInput(
      "zaak",
      makeZaak({
        identificatie: "fakeZaakIdentificatie2",
        zaaktypeOmschrijving: "fakeZaaktypeOmschrijving2",
      }),
    );
    fixture.detectChanges();

    expect(screen.getByText("fakeZaaktypeOmschrijving2")).toBeVisible();
    expect(
      screen.queryByText("fakeZaaktypeOmschrijving"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "fakeZaakIdentificatie2" }),
    ).toHaveAttribute("href", "/zaken/fakeZaakIdentificatie2");
  });
});
