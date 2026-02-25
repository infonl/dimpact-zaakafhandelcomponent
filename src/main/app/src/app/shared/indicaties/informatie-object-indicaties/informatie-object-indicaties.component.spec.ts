/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { SimpleChange, SimpleChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { DocumentZoekObject } from "../../../zoeken/model/documenten/document-zoek-object";
import { GeneratedType } from "../../utils/generated-types";
import { InformatieObjectIndicatiesComponent } from "./informatie-object-indicaties.component";

describe(InformatieObjectIndicatiesComponent.name, () => {
  let component: InformatieObjectIndicatiesComponent;
  let translateService: TranslateService;

  const mockDocument = {
    indicaties: [] as GeneratedType<"DocumentIndicatie">[],
    gelockedDoor: { id: "user1", naam: "Jan de Vries" },
    ondertekening: { soort: "Digitaal", datum: "2024-01-15" },
    verzenddatum: "2024-01-20",
  } as GeneratedType<"RestEnkelvoudigInformatieobject">;

  const mockZoekObject = {
    indicaties: [] as GeneratedType<"DocumentIndicatie">[],
    vergrendeldDoor: "Piet Pietersen",
    ondertekeningSoort: "Analoog",
    ondertekeningDatum: "2024-03-10",
    verzenddatum: "2024-03-15",
  } as DocumentZoekObject;

  beforeEach(() => {
    translateService = { instant: jest.fn() } as unknown as TranslateService;
    component = new InformatieObjectIndicatiesComponent(translateService);
  });

  function withDocument(
    indicaties: GeneratedType<"DocumentIndicatie">[],
    overrides: Partial<GeneratedType<"RestEnkelvoudigInformatieobject">> = {},
  ): SimpleChanges {
    return {
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties, ...overrides },
        true,
      ),
    };
  }

  function withZoekObject(
    indicaties: GeneratedType<"DocumentIndicatie">[],
    overrides: Partial<DocumentZoekObject> = {},
  ): SimpleChanges {
    return {
      documentZoekObject: new SimpleChange(
        undefined,
        { ...mockZoekObject, indicaties, ...overrides },
        true,
      ),
    };
  }

  it("should have empty indicaties when no inputs are provided", () => {
    component.ngOnChanges({} as SimpleChanges);

    expect(component.indicaties).toEqual([]);
  });

  describe("with document input", () => {
    it("VERGRENDELD → lock icon, primary=true, toelichting via translateService met gelockedDoor.naam", () => {
      component.ngOnChanges(withDocument(["VERGRENDELD"]));

      expect(component.indicaties).toHaveLength(1);
      expect(component.indicaties[0].icon).toBe("lock");
      expect(component.indicaties[0].primary).toBe(true);
      expect(translateService.instant).toHaveBeenCalledWith(
        "msg.document.vergrendeld",
        { gebruiker: "Jan de Vries" },
      );
    });

    it("VERGRENDELD zonder gelockedDoor → gebruiker is undefined", () => {
      component.ngOnChanges(
        withDocument(["VERGRENDELD"], { gelockedDoor: undefined }),
      );

      expect(translateService.instant).toHaveBeenCalledWith(
        "msg.document.vergrendeld",
        { gebruiker: undefined },
      );
    });

    it("ONDERTEKEND → fact_check icon, primary=false, toelichting bevat soort en datum-separator", () => {
      component.ngOnChanges(withDocument(["ONDERTEKEND"]));

      const item = component.indicaties[0];
      expect(item.icon).toBe("fact_check");
      expect(item.primary).toBe(false);
      expect(item.toelichting).toContain("Digitaal");
      expect(item.toelichting).toContain("-");
    });

    it("ONDERTEKEND zonder ondertekening → toelichting bevat alleen datum-separator", () => {
      component.ngOnChanges(
        withDocument(["ONDERTEKEND"], { ondertekening: undefined }),
      );

      expect(component.indicaties[0].toelichting).toBe("undefined-undefined");
    });

    it("BESLUIT → gavel icon, toelichting via translateService", () => {
      component.ngOnChanges(withDocument(["BESLUIT"]));

      expect(component.indicaties[0].icon).toBe("gavel");
      expect(translateService.instant).toHaveBeenCalledWith(
        "msg.document.besluit",
      );
    });

    it("GEBRUIKSRECHT → privacy_tip icon, primary=true, lege toelichting", () => {
      component.ngOnChanges(withDocument(["GEBRUIKSRECHT"]));

      const item = component.indicaties[0];
      expect(item.icon).toBe("privacy_tip");
      expect(item.primary).toBe(true);
      expect(item.toelichting).toBe("");
    });

    it("VERZONDEN → local_post_office icon, toelichting bevat geformatteerde verzenddatum", () => {
      component.ngOnChanges(withDocument(["VERZONDEN"]));

      const item = component.indicaties[0];
      expect(item.icon).toBe("local_post_office");
      expect(item.toelichting).toBeTruthy();
    });

    it("onbekende indicatie → console.warn, geen item toegevoegd", () => {
      const warnSpy = jest
        .spyOn(console, "warn")
        .mockImplementation(() => {});

      component.ngOnChanges(
        withDocument(["ONBEKEND" as GeneratedType<"DocumentIndicatie">]),
      );

      expect(component.indicaties).toHaveLength(0);
      expect(warnSpy).toHaveBeenCalledWith(
        expect.stringContaining("ONBEKEND"),
      );
    });

    it("meerdere indicaties → alle items worden toegevoegd", () => {
      component.ngOnChanges(
        withDocument(["VERGRENDELD", "BESLUIT", "GEBRUIKSRECHT"]),
      );

      expect(component.indicaties).toHaveLength(3);
    });
  });

  describe("with documentZoekObject input", () => {
    it("VERGRENDELD → lock icon, primary=true, toelichting gebruikt vergrendeldDoor", () => {
      component.ngOnChanges(withZoekObject(["VERGRENDELD"]));

      expect(component.indicaties[0].icon).toBe("lock");
      expect(component.indicaties[0].primary).toBe(true);
      expect(translateService.instant).toHaveBeenCalledWith(
        "msg.document.vergrendeld",
        { gebruiker: "Piet Pietersen" },
      );
    });

    it("ONDERTEKEND → toelichting bevat ondertekeningSoort en datum-separator", () => {
      component.ngOnChanges(withZoekObject(["ONDERTEKEND"]));

      const item = component.indicaties[0];
      expect(item.toelichting).toContain("Analoog");
      expect(item.toelichting).toContain("-");
    });

    it("VERZONDEN → local_post_office icon, toelichting bevat geformatteerde verzenddatum", () => {
      component.ngOnChanges(withZoekObject(["VERZONDEN"]));

      const item = component.indicaties[0];
      expect(item.icon).toBe("local_post_office");
      expect(item.toelichting).toBeTruthy();
    });
  });

  describe("priority: documentZoekObject heeft voorrang op document", () => {
    it("laadt indicaties uit documentZoekObject als beide inputs tegelijk veranderen", () => {
      component.ngOnChanges({
        document: new SimpleChange(
          undefined,
          { ...mockDocument, indicaties: ["BESLUIT"] },
          true,
        ),
        documentZoekObject: new SimpleChange(
          undefined,
          { ...mockZoekObject, indicaties: ["GEBRUIKSRECHT"] },
          true,
        ),
      });

      expect(component.indicaties).toHaveLength(1);
      expect(component.indicaties[0].icon).toBe("privacy_tip"); // GEBRUIKSRECHT, niet BESLUIT
    });
  });
});
