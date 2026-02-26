/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, fakeAsync, TestBed } from "@angular/core/testing";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { randomUUID } from "crypto";
import { of } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";
import { KlantZakenTabelComponent } from "./klant-zaken-tabel.component";

describe(KlantZakenTabelComponent.name, () => {
  let component: KlantZakenTabelComponent;
  let fixture: ComponentFixture<KlantZakenTabelComponent>;
  let zoekenService: ZoekenService;

  const mockPersoon = fromPartial<GeneratedType<"RestPersoon">>({
    bsn: "999993896",
    temporaryPersonId: randomUUID(),
    identificatieType: "BSN",
  });

  const mockCases: ZaakZoekObject[] = [
    {
      uuid: "zaak-uuid-1",
      identificatie: "ZAAK-001",
      status: "OPEN",
      betrokkenen: {
        Melder: [mockPersoon.bsn],
        Contactpersoon: [mockPersoon.bsn],
      },
    } as unknown as ZaakZoekObject,
    {
      uuid: "zaak-uuid-2",
      identificatie: "ZAAK-002",
      status: "OPEN",
      betrokkenen: {
        Melder: [mockPersoon.bsn],
      },
    } as unknown as ZaakZoekObject,
    {
      uuid: "zaak-uuid-3",
      identificatie: "ZAAK-003",
      status: "OPEN",
      betrokkenen: {
        Bewindvoerder: [mockPersoon.bsn, "999992958"],
      },
    } as unknown as ZaakZoekObject,
    {
      uuid: "zaak-uuid-4",
      identificatie: "ZAAK-004",
      status: "OPEN",
      betrokkenen: {
        Melder: [mockPersoon.bsn],
        Contactpersoon: [mockPersoon.bsn],
        Behandelaar: ["behandelaar-user"],
      },
    } as unknown as ZaakZoekObject,
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [KlantZakenTabelComponent],
      imports: [
        NoopAnimationsModule,
        MatTableModule,
        MatSortModule,
        MatPaginatorModule,
        TranslateModule.forRoot(),
        PipesModule,
        EmptyPipe,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    zoekenService = TestBed.inject(ZoekenService);
    jest.spyOn(zoekenService, "list").mockReturnValue(
      of(
        fromPartial<ZoekResultaat<ZaakZoekObject>>({
          resultaten: mockCases,
          totaal: 4,
          filters: {},
        }),
      ),
    );

    jest.spyOn(TestBed.inject(KlantenService), "listRoltypen").mockReturnValue(
      of([
        fromPartial<GeneratedType<"RestRoltype">>({ naam: "Initiator" }),
        fromPartial<GeneratedType<"RestRoltype">>({
          naam: "Belanghebbende",
        }),
        fromPartial<GeneratedType<"RestRoltype">>({ naam: "Medewerker" }),
      ]),
    );

    fixture = TestBed.createComponent(KlantZakenTabelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("klant", mockPersoon);

    fixture.detectChanges();
  });

  it(`should load ${mockCases.length} zaken`, fakeAsync(() => {
    expect(
      component["dataSource"].data.map((caseItem) => caseItem.identificatie),
    ).toEqual(mockCases.map((mockCase) => mockCase.identificatie));
  }));

  it("should return two betrokkenheden for first zaak", fakeAsync(() => {
    const betrokkenheid = component["getBetrokkenheid"](
      component["dataSource"].data[0],
    );

    expect(betrokkenheid).toEqual(["Melder", "Contactpersoon"]);
  }));

  describe("getBetrokkenheid", () => {
    it("should match betrokkene by BSN", fakeAsync(() => {
      const mockPersoonWithBsn = fromPartial<GeneratedType<"RestPersoon">>({
        bsn: "999993896",
        temporaryPersonId: randomUUID(),
        identificatieType: "BSN",
      });
      fixture.componentRef.setInput("klant", mockPersoonWithBsn);

      const mockZaak = {
        betrokkenen: {
          Melder: ["999993896"],
          Contactpersoon: ["other-bsn"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual(["Melder"]);
    }));

    it("should match betrokkene by kvkNummer for companies", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        kvkNummer: "12345678",
        rsin: "123456789",
        identificatieType: "RSIN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Belanghebbende: ["12345678"],
          Adviseur: ["87654321"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual(["Belanghebbende"]);
    }));

    it("should match betrokkene by kvkNummer when no vestigingsnummer is present", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        kvkNummer: "12345678",
        identificatieType: "RSIN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Belanghebbende: ["12345678"],
          Adviseur: ["87654321"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual(["Belanghebbende"]);
    }));

    it("should match betrokkene by vestigingsnummer for companies", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        vestigingsnummer: "000012345678",
        kvkNummer: "12345678",
        identificatieType: "VN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Belanghebbende: ["000012345678"],
          Adviseur: ["87654321"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual(["Belanghebbende"]);
    }));

    it("should prioritize vestigingsnummer over kvkNummer when both are present in same role", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        vestigingsnummer: "000012345678",
        kvkNummer: "12345678",
        identificatieType: "VN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Belanghebbende: ["000012345678", "12345678"], // both in same role
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      // Should match the role once when vestigingsnummer matches (else-if prevents kvkNummer from also matching)
      expect(result).toEqual(["Belanghebbende"]);
    }));

    it("should match both vestigingsnummer and kvkNummer when in different roles", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        vestigingsnummer: "000012345678",
        kvkNummer: "12345678",
        identificatieType: "VN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Belanghebbende: ["000012345678"], // vestigingsnummer
          Adviseur: ["12345678"], // kvkNummer
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      // Should match both because else-if is evaluated per role
      expect(result).toEqual(["Belanghebbende", "Adviseur"]);
    }));

    it("should match kvkNummer when vestigingsnummer is present but not in betrokkenen", fakeAsync(() => {
      const mockBedrijf = fromPartial<GeneratedType<"RestBedrijf">>({
        vestigingsnummer: "000012345678",
        kvkNummer: "12345678",
        identificatieType: "VN",
      });
      fixture.componentRef.setInput("klant", mockBedrijf);

      const mockZaak = {
        betrokkenen: {
          Adviseur: ["12345678"], // only kvkNummer in betrokkenen
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      // Should match kvkNummer because vestigingsnummer is not in this role's identifiers
      expect(result).toEqual(["Adviseur"]);
    }));

    it("should return multiple roles when betrokkene has multiple roles", fakeAsync(() => {
      const mockZaak = {
        betrokkenen: {
          Initiator: [mockPersoon.bsn],
          Melder: [mockPersoon.bsn],
          Contactpersoon: [mockPersoon.bsn],
          Behandelaar: ["other-id"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual(["Initiator", "Melder", "Contactpersoon"]);
    }));

    it("should return empty array when betrokkene has no roles in zaak", fakeAsync(() => {
      const mockZaak = {
        betrokkenen: {
          Behandelaar: ["other-id"],
          Adviseur: ["another-id"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual([]);
    }));

    it("should handle empty betrokkenen object", fakeAsync(() => {
      const mockZaak = {
        betrokkenen: {},
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      expect(result).toEqual([]);
    }));

    it("should not duplicate roles when same ID appears multiple times", fakeAsync(() => {
      const mockZaak = {
        betrokkenen: {
          Initiator: [mockPersoon.bsn, "other-id"],
        },
      } as unknown as ZaakZoekObject;

      const result = component["getBetrokkenheid"](mockZaak);

      // Should only return "Initiator" once, not duplicated
      expect(result).toEqual(["Initiator"]);
      expect(result.length).toBe(1);
    }));
  });

  describe("toBetrokkeneFieldName", () => {
    it.each([
      ["Behandelaar", "zaak_betrokkene_Behandelaar"],
      ["Melder", "zaak_betrokkene_Melder"],
      ["Initiator", "zaak_betrokkene_Initiator"],
    ])(
      "should convert simple role name '%s' to field name '%s'",
      (input, expected) => {
        expect(component["toBetrokkeneFieldName"](input)).toBe(expected);
      },
    );

    it.each([
      ["Belanghebbende Met Spaties", "zaak_betrokkene_Belanghebbende_Met_Spaties"],
      ["Rol Met Veel Spaties", "zaak_betrokkene_Rol_Met_Veel_Spaties"],
      ["A B C", "zaak_betrokkene_A_B_C"],
    ])(
      "should convert role name with spaces '%s' to field name '%s'",
      (input, expected) => {
        expect(component["toBetrokkeneFieldName"](input)).toBe(expected);
      },
    );

    it("should handle empty string", () => {
      expect(component["toBetrokkeneFieldName"]("")).toBe("zaak_betrokkene_");
    });

    it("should handle role name that already has underscores", () => {
      expect(component["toBetrokkeneFieldName"]("Rol_Met_Underscores")).toBe(
        "zaak_betrokkene_Rol_Met_Underscores",
      );
    });
  });

  describe("fromBetrokkeneFieldName", () => {
    it.each([
      ["Behandelaar", "Behandelaar"],
      ["Melder", "Melder"],
      ["Initiator", "Initiator"],
    ])(
      "should keep simple role name '%s' unchanged as '%s'",
      (input, expected) => {
        expect(component["fromBetrokkeneFieldName"](input)).toBe(expected);
      },
    );

    it.each([
      ["Belanghebbende_Met_Spaties", "Belanghebbende Met Spaties"],
      ["Rol_Met_Veel_Spaties", "Rol Met Veel Spaties"],
      ["A_B_C", "A B C"],
    ])(
      "should convert field name with underscores '%s' to human-readable '%s'",
      (input, expected) => {
        expect(component["fromBetrokkeneFieldName"](input)).toBe(expected);
      },
    );

    it("should handle empty string", () => {
      expect(component["fromBetrokkeneFieldName"]("")).toBe("");
    });
  });

  describe("betrokkene field name round-trip conversion", () => {
    it.each([
      "Behandelaar",
      "Melder",
      "Belanghebbende Met Spaties",
      "Rol Met Veel Spaties",
    ])(
      "should convert '%s' to field name and back to original (minus prefix)",
      (original) => {
        const fieldName = component["toBetrokkeneFieldName"](original);
        // Remove the prefix before converting back
        const withoutPrefix = fieldName.replace("zaak_betrokkene_", "");
        const result = component["fromBetrokkeneFieldName"](withoutPrefix);
        expect(result).toBe(original);
      },
    );
  });
});
