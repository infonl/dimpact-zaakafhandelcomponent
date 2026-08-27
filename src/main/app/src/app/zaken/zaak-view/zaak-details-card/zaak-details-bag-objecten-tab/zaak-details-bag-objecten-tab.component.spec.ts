/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatTableDataSource } from "@angular/material/table";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { within } from "@testing-library/angular";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../../shared/utils/generated-types";
import { ZaakDetailsBagObjectenTabComponent } from "./zaak-details-bag-objecten-tab.component";

describe(ZaakDetailsBagObjectenTabComponent.name, () => {
  let fixture: ComponentFixture<ZaakDetailsBagObjectenTabComponent>;

  const bagObjectGegevens = (
    overrides: Partial<GeneratedType<"RESTBAGObject">> = {},
  ) =>
    fromPartial<GeneratedType<"RESTBAGObjectGegevens">>({
      bagObject: fromPartial<GeneratedType<"RESTBAGObject">>({
        identificatie: "fakeBagIdentificatie",
        bagObjectType: "ADRES",
        omschrijving: "fakeBagOmschrijving",
        ...overrides,
      }),
    });

  const screen = () => within(fixture.nativeElement as HTMLElement);

  const renderBagObjecten = (
    bagObjecten: GeneratedType<"RESTBAGObjectGegevens">[],
    isOntkoppelenToegestaan = true,
  ) => {
    fixture.componentRef.setInput(
      "bagObjectenDataSource",
      new MatTableDataSource(bagObjecten),
    );
    fixture.componentRef.setInput(
      "isOntkoppelenToegestaan",
      isOntkoppelenToegestaan,
    );
    fixture.detectChanges();
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakDetailsBagObjectenTabComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ZaakDetailsBagObjectenTabComponent);
  });

  it("renders a row per gekoppeld bag object", () => {
    renderBagObjecten([bagObjectGegevens()]);

    expect(screen().getByText("fakeBagOmschrijving")).toBeInTheDocument();
    expect(screen().getByText("objecttype.ADRES")).toBeInTheDocument();
  });

  it("links to the bag object by its lowercased type and identificatie", () => {
    renderBagObjecten([bagObjectGegevens()]);

    expect(
      screen().getByRole("link", { name: "actie.bagObject.bekijken" }),
    ).toHaveAttribute("href", "/bag-objecten/adres/fakeBagIdentificatie");
  });

  it("emits bagObjectVerwijderen when the user ontkoppelt a bag object", () => {
    const bagObjectVerwijderen = jest.fn();
    const gekoppeldBagObject = bagObjectGegevens();
    renderBagObjecten([gekoppeldBagObject]);
    fixture.componentInstance.bagObjectVerwijderen.subscribe(
      bagObjectVerwijderen,
    );

    screen()
      .getByRole("button", { name: "actie.bagObject.ontkoppelen" })
      .click();

    expect(bagObjectVerwijderen).toHaveBeenCalledWith(gekoppeldBagObject);
  });

  it("offers no ontkoppelen button when the user may not behandelen", () => {
    renderBagObjecten([bagObjectGegevens()], false);

    expect(
      screen().queryByRole("button", { name: "actie.bagObject.ontkoppelen" }),
    ).toBeNull();
  });
});
