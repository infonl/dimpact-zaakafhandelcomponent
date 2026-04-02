/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormControl } from "@angular/forms";
import { MatDrawer } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { BAGService } from "../../bag.service";
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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BagZoekComponent, NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [provideHttpClient(), provideRouter([])],
    }).compileComponents();

    bagService = TestBed.inject(BAGService);
    fixture = TestBed.createComponent(BagZoekComponent);
    component = fixture.componentInstance;
    component.sideNav = fromPartial<MatDrawer>({ close: jest.fn() });
    fixture.detectChanges();
  });

  describe("zoek", () => {
    it("should call bagService with trefwoorden and populate bagObjecten", () => {
      const bagObject = makeBagObject();
      jest
        .spyOn(bagService, "listAdressen")
        .mockReturnValue(
          of({ resultaten: [bagObject] }) as unknown as ReturnType<
            typeof bagService.listAdressen
          >,
        );

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
    it("should add object to array and emit", () => {
      const bagObject = makeBagObject();
      let emitted: GeneratedType<"RESTBAGObject"> | undefined;
      component.bagObject.subscribe((obj) => (emitted = obj));

      component["selectBagObject"](bagObject);

      expect(
        component.gekoppeldeBagObjecten as GeneratedType<"RESTBAGObject">[],
      ).toContain(bagObject);
      expect(emitted).toBe(bagObject);
    });

    it("should update FormControl value and emit when gekoppeldeBagObjecten is a FormControl", () => {
      const existing = makeBagObject({ identificatie: "existing" });
      const newObject = makeBagObject({ identificatie: "new" });
      const control = new FormControl<GeneratedType<"RESTBAGObject">[] | null>([
        existing,
      ]);
      component.gekoppeldeBagObjecten = control;

      let emitted: GeneratedType<"RESTBAGObject"> | undefined;
      component.bagObject.subscribe((obj) => (emitted = obj));

      component["selectBagObject"](newObject);

      expect(control.value).toEqual([existing, newObject]);
      expect(emitted).toBe(newObject);
    });
  });

  describe("reedsGekoppeld", () => {
    it("should return true when identificatie and bagObjectType both match", () => {
      component.gekoppeldeBagObjecten = [
        makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
      ];
      expect(
        component["reedsGekoppeld"](
          makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
        ),
      ).toBe(true);
    });

    it("should return false when identificatie differs", () => {
      component.gekoppeldeBagObjecten = [
        makeBagObject({ identificatie: "123" }),
      ];
      expect(
        component["reedsGekoppeld"](makeBagObject({ identificatie: "456" })),
      ).toBe(false);
    });

    it("should return false when bagObjectType differs", () => {
      component.gekoppeldeBagObjecten = [
        makeBagObject({ identificatie: "123", bagObjectType: "ADRES" }),
      ];
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
            nummeraanduiding: fromPartial({ identificatie: "0363200000400021" }),
          } as Partial<GeneratedType<"RESTBAGAdres">>),
        ),
      ).toBeTruthy();
    });
  });
});
