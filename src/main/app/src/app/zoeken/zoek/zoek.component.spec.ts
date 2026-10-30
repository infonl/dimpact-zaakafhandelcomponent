/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { EventEmitter } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav } from "@angular/material/sidenav";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { screen } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import {
  fromPartial,
  provideZoekenServiceWithoutRetries,
} from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { FoutDetailedDialogComponent } from "../../fout-afhandeling/dialog/fout-detailed-dialog.component";
import { PolicyService } from "../../policy/policy.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZoekResultaat } from "../model/zoek-resultaat";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
import { ZoekenService } from "../zoeken.service";
import { ZoekComponent } from "./zoek.component";

const ZOEKEN_URL = "/rest/zoeken/list";

type ZoekObject =
  GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">;

const makeZoekResultaat = (fields: Partial<ZoekResultaat<ZoekObject>> = {}) =>
  fromPartial<ZoekResultaat<ZoekObject>>({
    resultaten: [],
    totaal: 0,
    filters: {},
    ...fields,
  });

const zakenResultaat = () =>
  makeZoekResultaat({
    totaal: 25,
    filters: {
      TYPE: [
        fromPartial<GeneratedType<"FilterResultaat">>({
          naam: "ZAAK",
          aantal: 25,
        }),
      ],
    },
  });

