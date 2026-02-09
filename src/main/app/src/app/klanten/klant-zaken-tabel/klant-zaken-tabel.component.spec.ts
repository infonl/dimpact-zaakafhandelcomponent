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
});
