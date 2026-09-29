/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter, Router } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BAGService } from "../bag.service";
import { BagZoekComponent } from "./bag-zoek.component";

const makeBagObject = (
  fields: Partial<GeneratedType<"RESTBAGObject">> = {},
): GeneratedType<"RESTBAGObject"> =>
  fromPartial<GeneratedType<"RESTBAGObject">>({
    identificatie: "0363010000012345",
    bagObjectType: "ADRES",
    ...fields,
  });

describe(BagZoekComponent.name, () => {
  let component: BagZoekComponent;
  let fixture: ComponentFixture<BagZoekComponent>;
  let bagService: BAGService;
  let sideNav: MatDrawer;

  const user = userEvent.setup();

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BagZoekComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    bagService = TestBed.inject(BAGService);
    sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    fixture = TestBed.createComponent(BagZoekComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("sideNav", sideNav);
    fixture.detectChanges();
  });

  function mockSearchResults(...resultaten: GeneratedType<"RESTBAGObject">[]) {
    jest
      .spyOn(bagService, "listAdressen")
      .mockReturnValue(
        of({ resultaten }) as unknown as ReturnType<
          typeof bagService.listAdressen
        >,
      );
  }

  async function search(...resultaten: GeneratedType<"RESTBAGObject">[]) {
    mockSearchResults(...resultaten);
    await user.type(
      screen.getByRole("textbox", { name: "bagObjecten" }),
      "fakeTrefwoord",
    );
    await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
    fixture.detectChanges();
  }

  function listenForSelectedBagObjects() {
    const selected: GeneratedType<"RESTBAGObject">[] = [];
    component.bagObject.subscribe((bagObject) => selected.push(bagObject));
    fixture.detectChanges();
    return selected;
  }

  const koppelButton = (identificatie: string) =>
    within(
      screen.getByRole("row", { name: new RegExp(identificatie) }),
    ).getByRole("button", { name: "actie.koppelen" });

  describe("zoek", () => {
    it("should call bagService with trefwoorden and populate bagObjecten", () => {
      const bagObject = makeBagObject();
      mockSearchResults(bagObject);

      component["trefwoorden"].setValue("Teststraat 1");
      component["zoek"]();

      expect(bagService.listAdressen).toHaveBeenCalledWith({
        trefwoorden: "Teststraat 1",
      });
      expect(component["bagObjecten"].data).toEqual([bagObject]);
    });

    it("should not call bagService when trefwoorden is empty", () => {
      jest.spyOn(bagService, "listAdressen");
      component["trefwoorden"].setValue("");
      component["zoek"]();
      expect(bagService.listAdressen).not.toHaveBeenCalled();
    });
  });

  describe("wissen", () => {
    it("should reset trefwoorden and clear bagObjecten", () => {
      component["trefwoorden"].setValue("Teststraat");
      component["bagObjecten"].data = [makeBagObject()];

      component["wissen"]();

      expect(component["trefwoorden"].value).toBeNull();
      expect(component["bagObjecten"].data).toHaveLength(0);
    });
  });

  describe("selectBagObject", () => {
    it("adds the selected object to the gekoppelde array it was given and emits it", () => {
      const gekoppeldeBagObjecten: GeneratedType<"RESTBAGObject">[] = [];
      fixture.componentRef.setInput(
        "gekoppeldeBagObjecten",
        gekoppeldeBagObjecten,
      );
      const bagObject = makeBagObject();
      const selected = listenForSelectedBagObjects();

      component["selectBagObject"](bagObject);

      expect(gekoppeldeBagObjecten).toEqual([bagObject]);
      expect(selected).toEqual([bagObject]);
    });

    it("should update FormControl value and emit when gekoppeldeBagObjecten is a FormControl", () => {
      const existing = makeBagObject({ identificatie: "existing" });
      const newObject = makeBagObject({ identificatie: "new" });
      const control = new FormControl<GeneratedType<"RESTBAGObject">[] | null>([
        existing,
      ]);
      fixture.componentRef.setInput("gekoppeldeBagObjecten", control);
      const selected = listenForSelectedBagObjects();

      component["selectBagObject"](newObject);

      expect(control.value).toEqual([existing, newObject]);
      expect(selected).toEqual([newObject]);
    });
  });

  describe("reedsGekoppeld", () => {
    it("should return true when identificatie and bagObjectType both match", () => {
      fixture.componentRef.setInput("gekoppeldeBagObjecten", [
        makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
      ]);
      expect(
        component["reedsGekoppeld"](
          makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
        ),
      ).toBe(true);
    });

    it("should return false when identificatie differs", () => {
      fixture.componentRef.setInput("gekoppeldeBagObjecten", [
        makeBagObject({ identificatie: "123" }),
      ]);
      expect(
        component["reedsGekoppeld"](makeBagObject({ identificatie: "456" })),
      ).toBe(false);
    });

    it("should return false when bagObjectType differs", () => {
      fixture.componentRef.setInput("gekoppeldeBagObjecten", [
        makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
      ]);
      expect(
        component["reedsGekoppeld"](
          makeBagObject({ identificatie: "123", bagObjectType: "PAND" }),
        ),
      ).toBe(false);
    });
  });

  describe("expandable", () => {
    it("should return false for non-ADRES bag objects", () => {
      expect(
        component["expandable"](makeBagObject({ bagObjectType: "PAND" })),
      ).toBeFalsy();
    });

    it("should return false for ADRES without child objects", () => {
      expect(
        component["expandable"](
          makeBagObject({
            bagObjectType: "ADRES",
            openbareRuimte: undefined,
            nummeraanduiding: undefined,
            woonplaats: undefined,
            panden: [],
          } as Partial<GeneratedType<"RESTBAGAdres">>),
        ),
      ).toBeFalsy();
    });

    it("should return truthy for ADRES with nummeraanduiding", () => {
      expect(
        component["expandable"](
          makeBagObject({
            bagObjectType: "ADRES",
            nummeraanduiding: fromPartial({
              identificatie: "0363200000400021",
            }),
          } as Partial<GeneratedType<"RESTBAGAdres">>),
        ),
      ).toBeTruthy();
    });
  });

  describe("linking a bag object", () => {
    it("disables linking the objects that are in the gekoppelde array", async () => {
      fixture.componentRef.setInput("gekoppeldeBagObjecten", [
        makeBagObject({ identificatie: "0363010000000001" }),
      ]);
      listenForSelectedBagObjects();

      await search(
        makeBagObject({ identificatie: "0363010000000001" }),
        makeBagObject({ identificatie: "0363010000000002" }),
      );

      expect(koppelButton("0363010000000001")).toBeDisabled();
      expect(koppelButton("0363010000000002")).toBeEnabled();
    });

    it("disables linking the objects that are in the gekoppelde form control", async () => {
      fixture.componentRef.setInput(
        "gekoppeldeBagObjecten",
        new FormControl([makeBagObject({ identificatie: "0363010000000001" })]),
      );
      listenForSelectedBagObjects();

      await search(
        makeBagObject({ identificatie: "0363010000000001" }),
        makeBagObject({ identificatie: "0363010000000002" }),
      );

      expect(koppelButton("0363010000000001")).toBeDisabled();
      expect(koppelButton("0363010000000002")).toBeEnabled();
    });

    it("emits the linked object and disables linking it again when no gekoppelde objecten were given", async () => {
      const selected = listenForSelectedBagObjects();
      const bagObject = makeBagObject({ identificatie: "0363010000000001" });
      await search(bagObject);

      await user.click(koppelButton("0363010000000001"));
      fixture.detectChanges();

      expect(selected).toEqual([bagObject]);
      expect(koppelButton("0363010000000001")).toBeDisabled();
    });

    it("closes the side nav from the header", async () => {
      listenForSelectedBagObjects();

      const [header] = screen.getAllByRole("heading", {
        name: "actie.bagObject.koppelen",
      });
      await user.click(within(header).getByRole("button"));

      expect(sideNav.close).toHaveBeenCalledTimes(1);
    });
  });

  describe("viewing a bag object", () => {
    it("closes the side nav and opens the page of the bag object", async () => {
      const navigate = jest
        .spyOn(TestBed.inject(Router), "navigate")
        .mockResolvedValue(true);
      await search(makeBagObject({ identificatie: "0363010000000001" }));

      await user.click(
        within(screen.getByRole("row", { name: /0363010000000001/ })).getByRole(
          "button",
          { name: "actie.bagObject.bekijken" },
        ),
      );

      expect(sideNav.close).toHaveBeenCalledTimes(1);
      expect(navigate).toHaveBeenCalledWith([
        "/bag-objecten",
        "adres",
        "0363010000000001",
      ]);
    });
  });

  it("closes the side nav when cancelled", async () => {
    await user.click(screen.getByRole("button", { name: "actie.annuleren" }));

    expect(sideNav.close).toHaveBeenCalledTimes(1);
  });
});
