/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LOCALE_ID, SimpleChange, SimpleChanges } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { DatumPipe } from "../../pipes/datum.pipe";
import { GeneratedType } from "../../utils/generated-types";
import { InformatieObjectIndicatiesComponent } from "./informatie-object-indicaties.component";

const indicatieMetadata: {
  indicatie: GeneratedType<"DocumentIndicatie">;
  expectedIcon: string;
  expectedPrimary: boolean;
}[] = [
  { indicatie: "VERGRENDELD", expectedIcon: "lock", expectedPrimary: true },
  {
    indicatie: "ONDERTEKEND",
    expectedIcon: "fact_check",
    expectedPrimary: false,
  },
  { indicatie: "BESLUIT", expectedIcon: "gavel", expectedPrimary: false },
  {
    indicatie: "GEBRUIKSRECHT",
    expectedIcon: "privacy_tip",
    expectedPrimary: true,
  },
  {
    indicatie: "VERZONDEN",
    expectedIcon: "local_post_office",
    expectedPrimary: false,
  },
];

const mockDocument = {
  gelockedDoor: { id: "user1", naam: "Jan de Vries" },
  ondertekening: { soort: "Digitaal", datum: "2024-01-15" },
  verzenddatum: "2024-01-20",
} as GeneratedType<"RestEnkelvoudigInformatieobject">;

const mockZoekObject = {
  vergrendeldDoor: "Piet Pietersen",
  ondertekeningSoort: "Analoog",
  ondertekeningDatum: "2024-03-10",
  verzenddatum: "2024-03-15",
};

describe(InformatieObjectIndicatiesComponent.name, () => {
  let component: InformatieObjectIndicatiesComponent;
  let translateInstant: jest.Mock;
  let datumPipe: DatumPipe;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DatumPipe, { provide: LOCALE_ID, useValue: "nl" }],
    });

    datumPipe = TestBed.inject(DatumPipe);

    translateInstant = jest.fn((key: string) => key);
    component = new InformatieObjectIndicatiesComponent({
      instant: translateInstant,
    } as unknown as TranslateService);

    jest.spyOn(console, "warn").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it("should have empty indicaties when no inputs are provided", () => {
    component.ngOnChanges({} as SimpleChanges);

    expect(component.indicaties).toHaveLength(0);
  });

  it.each(indicatieMetadata)(
    "document: $indicatie → icon '$expectedIcon', primary=$expectedPrimary",
    ({ indicatie, expectedIcon, expectedPrimary }) => {
      component.ngOnChanges({
        document: new SimpleChange(
          undefined,
          { ...mockDocument, indicaties: [indicatie] },
          true,
        ),
      });

      expect(component.indicaties).toHaveLength(1);
      expect(component.indicaties[0].naam).toBe(indicatie);
      expect(component.indicaties[0].icon).toBe(expectedIcon);
      expect(component.indicaties[0].primary).toBe(expectedPrimary);
    },
  );

  it.each(indicatieMetadata)(
    "documentZoekObject: $indicatie → icon '$expectedIcon', primary=$expectedPrimary",
    ({ indicatie, expectedIcon, expectedPrimary }) => {
      component.ngOnChanges({
        documentZoekObject: new SimpleChange(
          undefined,
          { ...mockZoekObject, indicaties: [indicatie] },
          true,
        ),
      });

      expect(component.indicaties).toHaveLength(1);
      expect(component.indicaties[0].naam).toBe(indicatie);
      expect(component.indicaties[0].icon).toBe(expectedIcon);
      expect(component.indicaties[0].primary).toBe(expectedPrimary);
    },
  );

  it("VERGRENDELD document: toelichting used gelockedDoor naam", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties: ["VERGRENDELD"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      "msg.document.vergrendeld",
    );
  });

  it("VERGRENDELD documentZoekObject: toelichting used vergrendeldDoor", () => {
    component.ngOnChanges({
      documentZoekObject: new SimpleChange(
        undefined,
        { ...mockZoekObject, indicaties: ["VERGRENDELD"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      "msg.document.vergrendeld",
    );
  });

  it("ONDERTEKEND document: toelichting bevat soort en geformatteerde datum", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties: ["ONDERTEKEND"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      `Digitaal-${datumPipe.transform("2024-01-15")}`,
    );
  });

  it("ONDERTEKEND documentZoekObject: toelichting bevat soort en geformatteerde datum", () => {
    component.ngOnChanges({
      documentZoekObject: new SimpleChange(
        undefined,
        { ...mockZoekObject, indicaties: ["ONDERTEKEND"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      `Analoog-${datumPipe.transform("2024-03-10")}`,
    );
  });

  it("BESLUIT document: toelichting via translateService", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties: ["BESLUIT"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe("msg.document.besluit");
  });

  it("GEBRUIKSRECHT document: toelichting is leeg", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties: ["GEBRUIKSRECHT"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe("");
  });

  it("VERZONDEN document: toelichting bevat geformatteerde verzenddatum", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        { ...mockDocument, indicaties: ["VERZONDEN"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      datumPipe.transform("2024-01-20"),
    );
  });

  it("VERZONDEN documentZoekObject: toelichting bevat geformatteerde verzenddatum", () => {
    component.ngOnChanges({
      documentZoekObject: new SimpleChange(
        undefined,
        { ...mockZoekObject, indicaties: ["VERZONDEN"] },
        true,
      ),
    });

    expect(component.indicaties[0].toelichting).toBe(
      datumPipe.transform("2024-03-15"),
    );
  });

  it("meerdere indicaties worden allemaal toegevoegd", () => {
    component.ngOnChanges({
      document: new SimpleChange(
        undefined,
        {
          ...mockDocument,
          indicaties: ["VERGRENDELD", "ONDERTEKEND", "BESLUIT"],
        },
        true,
      ),
    });

    expect(component.indicaties.map((i) => i.naam)).toEqual([
      "VERGRENDELD",
      "ONDERTEKEND",
      "BESLUIT",
    ]);
  });

  it("onbekende indicatie: console.warn, geen item toegevoegd", () => {
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
