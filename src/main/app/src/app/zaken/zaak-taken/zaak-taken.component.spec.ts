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
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { ObjectType } from "../../core/websocket/model/object-type";
import { Opcode } from "../../core/websocket/model/opcode";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { IdentityService } from "../../identity/identity.service";
import { ExpandableTableData } from "../../shared/dynamic-table/model/expandable-table-data";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TakenService } from "../../taken/taken.service";
import { ZaakTakenComponent } from "./zaak-taken.component";

const makeZaak = (
  fields: Partial<GeneratedType<"RestZaak">> = {},
): GeneratedType<"RestZaak"> =>
  ({
    uuid: "fake-zaak-uuid",
    ...fields,
  }) as Partial<
    GeneratedType<"RestZaak">
  > as unknown as GeneratedType<"RestZaak">;

const makeTaak = (
  fields: Partial<GeneratedType<"RestTask">> = {},
): GeneratedType<"RestTask"> =>
  ({
    id: "fake-taak-id",
    naam: "Fake Taak",
    status: "TOEGEKEND" as GeneratedType<"TaakStatus">,
    creatiedatumTijd: "2026-01-01T00:00:00Z",
    fataledatum: "2026-06-01",
    groep: { id: "fake-groep-id", naam: "Fake Groep" },
    behandelaar: { id: "fake-behandelaar-id", naam: "Fake Behandelaar" },
    zaakUuid: "fake-zaak-uuid",
    rechten: {
      lezen: true,
      toekennen: true,
      wijzigen: true,
      toevoegenDocument: true,
    },
    taakdata: {},
    tabellen: {},
    taakdocumenten: [],
    taakinformatie: {},
    ...fields,
  }) as Partial<
    GeneratedType<"RestTask">
  > as unknown as GeneratedType<"RestTask">;

