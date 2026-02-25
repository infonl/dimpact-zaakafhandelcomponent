/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { CommonModule } from "@angular/common";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { SimpleChange, SimpleChanges } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { DocumentZoekObject } from "../../../zoeken/model/documenten/document-zoek-object";
import { GeneratedType } from "../../utils/generated-types";
import { IndicatiesLayout } from "../indicaties.component";
import { InformatieObjectIndicatiesComponent } from "./informatie-object-indicaties.component";

const indicatieIcons: Record<GeneratedType<"DocumentIndicatie">, string> = {
  VERGRENDELD: "lock",
  ONDERTEKEND: "fact_check",
  BESLUIT: "gavel",
  GEBRUIKSRECHT: "privacy_tip",
  VERZONDEN: "local_post_office",
};

const documentTestCases = (
  Object.keys(indicatieIcons) as GeneratedType<"DocumentIndicatie">[]
).map((indicatie) => ({
  document: {
    gelockedDoor: { id: "user1", naam: "Jan de Vries" },
    ondertekening: { soort: "Digitaal", datum: "2024-01-15" },
    verzenddatum: "2024-01-20",
    indicaties: [indicatie],
  } as GeneratedType<"RestEnkelvoudigInformatieobject">,
  indicatie,
  expectedIcon: indicatieIcons[indicatie],
  expectedLabelKey: `indicatie.${indicatie}`,
}));

const zoekObjectTestCases = (
  Object.keys(indicatieIcons) as GeneratedType<"DocumentIndicatie">[]
).map((indicatie) => ({
  zoekObject: {
    vergrendeldDoor: "Piet Pietersen",
    ondertekeningSoort: "Analoog",
    ondertekeningDatum: "2024-03-10",
    verzenddatum: "2024-03-15",
    indicaties: [indicatie],
  } as DocumentZoekObject,
  indicatie,
  expectedIcon: indicatieIcons[indicatie],
  expectedLabelKey: `indicatie.${indicatie}`,
}));

describe(InformatieObjectIndicatiesComponent.name, () => {
  let component: InformatieObjectIndicatiesComponent;

  beforeEach(() => {
    component = new InformatieObjectIndicatiesComponent({
      instant: jest.fn(),
    } as unknown as TranslateService);
  });

  it("should have empty indicaties when no inputs are provided", () => {
    component.ngOnChanges({} as SimpleChanges);

    expect(component.indicaties).toHaveLength(0);
  });

  it.each(documentTestCases)(
    "document: $indicatie → icon '$expectedIcon', label '$expectedLabelKey'",
    ({ document, expectedIcon, expectedLabelKey }) => {
      component.ngOnChanges({
        document: new SimpleChange(undefined, document, true),
      });

      expect(component.indicaties[0].icon).toBe(expectedIcon);
      expect(`indicatie.${component.indicaties[0].naam}`).toBe(
        expectedLabelKey,
      );
    },
  );

  it.each(zoekObjectTestCases)(
    "documentZoekObject: $indicatie → icon '$expectedIcon', label '$expectedLabelKey'",
    ({ zoekObject, expectedIcon, expectedLabelKey }) => {
      component.ngOnChanges({
        documentZoekObject: new SimpleChange(undefined, zoekObject, true),
      });

      expect(component.indicaties[0].icon).toBe(expectedIcon);
      expect(`indicatie.${component.indicaties[0].naam}`).toBe(
        expectedLabelKey,
      );
    },
  );

  it("unknown indicatie → console.warn, geen item toegevoegd", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        {
          indicaties: ["ONBEKEND"],
        } as unknown as GeneratedType<"RestEnkelvoudigInformatieobject">,
        true,
      ),
    });

    expect(component.indicaties).toHaveLength(0);
    expect(console.warn).toHaveBeenCalledWith(
      expect.stringContaining("ONBEKEND"),
    );
  });
});
