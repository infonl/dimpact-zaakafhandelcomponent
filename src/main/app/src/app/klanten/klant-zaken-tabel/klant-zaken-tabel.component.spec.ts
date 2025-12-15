/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ComponentFixture,
  fakeAsync,
  flush,
  tick,
  TestBed,
} from "@angular/core/testing";
import { provideHttpClient } from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { MatPaginatorModule } from "@angular/material/paginator";
import { MatSortModule } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { TranslateModule } from "@ngx-translate/core";
import { of } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZoekResultaat } from "../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../zoeken/zoeken.service";
import { KlantenService } from "../klanten.service";
import { KlantZakenTabelComponent } from "./klant-zaken-tabel.component";
import { PipesModule } from "../../shared/pipes/pipes.module";

describe("KlantZakenTabelComponent", () => {
  let component: KlantZakenTabelComponent;
  let fixture: ComponentFixture<KlantZakenTabelComponent>;
  let zoekenService: ZoekenService;
  let klantenService: KlantenService;
  let utilService: UtilService;

  const mockPersoon: GeneratedType<"RestPersoon"> = {
    bsnNummer: "999990408",
    identificatieType: "BSN",
    indicaties: [],
  } as unknown as GeneratedType<"RestPersoon">;

  const mockZaak1 = {
    uuid: "zaak-uuid-1",
    identificatie: "ZAAK-001",
    status: "OPEN",
    betrokkenen: {
      Melder: ["999990408"],
      Contactpersoon: ["999990408"],
    },
  } as unknown as ZaakZoekObject;

  const mockZaak2 = {
    uuid: "zaak-uuid-2",
    identificatie: "ZAAK-002",
    status: "OPEN",
    betrokkenen: {
      Melder: ["999990408"],
    },
  } as unknown as ZaakZoekObject;

  const mockZaak3 = {
    uuid: "zaak-uuid-3",
    identificatie: "ZAAK-003",
    status: "OPEN",
    betrokkenen: {
      Bewindvoerder: ["999990408", "999992958"],
    },
  } as unknown as ZaakZoekObject;

  const mockZaak4 = {
    uuid: "zaak-uuid-4",
    identificatie: "ZAAK-004",
    status: "OPEN",
    betrokkenen: {
      Melder: ["999990408"],
      Contactpersoon: ["999990408"],
      Behandelaar: ["behandelaar-user"],
    },
  } as unknown as ZaakZoekObject;

  const mockRoltypen = [
    { naam: "Initiator" } as GeneratedType<"RestRoltype">,
    { naam: "Belanghebbende" } as GeneratedType<"RestRoltype">,
    { naam: "Medewerker" } as GeneratedType<"RestRoltype">,
  ];

  beforeEach(async () => {
    const mockZoekResultaat = {
      resultaten: [mockZaak1, mockZaak2, mockZaak3, mockZaak4],
      totaal: 4,
      aantal: 4,
      filters: {},
      foutmelding: undefined,
    } as unknown as ZoekResultaat<ZaakZoekObject>;

    const testQueryClient = new QueryClient();

    await TestBed.configureTestingModule({
      declarations: [KlantZakenTabelComponent],
      imports: [
        NoopAnimationsModule,
        MatTableModule,
        MatSortModule,
        MatPaginatorModule,
        TranslateModule.forRoot(),
        PipesModule,
      ],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideTanStackQuery(testQueryClient),
      ],
    }).compileComponents();

    zoekenService = TestBed.inject(ZoekenService);
    jest.spyOn(zoekenService, "list").mockReturnValue(of(mockZoekResultaat));

    klantenService = TestBed.inject(KlantenService);
    jest
      .spyOn(klantenService, "listRoltypen")
      .mockReturnValue(of(mockRoltypen));

    utilService = TestBed.inject(UtilService);
    jest.spyOn(utilService, "setLoading").mockReturnValue();

    fixture = TestBed.createComponent(KlantZakenTabelComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput("klant", mockPersoon);
  });

  it("should call zoekenService.list on ngAfterViewInit", () => {
    fixture.detectChanges();
    expect(zoekenService.list).toHaveBeenCalled();
  });

  it("should render 4 cases in the table", fakeAsync(() => {
    // Table renders in multiple cycles due to async data fetching and change detection
    fixture.detectChanges();
    flush();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component["dataSource"].data.length).toBe(4);
    expect(component["dataSource"].data[0].identificatie).toBe(
      mockZaak1.identificatie,
    );
    expect(component["dataSource"].data[3].identificatie).toBe(
      mockZaak4.identificatie,
    );
  }));

  it("should return 2 betrokkenheden for case with multiple matching roles", fakeAsync(() => {
    // Table renders in multiple cycles due to async data fetching and change detection
    fixture.detectChanges();
    flush();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const firstZaak = component["dataSource"].data[0];
    const betrokkenheid = component["getBetrokkenheid"](firstZaak);

    expect(betrokkenheid.length).toBe(2);
    expect(betrokkenheid).toContain("Melder");
    expect(betrokkenheid).toContain("Contactpersoon");
  }));
});
