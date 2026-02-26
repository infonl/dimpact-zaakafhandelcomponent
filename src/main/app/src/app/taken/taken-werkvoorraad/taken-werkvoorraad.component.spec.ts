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
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Data, RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { testQueryClient } from "../../../../setupJest";
import { IdentityService } from "../../identity/identity.service";
import { TabelGegevens } from "../../shared/dynamic-table/model/tabel-gegevens";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { MaterialModule } from "../../shared/material/material.module";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { PipesModule } from "../../shared/pipes/pipes.module";
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
      zakenTakenVerdelen: true,
      zakenTakenExporteren: true,
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
      declarations: [TakenWerkvoorraadComponent],
      imports: [
        MaterialModule,
        RouterModule,
        PipesModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
        EmptyPipe,
      ],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: mockActivatedRoute,
        },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
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

  describe(TakenWerkvoorraadComponent.prototype.showAssignToMe.name, () => {
    it.each([
      ["user2", true],
      ["user1", false],
    ])("for user %s it should return %o", (user, expectation) => {
      const taakZoekObject = fromPartial<TaakZoekObject>({
        id: "taak1",
        rechten: { toekennen: true },
        groepID: "groupA",
        behandelaarGebruikersnaam: user,
      });

      expect(component.showAssignToMe(taakZoekObject)).toBe(expectation);
    });

    it("returns false when taakZoekObject.rechten.toekennen is false", () => {
      const taakZoekObject = fromPartial<TaakZoekObject>({
        id: "taak-no-assign",
        rechten: { toekennen: false },
        groepNaam: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component.showAssignToMe(taakZoekObject)).toBe(false);
    });
    it.each([null, undefined])(
      "returns false when loggedInUser is %s",
      (loggedInUser) => {
        testQueryClient.setQueryData(
          identityService.readLoggedInUser().queryKey,
          loggedInUser as undefined,
        );
        const taakZoekObject = fromPartial<TaakZoekObject>({
          id: "taak-no-user",
          rechten: { toekennen: true },
          groepNaam: "groupA",
          behandelaarGebruikersnaam: "user2",
        });
        expect(component.showAssignToMe(taakZoekObject)).toBe(false);
      },
    );
    it("returns false when the user is not in the task's group", () => {
      const taakZoekObject = fromPartial<TaakZoekObject>({
        id: "taak-other-group",
        rechten: { toekennen: true },
        groepNaam: "groupC",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component.showAssignToMe(taakZoekObject)).toBe(false);
    });
  });

  describe("selection", () => {
    const mockTaak1 = fromPartial<TaakZoekObject>({
      id: "taak1",
      rechten: { toekennen: true },
      groepNaam: "groupA",
    });

    const mockTaak2 = fromPartial<TaakZoekObject>({
      id: "taak2",
      rechten: { toekennen: true },
      groepNaam: "groupA",
      behandelaarNaam: "Other User",
      behandelaarGebruikersnaam: "user2",
    });

    beforeEach(() => {
      jest
        .spyOn(component.dataSource, "data", "get")
        .mockReturnValue([mockTaak1, mockTaak2]);
    });

    it("should initialize with empty selection", () => {
      expect(component.selection.isEmpty()).toBe(true);
    });

    it("should return true when all rows are selected", () => {
      component.selection.select(mockTaak1, mockTaak2);
      expect(component.isAllSelected()).toBe(true);
    });

    it("should return false when not all rows are selected", () => {
      component.selection.select(mockTaak1);
      expect(component.isAllSelected()).toBe(false);
    });

    it("should select all rows when masterToggle is called and not all selected", () => {
      component.masterToggle();
      expect(component.selection.selected.length).toBe(2);
    });

    it("should clear selection when masterToggle is called and all selected", () => {
      component.selection.select(mockTaak1, mockTaak2);
      component.masterToggle();
      expect(component.selection.isEmpty()).toBe(true);
    });

    it("should return correct checkbox label for row", () => {
      expect(component.checkboxLabel(mockTaak1)).toBe("actie.selecteren");

      component.selection.select(mockTaak1);
      expect(component.checkboxLabel(mockTaak1)).toBe("actie.deselecteren");
    });

    it("should return correct checkbox label for header when none selected", () => {
      expect(component.checkboxLabel()).toBe("actie.alles.selecteren");
    });

    it("should return correct checkbox label for header when all selected", () => {
      component.selection.select(mockTaak1, mockTaak2);
      expect(component.checkboxLabel()).toBe("actie.alles.deselecteren");
    });

    it("should return true for isSelected when items are selected", () => {
      component.selection.select(mockTaak1);
      expect(component.isSelected()).toBe(true);
    });

    it("should return false for isSelected when no items are selected", () => {
      expect(component.isSelected()).toBe(false);
    });

    it("should count selected items correctly", () => {
      component.selection.select(mockTaak1, mockTaak2);
      expect(component.countSelected()).toBe(2);
    });

    it("should count only tasks with behandelaar when checkIfTaskHasHandler is true", () => {
      component.selection.select(mockTaak1, mockTaak2);
      expect(component.countSelected(true)).toBe(1);
    });
  });

  describe("getWerklijst", () => {
    it("should return WERKVOORRAAD_TAKEN", () => {
      expect(component.getWerklijst()).toBe("WERKVOORRAAD_TAKEN");
    });
  });

  describe("isAfterDate", () => {
    it("should return true for past dates", () => {
      const pastDate = new Date("2020-01-01");
      expect(component.isAfterDate(pastDate)).toBe(true);
    });

    it("should return false for future dates", () => {
      const futureDate = new Date("2030-01-01");
      expect(component.isAfterDate(futureDate)).toBe(false);
    });
  });

  describe("defaultColumns", () => {
    it("should return default columns map", () => {
      const columns = component.defaultColumns();
      expect(columns).toBeInstanceOf(Map);
      expect(columns.size).toBeGreaterThan(0);
    });

    it("should include SELECT column when user has zakenTakenVerdelen rights", () => {
      const columns = component.defaultColumns();
      expect(columns.has(ZoekenColumn.SELECT)).toBe(true);
    });
  });
});
