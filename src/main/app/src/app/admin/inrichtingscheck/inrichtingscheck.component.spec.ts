/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient, withXhr } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
  TestRequest,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService, TranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { sleep, testQueryClient } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { UtilService } from "../../core/service/util.service";
import { PaginatorTranslator } from "../../shared/paginator/paginator-translator";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  INRICHTINGSCHECK_FILTER_DEBOUNCE_MS,
  InrichtingscheckComponent,
} from "./inrichtingscheck.component";

type ZaaktypeInrichtingscheck = GeneratedType<"RestZaaktypeInrichtingscheck">;

const ZAAKTYPES_URL = "/rest/health-check/zaaktypes";
const REFERENTIETABELLEN_URL = "/rest/health-check/referentietabellen";
const BUILD_INFORMATIE_URL = "/rest/health-check/build-informatie";

const VALIDE_ICON_LABEL = "health-check.zaaktype.status.valide";
const WAARSCHUWING_ICON_LABEL = "health-check.zaaktype.status.waarschuwing";
const NIET_VALIDE_ICON_LABEL = "health-check.zaaktype.status.niet-valide";
const BEVINDINGEN_TABLE_NAME = "health-check.zaaktype.bevindingen";
const ROLTYPE_ONTBREEKT_MESSAGE =
  "health-check.zaaktype.zaakspecifieke-autorisatie.roltype-ontbreekt";
const GEEN_RESULTAATTYPE_MESSAGE =
  "health-check.zaaktype.geen-resultaattype-aanwezig";

function createZaaktypeInrichtingscheck(
  omschrijving: string,
  overrides: Partial<ZaaktypeInrichtingscheck> = {},
) {
  return fromPartial<ZaaktypeInrichtingscheck>({
    zaaktype: {
      uuid: `fakeUuid ${omschrijving}`,
      omschrijving,
      doel: `fakeDoel ${omschrijving}`,
      beginGeldigheid: "2024-01-01",
    },
    isValide: true,
    hasWaarschuwingen: false,
    isZaakafhandelParametersValide: true,
    isStatustypeIntakeAanwezig: true,
    isStatustypeInBehandelingAanwezig: true,
    isStatustypeHeropendAanwezig: true,
    isStatustypeAanvullendeInformatieVereist: true,
    isStatustypeAfgerondAanwezig: true,
    isStatustypeAfgerondLaatsteVolgnummer: true,
    isResultaattypeAanwezig: true,
    aantalInitiatorroltypen: 1,
    aantalBehandelaarroltypen: 1,
    isRolOverigeAanwezig: true,
    isInformatieobjecttypeEmailAanwezig: true,
    resultaattypesMetVerplichtBesluit: [],
    resultaattypesMetNietOndersteundeBrondatum: [],
    isBesluittypeAanwezig: true,
    isBrpInstellingenCorrect: true,
    isZaakspecifiekeAutorisatieEigenschapAanwezig: false,
    isZaakspecifiekeAutorisatieRoltypeAanwezig: false,
    ...overrides,
  });
}

function createZaaktypesThatNeedAttention(count: number, prefix = "fake") {
  return Array.from({ length: count }, (_, index) =>
    createZaaktypeInrichtingscheck(
      `${prefix} zaaktype ${String(index + 1).padStart(2, "0")}`,
      { isValide: false, isResultaattypeAanwezig: false },
    ),
  );
}