describe(ZaakTakenComponent.name, () => {
  let fixture: ComponentFixture<ZaakTakenComponent>;
  let component: ZaakTakenComponent;

  let takenService: TakenService;
  let websocketService: WebsocketService;
  let utilService: UtilService;
  let identityService: IdentityService;

  const fakeZaak = makeZaak({ uuid: "fake-zaak-uuid" });

  const fakeLoggedInUser = fromPartial<GeneratedType<"RestLoggedInUser">>({
    id: "logged-in-user-id",
    naam: "Logged In User",
    groupIds: ["fake-groep-id"],
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        ZaakTakenComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([]),
        provideQueryClient(testQueryClient),
      ],
    }).compileComponents();

    takenService = TestBed.inject(TakenService);
    websocketService = TestBed.inject(WebsocketService);
    utilService = TestBed.inject(UtilService);
    identityService = TestBed.inject(IdentityService);

    jest
      .spyOn(websocketService, "addListener")
      .mockReturnValue(fromPartial<WebsocketListener>({}));
    jest.spyOn(websocketService, "removeListener").mockReturnValue(undefined);
    jest.spyOn(websocketService, "suspendListener").mockReturnValue(undefined);

    testQueryClient.setQueryData(
      identityService.readLoggedInUser().queryKey,
      fakeLoggedInUser,
    );

    testQueryClient.setQueryData(
      takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
      [makeTaak()],
    );

    fixture = TestBed.createComponent(ZaakTakenComponent);
    fixture.componentRef.setInput("zaak", fakeZaak);
    fixture.detectChanges();
    component = fixture.componentInstance;
  });

  afterEach(() => {
    testQueryClient.clear();
    jest.clearAllMocks();
  });

  describe("init", () => {
    it("registers websocket listener for ZAAK_TAKEN on init", () => {
      expect(websocketService.addListener).toHaveBeenCalledWith(
        Opcode.UPDATED,
        ObjectType.ZAAK_TAKEN,
        fakeZaak.uuid,
        expect.any(Function),
      );
    });

    it("calls listTakenVoorZaakQuery with the zaak uuid", () => {
      const spy = jest.spyOn(takenService, "listTakenVoorZaakQuery");
      fixture.componentRef.setInput("zaak", makeZaak({ uuid: "new-uuid" }));
      fixture.detectChanges();
      expect(spy).toHaveBeenCalledWith("new-uuid");
    });

    it("restores toonAfgerondeTaken from session storage on init", () => {
      jest.spyOn(SessionStorageUtil, "getItem").mockReturnValue(true);

      const newFixture = TestBed.createComponent(ZaakTakenComponent);
      newFixture.componentRef.setInput("zaak", fakeZaak);
      newFixture.detectChanges();

      expect(newFixture.componentInstance["toonAfgerondeTaken"].value).toBe(
        true,
      );
    });

    it("invalidates the query when the websocket callback fires", () => {
      const invalidateSpy = jest.spyOn(
        component["queryClient"],
        "invalidateQueries",
      );
      const [[, , , callback]] = (websocketService.addListener as jest.Mock)
        .mock.calls;
      callback();
      expect(invalidateSpy).toHaveBeenCalled();
    });
  });

  describe("destroy", () => {
    it("calls websocketService.removeListener on destroy", () => {
      component.ngOnDestroy();
      expect(websocketService.removeListener).toHaveBeenCalled();
    });
  });

  describe("table rendering", () => {
    it("renders the taken table when data is available", () => {
      const table = fixture.nativeElement.querySelector("table");
      expect(table).not.toBeNull();
    });

    it("shows msg.geen.gegevens.gevonden when there is no data and not loading", () => {
      component["takenDataSource"].data = [];
      fixture.detectChanges();

      const table: HTMLElement = fixture.nativeElement.querySelector("table");
      expect(table).not.toBeNull();
      expect(table.textContent).toContain("msg.geen.gegevens.gevonden");
    });
  });

  describe("sortingDataAccessor", () => {
    it("returns groep naam for groep property", () => {
      const row = new ExpandableTableData(
        makeTaak({ groep: { id: "g1", naam: "Groep A" } }),
      );
      const result = component["takenDataSource"].sortingDataAccessor(
        row,
        "groep",
      );
      expect(result).toBe("Groep A");
    });

    it("returns behandelaar naam for behandelaar property", () => {
      const row = new ExpandableTableData(
        makeTaak({ behandelaar: { id: "b1", naam: "Behandelaar B" } }),
      );
      const result = component["takenDataSource"].sortingDataAccessor(
        row,
        "behandelaar",
      );
      expect(result).toBe("Behandelaar B");
    });

    it("returns string value for other properties", () => {
      const row = new ExpandableTableData(makeTaak({ naam: "Taak naam" }));
      const result = component["takenDataSource"].sortingDataAccessor(
        row,
        "naam",
      );
      expect(result).toBe("Taak naam");
    });
  });

  describe("expandTaken", () => {
    it("sets all rows expanded to true when expandTaken(true) is called", () => {
      const taak = makeTaak();
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [taak],
      );
      fixture.detectChanges();

      component["expandTaken"](true);
      const allExpanded = component["takenDataSource"].data.every(
        (row) => row.expanded,
      );
      expect(allExpanded).toBe(true);
    });

    it("sets all rows expanded to false when expandTaken(false) is called", () => {
      const taak = makeTaak();
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [taak],
      );
      fixture.detectChanges();

      component["expandTaken"](true);
      component["expandTaken"](false);
      const allCollapsed = component["takenDataSource"].data.every(
        (row) => !row.expanded,
      );
      expect(allCollapsed).toBe(true);
    });

    it("sets allTakenExpanded to true when all rows are expanded", () => {
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [makeTaak()],
      );
      fixture.detectChanges();

      component["expandTaken"](true);
      expect(component["allTakenExpanded"]).toBe(true);
    });

    it("sets allTakenExpanded to false when not all rows are expanded", () => {
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [makeTaak(), makeTaak({ id: "taak-2" })],
      );
      fixture.detectChanges();

      component["expandTaken"](false);
      expect(component["allTakenExpanded"]).toBe(false);
    });

    it("counts AFGEROND rows when toonAfgerondeTaken is true", () => {
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [makeTaak(), makeTaak({ id: "taak-afgerond", status: "AFGEROND" })],
      );
      fixture.detectChanges();

      component["toonAfgerondeTaken"].setValue(true);
      component["expandTaken"](true);
      expect(component["allTakenExpanded"]).toBe(true);
    });
  });

  describe("expandTaak", () => {
    it("toggles the expanded state of a single row", () => {
      const row = new ExpandableTableData(makeTaak());
      expect(row.expanded).toBeFalsy();
      component["expandTaak"](row);
      expect(row.expanded).toBe(true);
      component["expandTaak"](row);
      expect(row.expanded).toBe(false);
    });

    it("updates allTakenExpanded after toggling a row", () => {
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [makeTaak()],
      );
      fixture.detectChanges();

      const row = component["takenDataSource"].data[0];
      component["expandTaak"](row);
      expect(component["allTakenExpanded"]).toBe(true);
    });
  });

  describe("reload", () => {
    it("calls queryClient.invalidateQueries when reload() is invoked", () => {
      const invalidateSpy = jest.spyOn(
        component["queryClient"],
        "invalidateQueries",
      );
      component.reload();
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: expect.any(Array),
      });
    });
  });

  describe("taskStatusChipColor", () => {
    it("returns 'success' for AFGEROND status", () => {
      expect(component["taskStatusChipColor"]("AFGEROND")).toBe("success");
    });

    it("returns 'primary' for TOEGEKEND status", () => {
      expect(component["taskStatusChipColor"]("TOEGEKEND")).toBe("primary");
    });

    it("returns empty string for other statuses", () => {
      expect(
        component["taskStatusChipColor"](
          "NIET_TOEGEKEND" as GeneratedType<"TaakStatus">,
        ),
      ).toBe("");
    });
  });

  describe("showAssignTaakToMe", () => {
    it("returns false for AFGEROND tasks", () => {
      const taak = makeTaak({ status: "AFGEROND" });
      expect(component["showAssignTaakToMe"](taak)).toBe(false);
    });

    it("returns false when rechten.toekennen is false", () => {
      const taak = makeTaak({
        rechten: fromPartial<GeneratedType<"RestTaakRechten">>({
          toekennen: false,
        }),
      });
      expect(component["showAssignTaakToMe"](taak)).toBe(false);
    });

    it("returns false when taak has no groep", () => {
      const taak = makeTaak({ groep: undefined });
      expect(component["showAssignTaakToMe"](taak)).toBe(false);
    });

    it("returns false when logged-in user is already the behandelaar", () => {
      const taak = makeTaak({
        behandelaar: { id: "logged-in-user-id", naam: "Logged In User" },
      });
      expect(component["showAssignTaakToMe"](taak)).toBe(false);
    });

    it("returns true when logged-in user is in the groep and is not the behandelaar", () => {
      const taak = makeTaak({
        groep: { id: "fake-groep-id", naam: "Fake Groep" },
        behandelaar: { id: "other-user-id", naam: "Other User" },
      });
      expect(component["showAssignTaakToMe"](taak)).toBe(true);
    });

    it("returns false when logged-in user is not in the groep", () => {
      const taak = makeTaak({
        groep: { id: "another-groep-id", naam: "Another Groep" },
        behandelaar: { id: "other-user-id", naam: "Other User" },
      });
      expect(component["showAssignTaakToMe"](taak)).toBe(false);
    });
  });

  describe("filterTakenOpStatus / toonAfgerondeTaken", () => {
    it("filters out AFGEROND tasks when toonAfgerondeTaken is false", () => {
      const activeTaak = makeTaak({ id: "active-taak", status: "TOEGEKEND" });
      const afgerondTaak = makeTaak({
        id: "afgerond-taak",
        status: "AFGEROND",
      });
      testQueryClient.setQueryData(
        takenService.listTakenVoorZaakQuery(fakeZaak.uuid).queryKey,
        [activeTaak, afgerondTaak],
      );
      fixture.detectChanges();

      component["toonAfgerondeTaken"].setValue(false);
      component["filterTakenOpStatus"]();
      fixture.detectChanges();

      const visibleRows = component["takenDataSource"].filteredData;
      const hasAfgerond = visibleRows.some(
        (row) => row.data.status === "AFGEROND",
      );
      expect(hasAfgerond).toBe(false);
    });

    it("clears the status filter when toonAfgerondeTaken is true", () => {
      component["toonAfgerondeTaken"].setValue(true);
      component["filterTakenOpStatus"]();

      expect(component["takenDataSource"].filter).toBe("");
    });
  });

  describe("assignTaakToMe", () => {
    it("calls toekennenAanIngelogdeMedewerker and opens snackbar on success", () => {
      const returnedTaak = makeTaak({
        behandelaar: { id: "logged-in-user-id", naam: "Logged In User" },
        status: "TOEGEKEND",
      });
      jest
        .spyOn(takenService, "toekennenAanIngelogdeMedewerker")
        .mockReturnValue(of(returnedTaak));
      const snackbarSpy = jest
        .spyOn(utilService, "openSnackbar")
        .mockReturnValue(undefined as never);

      const taak = makeTaak({
        id: "fake-taak-id",
        zaakUuid: "fake-zaak-uuid",
        groep: { id: "fake-groep-id", naam: "Fake Groep" },
      });
      const mouseEvent = new MouseEvent("click");
      jest.spyOn(mouseEvent, "stopPropagation");

      component["assignTaakToMe"](taak, mouseEvent);

      expect(takenService.toekennenAanIngelogdeMedewerker).toHaveBeenCalledWith(
        {
          taakId: taak.id,
          zaakUuid: taak.zaakUuid,
          groepId: taak.groep!.id,
        },
      );
      expect(snackbarSpy).toHaveBeenCalledWith("msg.taak.toegekend", {
        behandelaar: returnedTaak.behandelaar?.naam,
      });
    });

    it("suspends the websocket listener when assigning task to me", () => {
      const returnedTaak = makeTaak();
      jest
        .spyOn(takenService, "toekennenAanIngelogdeMedewerker")
        .mockReturnValue(of(returnedTaak));
      jest
        .spyOn(utilService, "openSnackbar")
        .mockReturnValue(undefined as never);

      const taak = makeTaak();
      const mouseEvent = new MouseEvent("click");

      component["assignTaakToMe"](taak, mouseEvent);

      expect(websocketService.suspendListener).toHaveBeenCalled();
    });
  });
});