describe(ZoekComponent.name, () => {
  const dialog = { open: jest.fn(), closeAll: jest.fn() };
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ZoekComponent, NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideZoekenServiceWithoutRetries(),
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  /** Instantiates the test module, so a suite that overrides the component calls it after that. */
  function start() {
    httpTestingController = TestBed.inject(HttpTestingController);
    testQueryClient.setQueryData(
      TestBed.inject(PolicyService).readBrpRechten().queryKey,
      fromPartial<GeneratedType<"RestBrpRechten">>({ canZoeken: false }),
    );
  }

  function sidenav(opened: boolean) {
    return fromPartial<MatSidenav>({
      opened,
      openedStart: new EventEmitter<void>(),
    });
  }

  describe("when it loads", () => {
    beforeEach(start);

    async function load(opened: boolean) {
      TestBed.inject(ZoekenService).trefwoorden.set("fakeTrefwoord");
      const fixture = TestBed.createComponent(ZoekComponent);
      fixture.componentRef.setInput("zoekenSideNav", sidenav(opened));
      fixture.detectChanges();
      await sleep();
      fixture.detectChanges();
      await sleep();
    }

    it("searches when the sidenav was opened before it loaded", async () => {
      await load(true);

      httpTestingController.expectOne(ZOEKEN_URL).flush(makeZoekResultaat());
    });

    it("does not search while the sidenav is closed", async () => {
      await load(false);

      httpTestingController.expectNone(ZOEKEN_URL);
    });

    it("renders without errors when the sidenav is open and there are no trefwoorden", async () => {
      const fixture = TestBed.createComponent(ZoekComponent);
      fixture.componentRef.setInput("zoekenSideNav", sidenav(true));

      expect(() => fixture.detectChanges()).not.toThrow();
    });
  });

  describe("searching", () => {
    const user = userEvent.setup();
    let fixture: ComponentFixture<ZoekComponent>;
    let searches: Record<string, unknown>[];

    async function settle() {
      await sleep();
      fixture.detectChanges();
      await sleep();
      fixture.detectChanges();
    }

    async function nextSearch() {
      await settle();
      const request = httpTestingController.expectOne(ZOEKEN_URL);
      searches.push(request.request.body);
      return request;
    }

    async function respondWith(
      request: TestRequest,
      zoekResultaat = makeZoekResultaat(),
    ) {
      request.flush(zoekResultaat);
      await settle();
    }

    async function fail(request: TestRequest) {
      request.flush(
        { message: "fakeMessage", exception: "fakeException" },
        { status: 400, statusText: "Bad Request" },
      );
      await settle();
    }

    function trefwoordenInput() {
      return screen.getByRole("textbox", { name: "actie.zoeken" });
    }

    function betrokkeneRoltype() {
      return screen.getByRole("combobox", {
        name: "zoeken.filter.zaak-betrokkene",
      });
    }

    async function searchFor(trefwoorden: string) {
      await user.clear(trefwoordenInput());
      await user.type(trefwoordenInput(), `${trefwoorden}{Enter}`);
      return nextSearch();
    }

    async function nextPage() {
      await user.click(screen.getByRole("button", { name: "Next page" }));
      return nextSearch();
    }

    beforeEach(async () => {
      start();
      searches = [];
      fixture = TestBed.createComponent(ZoekComponent);
      fixture.componentRef.setInput("zoekenSideNav", sidenav(false));
      fixture.detectChanges();
      await settle();
    });

    it("searches for the trefwoorden in the chosen zoekveld, on the first page", async () => {
      await respondWith(await searchFor("fakeTrefwoord"));

      expect(searches.at(-1)).toEqual(
        expect.objectContaining({
          zoeken: { [ZoekVeld.ALLE]: "fakeTrefwoord" },
          page: 0,
          rows: 10,
        }),
      );
    });

    it("says that nothing was found when a search finds nothing", async () => {
      await respondWith(await searchFor("fakeTrefwoord"));

      expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    });

    describe("a search that fails", () => {
      it("keeps the results and the page it is showing", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
        await respondWith(await nextPage(), zakenResultaat());

        await fail(await nextPage());

        expect(screen.getByText("11 – 20 of 25")).toBeVisible();
        expect(
          screen.getByRole("checkbox", { name: "type.zaak" }),
        ).toBeInTheDocument();
      });

      it("goes back to the first page when the results were cleared before it", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
        await respondWith(await nextPage(), zakenResultaat());
        await user.clear(trefwoordenInput());
        await user.type(trefwoordenInput(), "{Enter}");
        await settle();
        httpTestingController.expectNone(ZOEKEN_URL);

        await fail(await searchFor("otherTrefwoord"));

        expect(screen.queryByText(/of 25/)).not.toBeInTheDocument();
        expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
          data: expect.objectContaining({ waarschuwing: undefined }),
        });
      });

      describe("after a filter was changed", () => {
        beforeEach(async () => {
          await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
          await user.click(screen.getByRole("checkbox", { name: "type.zaak" }));
          await fail(await nextSearch());
        });

        it("searched with that filter", () => {
          expect(searches.at(-1)).toEqual(
            expect.objectContaining({
              filters: { TYPE: { values: ["ZAAK"], inverse: false } },
            }),
          );
        });

        it("puts the filters back on those of the results it is showing", () => {
          expect(
            screen.getByRole("checkbox", { name: "type.zaak" }),
          ).not.toBeChecked();
        });

        it("opens one dialog that says the previous results and filters are shown again", () => {
          expect(dialog.open).toHaveBeenCalledTimes(1);
          expect(dialog.open).toHaveBeenCalledWith(
            FoutDetailedDialogComponent,
            {
              data: expect.objectContaining({
                waarschuwing: "msg.zoeken.mislukt.vorige.zoekopdracht",
                details: "fakeException",
              }),
            },
          );
        });
      });

      it("puts the betrokkene filter back on the roltype and id of the results it is showing", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
        await user.click(betrokkeneRoltype());
        await user.click(
          screen.getByRole("option", {
            name: "betrokkene-roltype.klantcontacter",
          }),
        );
        await user.type(
          screen.getByPlaceholderText("bsn.vestigingsnummer.rsin"),
          "999993896{Enter}",
        );
        await respondWith(await nextSearch(), zakenResultaat());

        await fail(await searchFor("otherTrefwoord"));
        // mat-select renders its selected option a tick after the recreated filter renders
        await settle();

        expect(betrokkeneRoltype()).toHaveTextContent(
          "betrokkene-roltype.klantcontacter",
        );
        expect(
          screen.getByPlaceholderText("bsn.vestigingsnummer.rsin"),
        ).toHaveValue("999993896");
      });

      it("pages through a filter change that is still loading, rather than through the results it is showing", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
        await user.click(screen.getByRole("checkbox", { name: "type.zaak" }));
        await settle();

        await user.click(screen.getByRole("button", { name: "Next page" }));
        await settle();

        const requests = httpTestingController.match(ZOEKEN_URL);
        expect(requests.at(-1)?.request.body).toEqual(
          expect.objectContaining({
            filters: { TYPE: { values: ["ZAAK"], inverse: false } },
            page: 1,
          }),
        );
      });

      it("keeps the trefwoorden the user typed, though it shows the results of the previous ones", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());

        await fail(await searchFor("otherTrefwoord"));

        expect(trefwoordenInput()).toHaveValue("otherTrefwoord");
        expect(screen.getByText("1 – 10 of 25")).toBeVisible();
      });

      it("pages through the results it is showing, not through the trefwoorden that failed", async () => {
        await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
        await fail(await searchFor("otherTrefwoord"));

        const request = await nextPage();
        expect(searches.at(-1)).toEqual(
          expect.objectContaining({
            zoeken: { [ZoekVeld.ALLE]: "fakeTrefwoord" },
            page: 1,
          }),
        );
        await respondWith(request, zakenResultaat());

        expect(screen.getByText("11 – 20 of 25")).toBeVisible();
      });

      it("searches for the same trefwoorden again when the user asks for it", async () => {
        await fail(await searchFor("fakeTrefwoord"));

        await user.click(screen.getByRole("button", { name: "actie.zoeken" }));
        await respondWith(await nextSearch(), zakenResultaat());

        expect(searches).toHaveLength(2);
        expect(screen.getByText("1 – 10 of 25")).toBeVisible();
      });

      it("sends the user to the error page when the session has expired", async () => {
        const navigate = jest
          .spyOn(TestBed.inject(Router), "navigate")
          .mockResolvedValue(true);

        (await searchFor("fakeTrefwoord")).error(new ProgressEvent("error"), {
          status: 0,
        });
        await settle();

        expect(navigate).toHaveBeenCalledWith(["/fout"]);
        expect(dialog.open).not.toHaveBeenCalled();
      });
    });

    it("searches in the zoekveld the user chose", async () => {
      await respondWith(await searchFor("fakeTrefwoord"));

      await user.click(screen.getByRole("combobox", { name: "Zoeken in" }));
      await user.click(
        screen.getByRole("option", { name: "zoek-veld.zaak-omschrijving" }),
      );
      await respondWith(await nextSearch());

      expect(searches.at(-1)).toEqual(
        expect.objectContaining({
          zoeken: { [ZoekVeld.ZAAK_OMSCHRIJVING]: "fakeTrefwoord" },
        }),
      );
    });

    it("clears the trefwoorden and the results when it is reset", async () => {
      await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());

      await user.click(
        screen.getByRole("button", { name: "actie.zoekopdracht.wissen" }),
      );
      await settle();

      expect(trefwoordenInput()).toHaveValue("");
      expect(screen.queryByText(/of 25/)).not.toBeInTheDocument();
      expect(TestBed.inject(ZoekenService).hasSearched()).toBe(false);
    });

    it("keeps the page size the user chose when it is reset", async () => {
      await respondWith(await searchFor("fakeTrefwoord"), zakenResultaat());
      await user.click(
        screen.getByRole("combobox", { name: /Items per page/ }),
      );
      await user.click(screen.getByRole("option", { name: "25" }));
      await respondWith(await nextSearch(), zakenResultaat());

      await user.click(
        screen.getByRole("button", { name: "actie.zoekopdracht.wissen" }),
      );
      await settle();
      await searchFor("otherTrefwoord");

      expect(searches.at(-1)).toEqual(
        expect.objectContaining({
          zoeken: { [ZoekVeld.ALLE]: "otherTrefwoord" },
          page: 0,
          rows: 25,
        }),
      );
    });
  });

  describe("component logic", () => {
    let component: ZoekComponent;
    let fixture: ComponentFixture<ZoekComponent>;

    beforeEach(async () => {
      await TestBed.overrideComponent(ZoekComponent, {
        set: { template: "", imports: [] },
      }).compileComponents();
      start();

      fixture = TestBed.createComponent(ZoekComponent);
      component = fixture.componentInstance;
      fixture.detectChanges();
    });

    describe("hasOption", () => {
      it("returns false for empty array", () => {
        expect(component["hasOption"]([])).toBe(false);
      });

      it("returns false when only option is -NULL-", () => {
        const options = [
          fromPartial<GeneratedType<"FilterResultaat">>({ naam: "-NULL-" }),
        ];
        expect(component["hasOption"](options)).toBe(false);
      });

      it("returns true when real options are present", () => {
        const options = [
          fromPartial<GeneratedType<"FilterResultaat">>({ naam: "ZAAK" }),
        ];
        expect(component["hasOption"](options)).toBe(true);
      });
    });

    describe("betrokkeneActief", () => {
      function searchWithZoeken(zoeken: Record<string, string>) {
        component["zoeken"]({ ...component["zoekParameters"](), zoeken });
      }

      it("returns false when no betrokkene fields are set", () => {
        expect(component["betrokkeneActief"]()).toBe(false);
      });

      it("returns true when ZAAK_BETROKKENEN is set", () => {
        searchWithZoeken({ ZAAK_BETROKKENEN: "test" });
        expect(component["betrokkeneActief"]()).toBe(true);
      });

      it("returns true when ZAAK_INITIATOR is set", () => {
        searchWithZoeken({ ZAAK_INITIATOR: "test" });
        expect(component["betrokkeneActief"]()).toBe(true);
      });
    });

    describe("setZoektype", () => {
      it("enables trefwoordenControl when set to ZAC", () => {
        component["setZoektype"](ZoekType.PERSONEN);
        component["setZoektype"](ZoekType.ZAC);
        expect(component["trefwoordenControl"].enabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.ZAC);
      });

      it("disables trefwoordenControl when set to PERSONEN", () => {
        component["setZoektype"](ZoekType.PERSONEN);
        expect(component["trefwoordenControl"].disabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.PERSONEN);
      });

      it("disables trefwoordenControl when set to BEDRIJVEN", () => {
        component["setZoektype"](ZoekType.BEDRIJVEN);
        expect(component["trefwoordenControl"].disabled).toBe(true);
        expect(component["zoekType"]).toBe(ZoekType.BEDRIJVEN);
      });
    });

    describe("filterChanged", () => {
      it("sets filter value for a key", () => {
        const filterValue = fromPartial<GeneratedType<"FilterParameters">>({
          values: ["ZAAK"],
        });
        component["filterChanged"]("TYPE", filterValue);
        expect(component["zoekParameters"]().filters?.["TYPE"]).toEqual(
          filterValue,
        );
      });

      it("removes filter when value is undefined", () => {
        const filterValue = fromPartial<GeneratedType<"FilterParameters">>({
          values: ["ZAAK"],
        });
        component["filterChanged"]("TYPE", filterValue);
        component["filterChanged"]("TYPE", undefined);
        expect(component["zoekParameters"]().filters?.["TYPE"]).toBeUndefined();
      });
    });

    describe("dateFilterChange", () => {
      it("sets date range for a key", () => {
        const range = fromPartial<GeneratedType<"RestDatumRange">>({
          van: "2024-01-01",
          tot: "2024-12-31",
        });
        component["dateFilterChange"]("STARTDATUM", range);
        expect(component["zoekParameters"]().datums?.["STARTDATUM"]).toEqual(
          range,
        );
      });
    });

    describe("reset", () => {
      it("resets search state", () => {
        component["hasSearched"] = true;
        component["reset"]();
        expect(component["hasSearched"]).toBe(false);
        expect(component["hasZaken"]()).toBe(false);
        expect(component["hasTaken"]()).toBe(false);
        expect(component["hasDocument"]()).toBe(false);
      });

      it("resets trefwoordenControl value", () => {
        component["trefwoordenControl"].setValue("test");
        component["reset"]();
        expect(component["trefwoordenControl"].value).toBe("");
      });
    });
  });

  describe("personen button", () => {
    let fixture: ComponentFixture<ZoekComponent>;
    let policyService: PolicyService;

    beforeEach(async () => {
      start();
      policyService = TestBed.inject(PolicyService);

      testQueryClient.setQueryData(
        policyService.readBrpRechten().queryKey,
        fromPartial<GeneratedType<"RestBrpRechten">>({
          canZoeken: true,
        }),
      );

      fixture = TestBed.createComponent(ZoekComponent);
      fixture.detectChanges();
    });

    describe("when brpZoeken is true", () => {
      it("should show the personen button", () => {
        expect(
          screen.getByRole("button", { name: "actie.zoeken.persoon" }),
        ).toBeVisible();
      });
    });

    describe("when brpZoeken is false", () => {
      beforeEach(() => {
        testQueryClient.setQueryData(
          policyService.readBrpRechten().queryKey,
          fromPartial<GeneratedType<"RestBrpRechten">>({
            canZoeken: false,
          }),
        );
        fixture = TestBed.createComponent(ZoekComponent);
        fixture.detectChanges();
      });

      it("should hide the personen button", () => {
        expect(
          screen.queryByRole("button", { name: "actie.zoeken.persoon" }),
        ).not.toBeInTheDocument();
      });
    });
  });
});
