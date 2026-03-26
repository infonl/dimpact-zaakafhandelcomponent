/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { MatSidenav } from "@angular/material/sidenav";
import { TranslateModule } from "@ngx-translate/core";
import { TaakZoekObjectComponent } from "./taak-zoek-object.component";
import { TaakZoekObject } from "../../model/taken/taak-zoek-object";

const makeTaak = (fields: Partial<TaakZoekObject>): TaakZoekObject =>
  fields as unknown as TaakZoekObject;
const makeSidenav = (fields: Partial<MatSidenav> = {}): MatSidenav =>
  fields as unknown as MatSidenav;

describe(TaakZoekObjectComponent.name, () => {
  let component: TaakZoekObjectComponent;
  let fixture: ComponentFixture<TaakZoekObjectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TaakZoekObjectComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(TaakZoekObjectComponent);
    component = fixture.componentInstance;
    component.taak = makeTaak({
      type: "TAAK",
      zaaktypeOmschrijving: "Aanvraag vergunning",
      status: "AFGEROND",
      zaakIdentificatie: "ZAAK-2026-001",
      behandelaarNaam: "Piet Jansen",
      groepNaam: "Vergunningen",
      creatiedatum: "2026-01-10",
      fataledatum: "2026-04-10",
      toekenningsdatum: "2026-01-12",
      toelichting: "Taakomschrijving",
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
        "zaakIdentificatie",
        "behandelaar",
        "groep",
        "creatiedatum",
        "fataledatum",
        "toekenningsdatum",
        "toelichting",
      ]),
    );
  });

  it("displays taak field values in the template", () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain("Aanvraag vergunning");
    expect(text).toContain("ZAAK-2026-001");
    expect(text).toContain("Piet Jansen");
    expect(text).toContain("Vergunningen");
    expect(text).toContain("Taakomschrijving");
  });
});
