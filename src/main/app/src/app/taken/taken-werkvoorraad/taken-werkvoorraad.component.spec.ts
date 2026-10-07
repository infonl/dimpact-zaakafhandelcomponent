/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { provideHttpClientTesting } from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Data, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { Opcode } from "../../core/websocket/model/opcode";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { IdentityService } from "../../identity/identity.service";
import { BatchProcessService } from "../../shared/batch-progress/batch-process.service";
import { TabelGegevens } from "../../shared/dynamic-table/model/tabel-gegevens";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenWerkvoorraadComponent } from "./taken-werkvoorraad.component";

describe(TakenWerkvoorraadComponent.name, () => {
  let component: TakenWerkvoorraadComponent;
  let fixture: ComponentFixture<TakenWerkvoorraadComponent>;
  let identityService: IdentityService;

  const mockTabelGegevens: TabelGegevens = {
    aantalPerPagina: 10,
    pageSizeOptions: [10, 25, 50],
    werklijstRechten: fromPartial<GeneratedType<"RestWerklijstRechten">>({
      canZakenTakenVerdelen: true,
      canZakenTakenExporteren: true,
    }),
  };

  const mockRouteData: Data = {
    tabelGegevens: mockTabelGegevens,
  };

  const mockActivatedRoute = fromPartial<ActivatedRoute>({
    data: of(mockRouteData),
  });

  beforeEach(async () => {
    TestBed.configureTestingModule({
      imports: [
        TakenWerkvoorraadComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideNativeDateAdapter(),
        provideQueryClient(testQueryClient),
      ],
    });

    fixture = TestBed.createComponent(TakenWerkvoorraadComponent);
    component = fixture.componentInstance;

    identityService = TestBed.inject(IdentityService);

    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "user1",
      naam: "testuser-1",
      groupIds: ["groupA", "groupB"],
    });
    fixture.detectChanges();
  });

  describe("showAssignToMe", () => {
    it.each([
      ["user2", true],
      ["user1", false],
    ])(
      "for user %s it should return %o",
      (user: string, expectation: boolean) => {
        const taakZoekObject = fromPartial<TaakZoekObject>({
          id: "taak1",
          rechten: { canToekennen: true },
          groepID: "groupA",
          behandelaarGebruikersnaam: user,
        });

        expect(component["showAssignToMe"](taakZoekObject)).toBe(expectation);
      },
    );

    it("returns false when taakZoekObject.rechten.toekennen is false", () => {
      const taakZoekObject = fromPartial<TaakZoekObject>({
        id: "taak-no-assign",
        rechten: { canToekennen: false },
        groepNaam: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](taakZoekObject)).toBe(false);
    });

    it.each([null, undefined])(
      "returns false when loggedInUser is %s",
      (loggedInUser: null | undefined) => {
        testQueryClient.setQueryData(
          identityService.readLoggedInUser().queryKey,
          loggedInUser as undefined,
        );
        const taakZoekObject = fromPartial<TaakZoekObject>({
          id: "taak-no-user",
          rechten: { canToekennen: true },
          groepNaam: "groupA",
          behandelaarGebruikersnaam: "user2",
        });
        expect(component["showAssignToMe"](taakZoekObject)).toBe(false);
      },
    );

    it("returns false when the user is not in the task's group", () => {
      const taakZoekObject = fromPartial<TaakZoekObject>({
        id: "taak-other-group",
        rechten: { canToekennen: true },
        groepNaam: "groupC",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](taakZoekObject)).toBe(false);
    });
  });

  describe("selection", () => {
    const mockTaak1 = fromPartial<TaakZoekObject>({
      id: "taak1",
      rechten: { canToekennen: true },
      groepNaam: "groupA",
    });

    const mockTaak2 = fromPartial<TaakZoekObject>({
      id: "taak2",
      rechten: { canToekennen: true },
      groepNaam: "groupA",
      behandelaarNaam: "Other User",
      behandelaarGebruikersnaam: "user2",
    });

    beforeEach(() => {
      jest
        .spyOn(component["dataSource"], "data", "get")
        .mockReturnValue([mockTaak1, mockTaak2]);
    });

    it("should initialize with empty selection", () => {
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return true when all rows are selected", () => {
      component["selection"].select(mockTaak1, mockTaak2);
      expect(component["isAllSelected"]()).toBe(true);
    });

    it("should return false when not all rows are selected", () => {
      component["selection"].select(mockTaak1);
      expect(component["isAllSelected"]()).toBe(false);
    });

    it("should select all rows when masterToggle is called and not all selected", () => {
      component["masterToggle"]();
      expect(component["selection"].selected.length).toBe(2);
    });

    it("should clear selection when masterToggle is called and all selected", () => {
      component["selection"].select(mockTaak1, mockTaak2);
      component["masterToggle"]();
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return correct checkbox label for row", () => {
      expect(component["checkboxLabel"](mockTaak1)).toBe("actie.selecteren");

      component["selection"].select(mockTaak1);
      expect(component["checkboxLabel"](mockTaak1)).toBe("actie.deselecteren");
    });

    it("should return correct checkbox label for header when none selected", () => {
      expect(component["checkboxLabel"]()).toBe("actie.alles.selecteren");
    });

    it("should return correct checkbox label for header when all selected", () => {
      component["selection"].select(mockTaak1, mockTaak2);
      expect(component["checkboxLabel"]()).toBe("actie.alles.deselecteren");
    });

    it("should return true for isSelected when items are selected", () => {
      component["selection"].select(mockTaak1);
      expect(component["isSelected"]()).toBe(true);
    });

    it("should return false for isSelected when no items are selected", () => {
      expect(component["isSelected"]()).toBe(false);
    });

    it("should count selected items correctly", () => {
      component["selection"].select(mockTaak1, mockTaak2);
      expect(component["countSelected"]()).toBe(2);
    });

    it("should count only tasks with behandelaar when checkIfTaskHasHandler is true", () => {
      component["selection"].select(mockTaak1, mockTaak2);
      expect(component["countSelected"](true)).toBe(1);
    });
  });

  describe("getWerklijst", () => {
    it("should return WERKVOORRAAD_TAKEN", () => {
      expect(component["getWerklijst"]()).toBe("WERKVOORRAAD_TAKEN");
    });
  });

  describe("isAfterDate", () => {
    it("should return true for past dates", () => {
      const pastDate = new Date("2020-01-01");
      expect(component["isAfterDate"](pastDate)).toBe(true);
    });

    it("should return false for future dates", () => {
      const futureDate = new Date("2030-01-01");
      expect(component["isAfterDate"](futureDate)).toBe(false);
    });
  });

  describe("defaultColumns", () => {
    it("should return default columns map", () => {
      const columns = component["defaultColumns"]();
      expect(columns).toBeInstanceOf(Map);
      expect(columns.size).toBeGreaterThan(0);
    });

    it("should include SELECT column when user has zakenTakenVerdelen rights", () => {
      const columns = component["defaultColumns"]();
      expect(columns.has(ZoekenColumn.SELECT)).toBe(true);
    });
  });

  describe("verdelen", () => {
    const taak1 = fromPartial<TaakZoekObject>({ id: "taak1" });
    const taak2 = fromPartial<TaakZoekObject>({ id: "taak2" });
    const taak3 = fromPartial<TaakZoekObject>({ id: "taak3" });

    let openSnackbar: jest.SpyInstance;
    let batchProcessOptions: Parameters<BatchProcessService["subscribe"]>[0];

    beforeEach(() => {
      openSnackbar = jest
        .spyOn(TestBed.inject(UtilService), "openSnackbar")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["batchProcessService"], "subscribe")
        .mockImplementation((options) => {
          batchProcessOptions = options;
        });
      jest
        .spyOn(component["batchProcessService"], "showProgress")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["batchProcessService"], "stop")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["dataSource"], "load")
        .mockImplementation(() => undefined);
      jest.spyOn(component["dialog"], "open").mockReturnValue(
        fromPartial<MatDialogRef<unknown>>({
          beforeClosed: () =>
            of({ groep: { id: "groupA" }, medewerker: { id: "user3" } }),
        }),
      );
    });

    function finishVerdelen(skippedTaken: TaakZoekObject[]) {
      component["selection"].select(taak1, taak2, taak3);
      component["openVerdelenScherm"]();
      skippedTaken.forEach(({ id }) =>
        batchProcessOptions.progressSubscription.onNotification?.(
          id,
          fromPartial<ScreenEvent>({ opcode: Opcode.SKIPPED }),
        ),
      );
      batchProcessOptions.finally();
    }

    it("shows the singular message when the backend skipped one taak", () => {
      finishVerdelen([taak1]);

      expect(openSnackbar).toHaveBeenCalledWith(
        "msg.taken.verdelen.overgeslagen.enkelvoud",
        { aantal: 1 },
        8,
      );
    });

    it("shows the plural message when the backend skipped more than one taak", () => {
      finishVerdelen([taak1, taak2]);

      expect(openSnackbar).toHaveBeenCalledWith(
        "msg.taken.verdelen.overgeslagen.meervoud",
        { aantal: 2 },
        8,
      );
    });

    it("shows no skipped message when the backend skipped no taak", () => {
      finishVerdelen([]);

      expect(openSnackbar).not.toHaveBeenCalled();
    });
  });
});