describe(InrichtingscheckComponent.name, () => {
  let httpTestingController: HttpTestingController;
  let user: ReturnType<typeof userEvent.setup>;
  let fixture: ComponentFixture<InrichtingscheckComponent>;

  async function settle(ms = 0) {
    await sleep(ms);
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  async function renderComponent({
    isCommunicatiekanaalEformulierAanwezig = true,
  } = {}) {
    user = userEvent.setup();
    ({ fixture } = await render(InrichtingscheckComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideHttpClient(withXhr()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideRouter([]),
        {
          provide: MatPaginatorIntl,
          deps: [TranslateService],
          useFactory: (translateService: TranslateService) =>
            new PaginatorTranslator(translateService).getTranslatedPaginator(),
        },
        {
          provide: UtilService,
          useValue: fromPartial<UtilService>({ openSnackbar: jest.fn() }),
        },
      ],
    }));
    httpTestingController = TestBed.inject(HttpTestingController);
    await settle();
    httpTestingController
      .expectOne(REFERENTIETABELLEN_URL)
      .flush({ isCommunicatiekanaalEformulierAanwezig });
    httpTestingController.expectOne(BUILD_INFORMATIE_URL).flush({});
    await settle();
  }

  function expectZaaktypesRequest() {
    return httpTestingController.expectOne((request) =>
      request.url.startsWith(ZAAKTYPES_URL),
    );
  }

  function queryParametersOf(testRequest: TestRequest) {
    return Object.fromEntries(
      new URL(testRequest.request.url, "https://example.com").searchParams,
    );
  }

  async function respond(
    testRequest: TestRequest,
    resultaten: ZaaktypeInrichtingscheck[],
    totaal = resultaten.length,
  ) {
    testRequest.flush({ resultaten, totaal, foutmelding: "" });
    await settle();
    await settle();
  }

  function zaaktypeRows() {
    return screen.queryAllByRole("row", { name: /zaaktype \d+/ });
  }

  function zaaktypeRow(omschrijving: string) {
    return screen.getByRole("row", { name: new RegExp(omschrijving) });
  }

  afterEach(() => {
    httpTestingController.verify();
  });

  describe("given 40 checked zaaktypes, of which 30 need attention", () => {
    const zaaktypesThatNeedAttention = createZaaktypesThatNeedAttention(30);

    it("opens on the first page of 25 zaaktypes that need attention, sorted on omschrijving, with 30 in total", async () => {
      await renderComponent();

      const testRequest = expectZaaktypesRequest();
      expect(queryParametersOf(testRequest)).toEqual({
        page: "0",
        rows: "25",
        status: "AANDACHT_NODIG",
        sort: "omschrijving",
        order: "asc",
      });
      await respond(testRequest, zaaktypesThatNeedAttention.slice(0, 25), 30);

      expect(zaaktypeRows()).toHaveLength(25);
      expect(screen.getByText("1 - 25 msg.pagina 30")).toBeInTheDocument();
    });

    it("shows the other 5 zaaktypes on the next page", async () => {
      await renderComponent();
      await respond(
        expectZaaktypesRequest(),
        zaaktypesThatNeedAttention.slice(0, 25),
        30,
      );

      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await settle();

      const testRequest = expectZaaktypesRequest();
      expect(queryParametersOf(testRequest)).toMatchObject({ page: "1" });
      await respond(testRequest, zaaktypesThatNeedAttention.slice(25), 30);

      expect(zaaktypeRows()).toHaveLength(5);
      expect(zaaktypeRow("fake zaaktype 26")).toBeInTheDocument();
    });

    it("starts on the first page when the beheerder types a filter, and keeps the text and focus of the filter while the page loads", async () => {
      await renderComponent();
      await respond(
        expectZaaktypesRequest(),
        zaaktypesThatNeedAttention.slice(0, 25),
        30,
      );
      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await settle();
      await respond(
        expectZaaktypesRequest(),
        zaaktypesThatNeedAttention.slice(25),
        30,
      );

      const filterInput = screen.getByRole("textbox", { name: "filter" });
      await user.type(filterInput, "melding");
      await settle(INRICHTINGSCHECK_FILTER_DEBOUNCE_MS + 50);

      const testRequest = expectZaaktypesRequest();
      expect(queryParametersOf(testRequest)).toMatchObject({
        page: "0",
        omschrijving: "melding",
      });
      expect(screen.getByRole("textbox", { name: "filter" })).toHaveFocus();
      expect(screen.getByRole("textbox", { name: "filter" })).toHaveValue(
        "melding",
      );
      expect(screen.getByRole("progressbar")).toBeInTheDocument();

      const meldingZaaktypes = createZaaktypesThatNeedAttention(2, "melding");
      await respond(testRequest, meldingZaaktypes);

      expect(zaaktypeRows()).toHaveLength(2);
      expect(zaaktypeRow("melding zaaktype 01")).toBeInTheDocument();
      expect(screen.queryByRole("progressbar")).not.toBeInTheDocument();
    });
  });

  describe("the toggle and the column sort", () => {
    it("sends the toggle as status, without a status for all zaaktypes, and starts on the first page", async () => {
      await renderComponent();
      await respond(
        expectZaaktypesRequest(),
        createZaaktypesThatNeedAttention(25),
        30,
      );
      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await settle();
      await respond(
        expectZaaktypesRequest(),
        createZaaktypesThatNeedAttention(5),
        30,
      );

      const filterRow = screen
        .getAllByRole("row")
        .find((row) => within(row).queryByRole("textbox", { name: "filter" }));
      await user.click(within(filterRow!).getByRole("button"));
      await settle();

      const allZaaktypesRequest = expectZaaktypesRequest();
      expect(queryParametersOf(allZaaktypesRequest)).toEqual({
        page: "0",
        rows: "25",
        sort: "omschrijving",
        order: "asc",
      });
      await respond(allZaaktypesRequest, []);

      await user.click(within(filterRow!).getByRole("button"));
      await settle();

      const zaaktypesInOrdeRequest = expectZaaktypesRequest();
      expect(queryParametersOf(zaaktypesInOrdeRequest)).toMatchObject({
        page: "0",
        status: "IN_ORDE",
      });
      await respond(zaaktypesInOrdeRequest, []);
    });

    it("sends the column sort as sort and order and starts on the first page", async () => {
      await renderComponent();
      await respond(
        expectZaaktypesRequest(),
        createZaaktypesThatNeedAttention(25),
        30,
      );
      await user.click(
        screen.getByRole("button", { name: "actie.pagina.volgende" }),
      );
      await settle();
      await respond(
        expectZaaktypesRequest(),
        createZaaktypesThatNeedAttention(5),
        30,
      );

      await user.click(screen.getByRole("button", { name: /^doel/ }));
      await settle();

      const testRequest = expectZaaktypesRequest();
      expect(queryParametersOf(testRequest)).toMatchObject({
        page: "0",
        sort: "doel",
        order: "asc",
      });
      await respond(testRequest, []);
    });
  });

  describe("the rows of the current page", () => {
    const zaaktypeZonderResultaattype = createZaaktypeInrichtingscheck(
      "fake zaaktype 01",
      { isValide: false, isResultaattypeAanwezig: false },
    );
    const zaaktypeZonderRoltype = createZaaktypeInrichtingscheck(
      "fake zaaktype 02",
      {
        hasWaarschuwingen: true,
        isZaakspecifiekeAutorisatieEigenschapAanwezig: true,
      },
    );
    const volledigIngerichtZaaktype =
      createZaaktypeInrichtingscheck("fake zaaktype 03");

    async function renderPage() {
      await renderComponent();
      await respond(expectZaaktypesRequest(), [
        zaaktypeZonderResultaattype,
        zaaktypeZonderRoltype,
        volledigIngerichtZaaktype,
      ]);
    }

    it("expands a zaaktype without a resultaattype and shows that no resultaattype is present", async () => {
      await renderPage();
      expect(
        screen.queryByRole("table", { name: BEVINDINGEN_TABLE_NAME }),
      ).not.toBeInTheDocument();

      await user.click(zaaktypeRow("fake zaaktype 01"));
      await settle();

      const bevindingen = screen.getByRole("table", {
        name: BEVINDINGEN_TABLE_NAME,
      });
      expect(
        within(bevindingen).getByText(GEEN_RESULTAATTYPE_MESSAGE),
      ).toBeInTheDocument();
      expect(
        within(zaaktypeRow("fake zaaktype 01")).getByRole("img", {
          name: NIET_VALIDE_ICON_LABEL,
        }),
      ).toBeInTheDocument();
    });

    it("collapses an expanded zaaktype on a second click", async () => {
      await renderPage();
      await user.click(zaaktypeRow("fake zaaktype 01"));
      await settle();
      await user.click(
        screen.getAllByRole("row", { name: /fake zaaktype 01/ })[0],
      );
      await settle();

      expect(
        screen.queryByRole("table", { name: BEVINDINGEN_TABLE_NAME }),
      ).not.toBeInTheDocument();
    });

    it("shows the waarschuwing icon for a valide zaaktype without the roltype and reveals that the roltype is missing", async () => {
      await renderPage();
      const row = zaaktypeRow("fake zaaktype 02");
      expect(
        within(row).getByRole("img", { name: WAARSCHUWING_ICON_LABEL }),
      ).toBeInTheDocument();
      expect(row).toHaveClass("warning");

      await user.click(row);
      await settle();

      expect(
        within(
          screen.getByRole("table", { name: BEVINDINGEN_TABLE_NAME }),
        ).getByText(ROLTYPE_ONTBREEKT_MESSAGE),
      ).toBeInTheDocument();
    });

    it("shows the valide icon for a fully configured zaaktype and does not expand it on click", async () => {
      await renderPage();
      const row = zaaktypeRow("fake zaaktype 03");
      expect(
        within(row).getByRole("img", { name: VALIDE_ICON_LABEL }),
      ).toBeInTheDocument();
      expect(row).toHaveClass("ok");

      await user.click(row);
      await settle();

      expect(
        screen.queryByRole("table", { name: BEVINDINGEN_TABLE_NAME }),
      ).not.toBeInTheDocument();
    });
  });

  it("shows that no data was found and keeps the filter row when no zaaktype matches", async () => {
    await renderComponent();
    await respond(expectZaaktypesRequest(), []);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "filter" })).toBeInTheDocument();
  });

  it("shows the loading message and keeps the filter row while the first page loads", async () => {
    await renderComponent();

    expect(screen.getByText("msg.loading")).toBeInTheDocument();
    expect(screen.getByRole("textbox", { name: "filter" })).toBeInTheDocument();

    await respond(expectZaaktypesRequest(), []);
  });

  describe("the E-formulier communicatiekanaal", () => {
    it("shows that it is present", async () => {
      await renderComponent({ isCommunicatiekanaalEformulierAanwezig: true });
      await respond(expectZaaktypesRequest(), []);

      expect(
        screen.getByText(
          "health-check.communicatiekanaal.e-formulier.beschikbaar",
        ),
      ).toBeInTheDocument();
    });

    it("shows that it is missing", async () => {
      await renderComponent({ isCommunicatiekanaalEformulierAanwezig: false });
      await respond(expectZaaktypesRequest(), []);

      expect(
        screen.getByText(
          "health-check.communicatiekanaal.e-formulier.niet.beschikbaar",
        ),
      ).toBeInTheDocument();
    });
  });
});
