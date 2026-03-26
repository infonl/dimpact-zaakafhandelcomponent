/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatSidenav } from "@angular/material/sidenav";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { ZaakZoekObject } from "../../model/zaken/zaak-zoek-object";
import { ZaakZoekObjectComponent } from "./zaak-zoek-object.component";

const makeZaak = (fields: Partial<ZaakZoekObject>): ZaakZoekObject =>
  fields as unknown as ZaakZoekObject;
const makeSidenav = (fields: Partial<MatSidenav> = {}): MatSidenav =>
  fields as unknown as MatSidenav;

describe(ZaakZoekObjectComponent.name, () => {
  let component: ZaakZoekObjectComponent;
  let fixture: ComponentFixture<ZaakZoekObjectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakZoekObjectComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakZoekObjectComponent);
    component = fixture.componentInstance;
    component.zaak = makeZaak({
      type: "ZAAK",
      zaaktypeOmschrijving: "Melding openbare ruimte",
      statustypeOmschrijving: "In behandeling",
      groepNaam: "Beheer",
      behandelaarNaam: "Jan de Vries",
      resultaattypeOmschrijving: "Verleend",
      startdatum: "2026-01-01",
      einddatumGepland: "2026-03-01",
      uiterlijkeEinddatumAfdoening: "2026-04-01",
      einddatum: "2026-02-15",
      omschrijving: "Testomschrijving",
      toelichting: "Testtoelichting",
    });
    component.sideNav = makeSidenav();
    fixture.detectChanges();
  });

  it("renders all required field labels", () => {
    const labels = fixture.debugElement
      .queryAll(By.css("zac-static-text"))
      .map((el) => el.nativeElement.getAttribute("label"));
    expect(labels).toEqual(
      expect.arrayContaining([
        "zaaktype",
        "status",
        "groep",
        "behandelaar",
        "resultaat",
        "startdatum",
        "einddatumGepland",
        "uiterlijkeEinddatumAfdoening",
        "einddatum",
        "omschrijving",
        "toelichting",
      ]),
    );
  });

  it("displays zaak field values in the template", () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain("Melding openbare ruimte");
    expect(text).toContain("In behandeling");
    expect(text).toContain("Beheer");
    expect(text).toContain("Jan de Vries");
    expect(text).toContain("Testomschrijving");
    expect(text).toContain("Testtoelichting");
  });
});
