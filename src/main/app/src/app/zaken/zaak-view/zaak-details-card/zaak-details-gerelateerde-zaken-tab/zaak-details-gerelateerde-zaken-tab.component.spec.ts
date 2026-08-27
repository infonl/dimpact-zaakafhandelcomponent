/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { within } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakDetailsGerelateerdeZakenTabComponent } from "./zaak-details-gerelateerde-zaken-tab.component";

describe(ZaakDetailsGerelateerdeZakenTabComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsGerelateerdeZakenTabComponent>;

  const gerelateerdeZaak = (
    overrides: Partial<GeneratedType<"RestGerelateerdeZaak">> = {},
  ) =>
    fromPartial<GeneratedType<"RestGerelateerdeZaak">>({
      identificatie: "ZAAK-2026-0002",
      zaaktypeOmschrijving: "fakeZaaktypeOmschrijving",
      statustypeOmschrijving: "fakeStatustypeOmschrijving",
      startdatum: "2026-01-15",
      relatieType: "VERVOLG",
      rechten: fromPartial<GeneratedType<"RestGerelateerdeZaak">["rechten"]>({
        lezen: true,
      }),
      ...overrides,
    });

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderZaken = (
    gerelateerdeZaken: GeneratedType<"RestGerelateerdeZaak">[],
  ) => {
    fixture.componentRef.setInput("gerelateerdeZaken", gerelateerdeZaken);
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDetailsGerelateerdeZakenTabComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ZaakDetailsGerelateerdeZakenTabComponent,
    );
  });

  it("renders a row per gerelateerde zaak", () => {
    renderZaken([
      gerelateerdeZaak(),
      gerelateerdeZaak({ identificatie: "ZAAK-2026-0003" }),
    ]);

    expect(screen().getByText("ZAAK-2026-0002")).toBeInTheDocument();
    expect(screen().getByText("ZAAK-2026-0003")).toBeInTheDocument();
  });

  it("links to a gerelateerde zaak the user may read", () => {
    renderZaken([gerelateerdeZaak()]);

    expect(
      screen().getByRole("link", { name: "actie.zaak.bekijken" }),
    ).toHaveAttribute("href", "/zaken/ZAAK-2026-0002");
  });

  it("hides the link to a gerelateerde zaak the user may not read", () => {
    renderZaken([gerelateerdeZaak({
        rechten: fromPartial<GeneratedType<"RestGerelateerdeZaak">["rechten"]>({
          lezen: false,
        }),
      })]);

    expect(
      screen().queryByRole("link", { name: "actie.zaak.bekijken" }),
    ).toBeNull();
  });

  it("emits zaakOntkoppelen for a zaak that may be ontkoppeld", () => {
    const zaakOntkoppelen = jest.fn();
    const ontkoppelbareZaak = gerelateerdeZaak({ ontkoppelen: true });
    renderZaken([ontkoppelbareZaak]);
    fixture.componentInstance.zaakOntkoppelen.subscribe(zaakOntkoppelen);

    screen()
      .getByRole("button", { name: "actie.zaak.ontkoppelen" })
      .click();

    expect(zaakOntkoppelen).toHaveBeenCalledWith(ontkoppelbareZaak);
  });

  it("offers no ontkoppelen button for a zaak that may not be ontkoppeld", () => {
    renderZaken([gerelateerdeZaak({ ontkoppelen: false })]);

    expect(
      screen().queryByRole("button", { name: "actie.zaak.ontkoppelen" }),
    ).toBeNull();
  });
});
