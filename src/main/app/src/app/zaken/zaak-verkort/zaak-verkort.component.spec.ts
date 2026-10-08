/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { LOCALE_ID } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakVerkortComponent } from "./zaak-verkort.component";

const makeZaak = (fields: Partial<GeneratedType<"RestZaak">> = {}) =>
  fromPartial<GeneratedType<"RestZaak">>({
    identificatie: "ZAAK-001",
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      omschrijving: "Testtype",
    }),
    status: fromPartial<GeneratedType<"RestZaakStatus">>({ naam: "Open" }),
    startdatum: "2026-01-01",
    einddatumGepland: null,
    einddatum: null,
    toelichting: "Testtoelichting",
    ...fields,
  });

const setup = (zaak = makeZaak()) => {
  TestBed.configureTestingModule({
    imports: [
      ZaakVerkortComponent,
      NoopAnimationsModule,
      TranslateModule.forRoot(),
    ],
    providers: [
      provideHttpClient(),
      provideRouter([]),
      { provide: LOCALE_ID, useValue: "nl" },
    ],
  });
  const fixture: ComponentFixture<ZaakVerkortComponent> =
    TestBed.createComponent(ZaakVerkortComponent);
  fixture.componentRef.setInput("zaak", zaak);
  fixture.detectChanges();
  return { fixture };
};

const exceededWarning = () => screen.queryByTitle("msg.datum.overschreden");

describe(ZaakVerkortComponent.name, () => {
  it("renders the zaak identificatie in the card title", () => {
    setup(makeZaak({ identificatie: "ZAAK-2026-001" }));

    expect(screen.getByText("zaak ZAAK-2026-001")).toBeVisible();
  });

  it("renders zaaktype omschrijving as subtitle", () => {
    setup(
      makeZaak({
        zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
          omschrijving: "Bijzonder type",
        }),
      }),
    );

    expect(screen.getByText("Bijzonder type")).toBeVisible();
  });

  it("renders link to zaak detail page", () => {
    setup(makeZaak({ identificatie: "ZAAK-999" }));

    expect(
      screen.getByRole("link", { name: "ZAAK-999 openen" }),
    ).toHaveAttribute("href", "/zaken/ZAAK-999");
  });

  it("renders status naam via empty pipe", () => {
    setup(
      makeZaak({
        status: fromPartial<GeneratedType<"RestZaakStatus">>({
          naam: "In behandeling",
        }),
      }),
    );

    expect(screen.getByText("In behandeling")).toBeVisible();
  });

  it("renders startdatum via datum pipe", () => {
    setup(makeZaak({ startdatum: "2026-03-15" }));

    expect(screen.getByText("15‑03‑2026")).toBeVisible();
  });

  it("renders toelichting value", () => {
    setup(makeZaak({ toelichting: "Mijn toelichting" }));

    expect(screen.getByText("Mijn toelichting")).toBeVisible();
  });

  it("warns that the planned end date has passed for an open zaak", () => {
    setup(makeZaak({ einddatumGepland: "2020-01-01", einddatum: null }));

    expect(exceededWarning()).toBeInTheDocument();
  });

  it("does not warn about a planned end date that is still ahead", () => {
    setup(makeZaak({ einddatumGepland: "2999-01-01", einddatum: null }));

    expect(exceededWarning()).not.toBeInTheDocument();
  });

  it("warns that the zaak was closed after its planned end date", () => {
    setup(
      makeZaak({ einddatumGepland: "2020-01-01", einddatum: "2020-06-01" }),
    );

    expect(exceededWarning()).toBeInTheDocument();
  });

  it("does not warn when the zaak was closed before its planned end date", () => {
    setup(
      makeZaak({ einddatumGepland: "2020-06-01", einddatum: "2020-01-01" }),
    );

    expect(exceededWarning()).not.toBeInTheDocument();
  });

  it("re-evaluates the warning against the einddatum of a changed zaak", () => {
    const { fixture } = setup(
      makeZaak({ einddatumGepland: "2020-06-01", einddatum: "2020-01-01" }),
    );

    fixture.componentRef.setInput(
      "zaak",
      makeZaak({ einddatumGepland: "2020-06-01", einddatum: "2020-12-01" }),
    );
    fixture.detectChanges();

    expect(exceededWarning()).toBeInTheDocument();
  });

  it("shows the identificatie of a changed zaak", () => {
    const { fixture } = setup(makeZaak({ identificatie: "ZAAK-2026-001" }));

    fixture.componentRef.setInput(
      "zaak",
      makeZaak({ identificatie: "ZAAK-2026-002" }),
    );
    fixture.detectChanges();

    expect(screen.getByText("zaak ZAAK-2026-002")).toBeVisible();
  });
});
