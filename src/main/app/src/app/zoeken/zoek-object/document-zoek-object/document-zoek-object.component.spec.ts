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
import { DocumentZoekObject } from "../../model/documenten/document-zoek-object";
import { DocumentZoekObjectComponent } from "./document-zoek-object.component";

const makeDocument = (fields: Partial<DocumentZoekObject> = {}) =>
  fromPartial<DocumentZoekObject>({
    type: "DOCUMENT",
    id: "fakeDocumentUuid",
    titel: "fakeDocumentTitel",
    zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
    status: "DEFINITIEF",
    zaakIdentificatie: "fakeZaakIdentificatie",
    documentType: "fakeDocumentType",
    auteur: "fakeAuteur",
    creatiedatum: "2026-02-01",
    verzenddatum: "2026-02-10",
    ontvangstdatum: "2026-02-05",
    beschrijving: "fakeBeschrijving",
    indicaties: [],
    ...fields,
  });

describe(DocumentZoekObjectComponent.name, () => {
  let fixture: ComponentFixture<DocumentZoekObjectComponent>;
  let sideNav: MatSidenav;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        DocumentZoekObjectComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([{ path: "**", children: [] }])],
    }).compileComponents();

    sideNav = fromPartial<MatSidenav>({ close: jest.fn() });
    fixture = TestBed.createComponent(DocumentZoekObjectComponent);
    fixture.componentRef.setInput("document", makeDocument());
    fixture.componentRef.setInput("sideNav", sideNav);
    fixture.detectChanges();
  });

  it.each([
    ["zaaktype", "fakeZaaktypeOmschrijving"],
    ["status", "informatieobject.status.DEFINITIEF"],
    ["zaakIdentificatie", "fakeZaakIdentificatie"],
    ["documentType", "fakeDocumentType"],
    ["auteur", "fakeAuteur"],
    ["creatiedatum", "02/01/2026"],
    ["verzenddatum", "02/10/2026"],
    ["ontvangstdatum", "02/05/2026"],
    ["beschrijving", "fakeBeschrijving"],
  ])("shows the %s of the document under its label", (label, value) => {
    expect(screen.getByText(label)).toBeVisible();
    expect(fixture.nativeElement).toHaveTextContent(`${label} ${value}`);
  });

  it("links to the document", () => {
    expect(
      screen.getByRole("link", { name: "fakeDocumentTitel" }),
    ).toHaveAttribute("href", "/informatie-objecten/fakeDocumentUuid");
  });

  it("closes the side nav when the link to the document is followed", async () => {
    const user = userEvent.setup({ delay: null });

    await user.click(screen.getByRole("link", { name: "fakeDocumentTitel" }));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });

  it("shows the document that replaced the previous one", () => {
    fixture.componentRef.setInput(
      "document",
      makeDocument({
        id: "fakeDocumentUuid2",
        titel: "fakeDocumentTitel2",
        zaaktypeOmschrijving: "fakeZaaktypeOmschrijving2",
      }),
    );
    fixture.detectChanges();

    expect(screen.getByText("fakeZaaktypeOmschrijving2")).toBeVisible();
    expect(
      screen.queryByText("fakeZaaktypeOmschrijving"),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "fakeDocumentTitel2" }),
    ).toHaveAttribute("href", "/informatie-objecten/fakeDocumentUuid2");
  });
});
