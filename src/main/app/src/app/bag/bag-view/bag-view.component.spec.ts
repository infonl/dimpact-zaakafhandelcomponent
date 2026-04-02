/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNativeDateModule } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { StaticTextComponent } from "../../shared/static-text/static-text.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { BagLocatieComponent } from "../bag-locatie/bag-locatie.component";
import { BagZakenTabelComponent } from "../bag-zaken-tabel/bag-zaken-tabel.component";
import { BAGViewComponent } from "./bag-view.component";

const makeBAGObject = (
  fields: Partial<GeneratedType<"RESTBAGObject">> = {},
): GeneratedType<"RESTBAGObject"> =>
  ({
    identificatie: "0363200000218908",
    bagObjectType: "ADRES",
    omschrijving: "Test omschrijving",
    ...fields,
  }) as Partial<
    GeneratedType<"RESTBAGObject">
  > as unknown as GeneratedType<"RESTBAGObject">;

describe(BAGViewComponent.name, () => {
  let fixture: ComponentFixture<BAGViewComponent>;
  let component: BAGViewComponent;
  let utilService: UtilService;

  const configureTestBed = async (
    bagObject: GeneratedType<"RESTBAGObject">,
  ) => {
    await TestBed.configureTestingModule({
      imports: [
        BAGViewComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
        MatNativeDateModule,
        StaticTextComponent,
        BagZakenTabelComponent,
        BagLocatieComponent,
      ],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { data: of({ bagObject }) },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BAGViewComponent);
    component = fixture.componentInstance;
    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "setTitle");
    jest.spyOn(utilService, "setLoading");
    fixture.detectChanges();
  };

  describe("when bagObjectType is ADRES", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "ADRES",
          identificatie: "0363200000218908",
        }),
      );
    });

    it("sets the adres property and its geometry", () => {
      expect(component["adres"]?.identificatie).toBe("0363200000218908");
    });

    it("sets bagIdentificatie from the bagObject", () => {
      expect(component["bagIdentificatie"]).toBe("0363200000218908");
    });

    it("calls utilService.setTitle with bagobjectgegevens", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("bagobjectgegevens");
    });
  });

  describe("when bagObjectType is WOONPLAATS", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "WOONPLAATS",
          identificatie: "3594",
        }),
      );
    });

    it("sets the woonplaats property", () => {
      expect(component["woonplaats"]?.identificatie).toBe("3594");
    });

    it("calls utilService.setTitle with bagobjectgegevens", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("bagobjectgegevens");
    });
  });

  describe("when bagObjectType is PAND", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "PAND",
          identificatie: "0363100012165490",
        }),
      );
    });

    it("sets the pand property and its geometry", () => {
      expect(component["pand"]?.identificatie).toBe("0363100012165490");
    });

    it("calls utilService.setTitle with bagobjectgegevens", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("bagobjectgegevens");
    });
  });

  describe("when bagObjectType is OPENBARE_RUIMTE", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "OPENBARE_RUIMTE",
          identificatie: "0363300000002244",
        }),
      );
    });

    it("sets the openbareRuimte property", () => {
      expect(component["openbareRuimte"]?.identificatie).toBe(
        "0363300000002244",
      );
    });

    it("calls utilService.setTitle with bagobjectgegevens", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("bagobjectgegevens");
    });
  });

  describe("when bagObjectType is NUMMERAANDUIDING", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "NUMMERAANDUIDING",
          identificatie: "0363200000218908",
        }),
      );
    });

    it("sets the nummeraanduiding property", () => {
      expect(component["nummeraanduiding"]?.identificatie).toBe(
        "0363200000218908",
      );
    });

    it("calls utilService.setTitle with bagobjectgegevens", () => {
      expect(utilService.setTitle).toHaveBeenCalledWith("bagobjectgegevens");
    });
  });

  describe("when bagObjectType is ADRESSEERBAAR_OBJECT", () => {
    beforeEach(async () => {
      await configureTestBed(
        makeBAGObject({
          bagObjectType: "ADRESSEERBAAR_OBJECT",
          identificatie: "0363010000721374",
        }),
      );
    });

    it("still sets bagIdentificatie", () => {
      expect(component["bagIdentificatie"]).toBe("0363010000721374");
    });
  });
});
