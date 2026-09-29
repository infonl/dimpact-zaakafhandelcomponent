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
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";
import { TaakZoekObjectComponent } from "./taak-zoek-object.component";

const makeTaak = (fields: Partial<TaakZoekObject> = {}) =>
  fromPartial<TaakZoekObject>({
    type: "TAAK",
    id: "fakeTaakId",
    naam: "fakeTaakNaam",
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    status: "AFGEROND",
    zaakIdentificatie: "fakeZaakIdentificatie",
    behandelaarNaam: "fakeBehandelaarNaam",
    groepNaam: "fakeGroepNaam",
    creatiedatum: "2026-01-10",
    fataledatum: "2026-04-10",
    toekenningsdatum: "2026-01-12",
    toelichting: "fakeToelichting",
    ...fields,
  });

describe(TaakZoekObjectComponent.name, () => {
  let fixture: ComponentFixture<TaakZoekObjectComponent>;
  let sideNav: MatSidenav;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TaakZoekObjectComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([{ path: "**", children: [] }])],
    }).compileComponents();

    sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
    fixture = TestBed.createComponent(TaakZoekObjectComponent);
    fixture.componentRef.setInput("taak", makeTaak());
    fixture.componentRef.setInput("sideNav", sideNav);
    fixture.detectChanges();
  });

  it.each([
    ["zaaktype", "fakeZaaktypeOmschrijving"],
    ["status", "taak.status.AFGEROND"],
    ["zaakIdentificatie", "fakeZaakIdentificatie"],
    ["behandelaar", "fakeBehandelaarNaam"],
    ["groep", "fakeGroepNaam"],
    ["creatiedatum", "01/10/2026"],
    ["fataledatum", "04/10/2026"],
    ["toekenningsdatum", "01/12/2026"],
    ["toelichting", "fakeToelichting"],
  ])("shows the %s of the taak under its label", (label, value) => {
    expect(screen.getByText(label)).toBeVisible();
    expect(fixture.nativeElement).toHaveTextContent(`${label} ${value}`);
  });

  it("links to the taak", () => {
    expect(screen.getByRole("link", { name: "fakeTaakNaam" })).toHaveAttribute(
      "href",
      "/taken/fakeTaakId",
    );
  });

  it("closes the side nav when the link to the taak is followed", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(screen.getByRole("link", { name: "fakeTaakNaam" }));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  it("shows the taak that replaced the previous one", () => {
    fixture.componentRef.setInput(
      "taak",
      makeTaak({
        id: "fakeTaakId2",
        naam: "fakeTaakNaam2",
        zaaktypeOmschrijving: "fakeZaaktypeOmschrijving2",
      }),
    );
    fixture.detectChanges();

    expect(screen.getByText("fakeZaaktypeOmschrijving2")).toBeVisible();
    expect(
      screen.queryByText("fakeZaaktypeOmschrijving"),
    ).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "fakeTaakNaam2" })).toHaveAttribute(
      "href",
      "/taken/fakeTaakId2",
    );
  });
});
