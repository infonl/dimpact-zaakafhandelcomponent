/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { MatDialogRef } from "@angular/material/dialog";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, Data, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { IdentityService } from "../../identity/identity.service";
import { TabelGegevens } from "../../shared/dynamic-table/model/tabel-gegevens";
import { ZoekenColumn } from "../../shared/dynamic-table/model/zoeken-column";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakZoekObject } from "../../zoeken/model/zaken/zaak-zoek-object";
import { ZakenWerkvoorraadComponent } from "./zaken-werkvoorraad.component";

describe(ZakenWerkvoorraadComponent.name, () => {
  let component: ZakenWerkvoorraadComponent;
  let fixture: ComponentFixture<ZakenWerkvoorraadComponent>;
  let identityService: IdentityService;
  let httpTestingController: HttpTestingController;

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

  async function setup() {
    const { fixture: renderedFixture } = await render(
      ZakenWerkvoorraadComponent,
      {
        imports: [NoopAnimationsModule, TranslateModule.forRoot()],
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
      },
    );

    fixture = renderedFixture;
    component = fixture.componentInstance;

    identityService = TestBed.inject(IdentityService);
    httpTestingController = TestBed.inject(HttpTestingController);

    testQueryClient.setQueryData(identityService.readLoggedInUser().queryKey, {
      id: "user1",
      naam: "testuser-1",
      groupIds: ["groupA", "groupB"],
    });
    fixture.detectChanges();
  }

  describe("showAssignToMe", () => {
    it.each([
      ["user2", true],
      ["user1", false],
    ])(
      "for user %s it should return %o",
      async (user: string, expectation: boolean) => {
        await setup();
        const zaakZoekObject = fromPartial<ZaakZoekObject>({
          id: "zaak1",
          rechten: { toekennen: true },
          groepId: "groupA",
          behandelaarGebruikersnaam: user,
        });

        expect(component["showAssignToMe"](zaakZoekObject)).toBe(expectation);
      },
    );

    it("returns false when zaakZoekObject.rechten.toekennen is false", async () => {
      await setup();
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-no-assign",
        rechten: { toekennen: false },
        groepId: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });

    it("returns false when loggedInUser is null", async () => {
      await setup();
      testQueryClient.setQueryData(
        identityService.readLoggedInUser().queryKey,
        null as never,
      );
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-no-user",
        rechten: { toekennen: true },
        groepId: "groupA",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });

    it("returns false when the user is not in the zaak's group", async () => {
      await setup();
      const zaakZoekObject = fromPartial<ZaakZoekObject>({
        id: "zaak-other-group",
        rechten: { toekennen: true },
        groepId: "groupC",
        behandelaarGebruikersnaam: "user2",
      });
      expect(component["showAssignToMe"](zaakZoekObject)).toBe(false);
    });
  });

  describe("assigning a zaak to yourself", () => {
    const zaakZoekObject = () =>
      fromPartial<ZaakZoekObject>({
        id: "fakeZaakUuid",
        identificatie: "ZAAK-1",
        groepId: "groupA",
        behandelaarNaam: "",
        behandelaarGebruikersnaam: "",
        rechten: { toekennen: true },
      });

    async function showZaak(zaak: ZaakZoekObject) {
      await sleep();
      httpTestingController
        .match("/rest/zoeken/list")
        .forEach((request) =>
          request.flush({ totaal: 1, resultaten: [zaak], filters: {} }),
        );
      await sleep();
      // the table creates the row views in one pass and binds their cells in the next
      fixture.detectChanges();
      fixture.detectChanges();
    }

    function assignToMeButton() {
      return screen.queryByRole("button", { name: "actie.mij.toekennen" });
    }

    async function clickAssignToMe() {
      const user = userEvent.setup();
      await user.click(
        screen.getByRole("button", { name: "actie.mij.toekennen" }),
      );
      await sleep();
    }

    it("assigns the zaak of the row it was clicked on", async () => {
      await setup();
      await showZaak(zaakZoekObject());

      await clickAssignToMe();

      const request = httpTestingController.expectOne(
        "/rest/zaken/lijst/toekennen/mij",
      );
      expect(request.request.method).toBe("PUT");
      expect(request.request.body).toEqual({
        zaakUUID: "fakeZaakUuid",
        groepId: "groupA",
      });
    });

    it("shows the assigned behandelaar on the row it was clicked on", async () => {
      await setup();
      await showZaak(zaakZoekObject());

      await clickAssignToMe();
      httpTestingController.expectOne("/rest/zaken/lijst/toekennen/mij").flush(
        fromPartial<GeneratedType<"RestZaakOverzicht">>({
          behandelaar: { id: "user1", naam: "testuser-1" },
        }),
      );
      await sleep();
      fixture.detectChanges();

      expect(screen.getByText("testuser-1")).toBeVisible();
      expect(assignToMeButton()).toBeNull();
    });

    it("leaves the row alone when the response names no behandelaar", async () => {
      await setup();
      await showZaak(zaakZoekObject());

      await clickAssignToMe();
      httpTestingController
        .expectOne("/rest/zaken/lijst/toekennen/mij")
        .flush(fromPartial<GeneratedType<"RestZaakOverzicht">>({}));
      await sleep();
      fixture.detectChanges();

      expect(screen.queryByText("testuser-1")).toBeNull();
      expect(assignToMeButton()).toBeVisible();
    });
  });

  describe("selection", () => {
    const mockZaak1 = fromPartial<ZaakZoekObject>({
      id: "zaak1",
      rechten: { toekennen: true },
      groepId: "groupA",
    });

    const mockZaak2 = fromPartial<ZaakZoekObject>({
      id: "zaak2",
      rechten: { toekennen: true },
      groepId: "groupA",
      behandelaarNaam: "Other User",
      behandelaarGebruikersnaam: "user2",
    });

    async function setupWithTwoZaken() {
      await setup();
      jest
        .spyOn(component["dataSource"], "data", "get")
        .mockReturnValue([mockZaak1, mockZaak2]);
    }

    it("should initialize with empty selection", async () => {
      await setupWithTwoZaken();
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return true when all rows are selected", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["isAllSelected"]()).toBe(true);
    });

    it("should return false when not all rows are selected", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1);
      expect(component["isAllSelected"]()).toBe(false);
    });

    it("should select all rows when masterToggle is called and not all selected", async () => {
      await setupWithTwoZaken();
      component["masterToggle"]();
      expect(component["selection"].selected.length).toBe(2);
    });

    it("should clear selection when masterToggle is called and all selected", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1, mockZaak2);
      component["masterToggle"]();
      expect(component["selection"].isEmpty()).toBe(true);
    });

    it("should return correct checkbox label for row", async () => {
      await setupWithTwoZaken();
      expect(component["checkboxLabel"](mockZaak1)).toBe("actie.selecteren");

      component["selection"].select(mockZaak1);
      expect(component["checkboxLabel"](mockZaak1)).toBe("actie.deselecteren");
    });

    it("should return correct checkbox label for header when none selected", async () => {
      await setupWithTwoZaken();
      expect(component["checkboxLabel"]()).toBe("actie.alles.selecteren");
    });

    it("should return correct checkbox label for header when all selected", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["checkboxLabel"]()).toBe("actie.alles.deselecteren");
    });

    it("should return true for isSelected when items are selected", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1);
      expect(component["isSelected"]()).toBe(true);
    });

    it("should return false for isSelected when no items are selected", async () => {
      await setupWithTwoZaken();
      expect(component["isSelected"]()).toBe(false);
    });

    it("should count selected items correctly", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["countSelected"]()).toBe(2);
    });

    it("should count only zaken with behandelaar when checkIfZaakHasHandler is true", async () => {
      await setupWithTwoZaken();
      component["selection"].select(mockZaak1, mockZaak2);
      expect(component["countSelected"](true)).toBe(1);
    });
  });

  describe("getWerklijst", () => {
    it("should return WERKVOORRAAD_ZAKEN", async () => {
      await setup();
      expect(component["getWerklijst"]()).toBe("WERKVOORRAAD_ZAKEN");
    });
  });

  describe("isAfterDate", () => {
    it("should return true for past dates", async () => {
      await setup();
      const pastDate = new Date("2020-01-01");
      expect(component["isAfterDate"](pastDate)).toBe(true);
    });

    it("should return false for future dates", async () => {
      await setup();
      const futureDate = new Date("2030-01-01");
      expect(component["isAfterDate"](futureDate)).toBe(false);
    });
  });

  describe("defaultColumns", () => {
    it("should return default columns map", async () => {
      await setup();
      const columns = component["defaultColumns"]();
      expect(columns).toBeInstanceOf(Map);
      expect(columns.size).toBeGreaterThan(0);
    });

    it("should include SELECT column when user has zakenTakenVerdelen rights", async () => {
      await setup();
      const columns = component["defaultColumns"]();
      expect(columns.has(ZoekenColumn.SELECT)).toBe(true);
    });
  });
  describe("skipped zaken in a batch verdelen or vrijgeven", () => {
    const geautoriseerdeZaak = fromPartial<ZaakZoekObject>({
      id: "zaak-geautoriseerd",
      rechten: { toekennen: true },
      groepId: "groupA",
      behandelaarGebruikersnaam: "user2",
      isZaakspecifiekGeautoriseerd: true,
    });
    const afgehandeldeZaak = fromPartial<ZaakZoekObject>({
      id: "zaak-afgehandeld",
      rechten: { toekennen: true },
      groepId: "groupA",
      behandelaarGebruikersnaam: "user2",
      afgehandeld: true,
    });
    const gewoneZaak = fromPartial<ZaakZoekObject>({
      id: "zaak-gewoon",
      rechten: { toekennen: true },
      groepId: "groupA",
      behandelaarGebruikersnaam: "user2",
    });

    let openSnackbar: jest.SpyInstance;
    let dialogData: ZaakZoekObject[] | undefined;

    async function setupWithMockedBatchProcess() {
      await setup();
      openSnackbar = jest
        .spyOn(TestBed.inject(UtilService), "openSnackbar")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["batchProcessService"], "subscribe")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["batchProcessService"], "update")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["batchProcessService"], "showProgress")
        .mockImplementation(() => undefined);
      jest
        .spyOn(component["dialog"], "open")
        .mockImplementation((_component, config) => {
          dialogData = config?.data as ZaakZoekObject[];
          return fromPartial<MatDialogRef<unknown>>({
            beforeClosed: () => of({ groep: { id: "groupA" } }),
          });
        });
    }

    it("names only the zaakspecifiek geautoriseerde reason when that is the only one", async () => {
      await setupWithMockedBatchProcess();
      component["selection"].select(geautoriseerdeZaak, gewoneZaak);

      component["openVerdelenScherm"]();

      expect(dialogData).toEqual([gewoneZaak]);
      expect(openSnackbar).toHaveBeenCalledWith(
        "msg.zaken.verdelen.overgeslagen.zaakspecifiek-geautoriseerd",
        undefined,
        8,
      );
    });

    it("names both reasons separately when zaken are skipped for both", async () => {
      await setupWithMockedBatchProcess();
      component["selection"].select(
        geautoriseerdeZaak,
        afgehandeldeZaak,
        gewoneZaak,
      );

      component["openVrijgevenScherm"]();

      expect(dialogData).toEqual([gewoneZaak]);
      expect(openSnackbar).toHaveBeenCalledWith(
        "msg.zaken.vrijgeven.overgeslagen.zaakspecifiek-geautoriseerd " +
          "msg.zaken.vrijgeven.overgeslagen.afgehandeld",
        undefined,
        8,
      );
    });

    it("shows no reason message when every selected zaak can be processed", async () => {
      await setupWithMockedBatchProcess();
      component["selection"].select(gewoneZaak);

      component["openVerdelenScherm"]();

      expect(dialogData).toEqual([gewoneZaak]);
      expect(openSnackbar).not.toHaveBeenCalled();
    });
  });
});
