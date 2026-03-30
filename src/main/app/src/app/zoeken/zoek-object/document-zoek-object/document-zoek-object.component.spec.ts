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
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { DocumentZoekObjectComponent } from "./document-zoek-object.component";

const makeDocument = (
  fields: Partial<DocumentZoekObject>,
): DocumentZoekObject => fields as unknown as DocumentZoekObject;
const makeSidenav = (fields: Partial<MatSidenav> = {}): MatSidenav =>
  fields as unknown as MatSidenav;

describe(DocumentZoekObjectComponent.name, () => {
  let component: DocumentZoekObjectComponent;
  let fixture: ComponentFixture<DocumentZoekObjectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        DocumentZoekObjectComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentZoekObjectComponent);
    component = fixture.componentInstance;
    component.document = makeDocument({
      type: "DOCUMENT",
      zaaktypeOmschrijving: "Omgevingsvergunning",
      status: "DEFINITIEF",
      zaakIdentificatie: "ZAAK-2026-002",
      documentType: "Besluit",
      auteur: "Anna Bakker",
      creatiedatum: "2026-02-01",
      verzenddatum: "2026-02-10",
      ontvangstdatum: "2026-02-05",
      beschrijving: "Documentbeschrijving",
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
        "documentType",
        "auteur",
        "creatiedatum",
        "verzenddatum",
        "ontvangstdatum",
        "beschrijving",
      ]),
    );
  });

  it("displays document field values in the template", () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain("Omgevingsvergunning");
    expect(text).toContain("ZAAK-2026-002");
    expect(text).toContain("Besluit");
    expect(text).toContain("Anna Bakker");
    expect(text).toContain("Documentbeschrijving");
  });
});
