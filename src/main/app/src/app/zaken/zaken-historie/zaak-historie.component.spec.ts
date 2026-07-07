/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatTableHarness } from "@angular/material/table/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../zaken.service";
import { ZaakHistorieComponent } from "./zaak-historie.component";

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  ({
    uuid: "fake-zaak-uuid",
    ...fields,
  }) as Partial<
    GeneratedType<"RestZaak">
  > as unknown as GeneratedType<"RestZaak">;

const makeHistorieRegel = (
  fields: Partial<GeneratedType<"HistoryLine">> = {},
): GeneratedType<"HistoryLine"> =>
  ({
    attribuutLabel: "fakeAttribuutLabel",
    oudeWaarde: "fakeOudeWaarde",
    nieuweWaarde: "fakeNieuweWaarde",
    toelichting: "fakeToelichting",
    datumTijd: "2026-01-01T00:00:00Z",
    door: "fakeGebruiker",
    ...fields,
  }) as Partial<
    GeneratedType<"HistoryLine">
  > as unknown as GeneratedType<"HistoryLine">;

describe(ZaakHistorieComponent.name, () => {
  let fixture: ComponentFixture<ZaakHistorieComponent>;
  let component: ZaakHistorieComponent;
  let loader: HarnessLoader;
  let zakenService: ZakenService;

  const fakeZaak = makeZaak({ uuid: "fake-zaak-uuid" });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakHistorieComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    zakenService = TestBed.inject(ZakenService);

    testQueryClient.setQueryData(
      zakenService.listHistorieVoorZaakQuery(fakeZaak.uuid).queryKey,
      [makeHistorieRegel()],
    );

    fixture = TestBed.createComponent(ZaakHistorieComponent);
    fixture.componentRef.setInput("zaak", fakeZaak);
    fixture.detectChanges();
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("init", () => {
    it("calls listHistorieVoorZaakQuery with the zaak uuid", () => {
      const spy = jest.spyOn(zakenService, "listHistorieVoorZaakQuery");
      fixture.componentRef.setInput("zaak", makeZaak({ uuid: "new-uuid" }));
      fixture.detectChanges();
      expect(spy).toHaveBeenCalledWith("new-uuid");
    });
  });

  describe("table rendering", () => {
    it("renders the historie table when data is available", async () => {
      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      expect(rows.length).toBe(1);
    });

    it("renders the fetched historie data in the table", async () => {
      const table = await loader.getHarness(MatTableHarness);
      const rows = await table.getRows();
      const cellText = await rows[0].getCellTextByColumnName();
      expect(cellText["toelichting"]).toBe("fakeToelichting");
    });
  });

  describe("sortingDataAccessor", () => {
    it("returns the datumTijd as a string for the datum property", () => {
      const regel = makeHistorieRegel({ datumTijd: "2026-05-01T12:00:00Z" });
      const result = component["historie"].sortingDataAccessor(regel, "datum");
      expect(result).toBe("2026-05-01T12:00:00Z");
    });

    it("returns the door value for the gebruiker property", () => {
      const regel = makeHistorieRegel({ door: "fakeGebruikerNaam" });
      const result = component["historie"].sortingDataAccessor(
        regel,
        "gebruiker",
      );
      expect(result).toBe("fakeGebruikerNaam");
    });

    it("returns a string value for other properties", () => {
      const regel = makeHistorieRegel({ toelichting: "fakeToelichting" });
      const result = component["historie"].sortingDataAccessor(
        regel,
        "toelichting",
      );
      expect(result).toBe("fakeToelichting");
    });
  });
});
