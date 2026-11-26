/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { signal } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatNativeDateModule } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { focusManager } from "@tanstack/angular-query-experimental";
import { screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ScreenEvent } from "../../core/websocket/model/screen-event";
import { WebsocketListener } from "../../core/websocket/model/websocket-listener";
import { WebsocketService } from "../../core/websocket/websocket.service";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ParametersComponent } from "./parameters.component";
import {
  ZaakafhandelParametersListParameters,
  toZaaktypeConfigurationListQuery,
} from "./zaakafhandel-parameters-list-parameters";

const fakeZaaktypeConfigurationOverzicht = (
  omschrijving = "Fake zaaktype",
): GeneratedType<"RestZaaktypeConfigurationOverzicht"> =>
  fromPartial<GeneratedType<"RestZaaktypeConfigurationOverzicht">>({
    zaaktype: {
      uuid: `uuid-${omschrijving.replaceAll(" ", "-")}`,
      identificatie: `identificatie-${omschrijving}`,
      omschrijving,
      isNuGeldig: true,
    },
    processDefinition: { key: "fakeKey", naam: "Fake procesmodel" },
    isValide: true,
  });

const fakePage = (
  page: Partial<GeneratedType<"RestZaaktypeConfigurationOverzichtResultaat">>,
) =>
  fromPartial<GeneratedType<"RestZaaktypeConfigurationOverzichtResultaat">>({
    resultaten: [],
    totaal: page.resultaten?.length ?? 0,
    filterZaaktypes: [],
    filterProcessDefinitions: [],
    ...page,
  });

type WebsocketCallback = Parameters<WebsocketService["addListener"]>[3];

function mockWebsocketListener() {
  const websocketService = TestBed.inject(WebsocketService);
  const listener = fromPartial<WebsocketListener>({});
  let zaaktypeConfiguratiesChanged: WebsocketCallback | undefined;
  const addListener = jest
    .spyOn(websocketService, "addListener")
    .mockImplementation((_opcode, _objectType, _objectId, callback) => {
      zaaktypeConfiguratiesChanged = callback;
      return listener;
    });
  const removeListener = jest
    .spyOn(websocketService, "removeListener")
    .mockImplementation();
  return {
    listener,
    addListener,
    removeListener,
    sendZaaktypeConfiguratiesEvent: () =>
      zaaktypeConfiguratiesChanged?.(
        new ScreenEvent("UPDATED", "ZAAKTYPE_CONFIGURATIES", {
          resource: "zaaktype-configuraties",
          detail: null,
        }),
      ),
  };
}

const configureTestingModule = () =>
  TestBed.configureTestingModule({
    imports: [ParametersComponent, NoopAnimationsModule, MatNativeDateModule],
    providers: [
      provideTranslateService(),
      {
        provide: UtilService,
        useValue: fromPartial<UtilService>({ loading: signal(false) }),
      },
      { provide: ConfiguratieService, useValue: {} },
      provideRouter([]),
    ],
  }).compileComponents();

describe(ParametersComponent.name, () => {
  let fixture: ComponentFixture<ParametersComponent>;
  let httpTestingController: HttpTestingController;
  let websocketMock: ReturnType<typeof mockWebsocketListener>;

  async function render() {
    httpTestingController = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ParametersComponent);
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  async function expectPageRequest() {
    await sleep();
    const request = httpTestingController.expectOne(
      ({ url }) => url.split("?")[0] === "/rest/zaakafhandelparameters",
    );
    expect(request.request.method).toBe("GET");
    return request;
  }

  function requestedQuery(request: TestRequest) {
    return Object.fromEntries(
      new URL(request.request.urlWithParams, "https://example.com")
        .searchParams,
    );
  }

  async function answer(
    request: TestRequest,
    page: Parameters<typeof fakePage>[0] = {},
  ) {
    request.flush(fakePage(page));
    await sleep();
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();
  }

  function restoreStoredFilters(
    changes: Partial<ZaakafhandelParametersListParameters>,
  ) {
    jest.spyOn(SessionStorageUtil, "getItem").mockReturnValue({
      ...new ZaakafhandelParametersListParameters("geldig", "desc"),
      ...changes,
    });
  }

  beforeEach(async () => {
    await configureTestingModule();
    sessionStorage.clear();
    websocketMock = mockWebsocketListener();
  });

  afterEach(() => {
    // destroys the page while removeListener is still mocked
    TestBed.resetTestingModule();
    jest.restoreAllMocks();
    focusManager.setFocused(undefined);
  });

  it("shows the loading message while the first page is being read", async () => {
    await render();

    expect(screen.getByText("msg.loading")).toBeVisible();
  });

  it("shows the no-data message when no zaaktype matches the filters", async () => {
    await render();

    await answer(await expectPageRequest());

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
    expect(screen.queryByText("msg.loading")).not.toBeInTheDocument();
  });

  it("reads the first page of 25 valid zaaktypes that are valid today, sorted on geldig descending", async () => {
    await render();

    const request = await expectPageRequest();

    expect(requestedQuery(request)).toEqual(
      expect.objectContaining({
        page: "0",
        rows: "25",
        sortField: "GELDIG",
        sortOrder: "DESC",
        isValide: "true",
        isNuGeldig: "true",
      }),
    );
  });

  it("links each row of the page to the edit page of the configuration of its zaaktype", async () => {
    await render();

    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
    });

    const row = screen.getByRole("row", { name: /Fake zaaktype/ });
    expect(row).toHaveTextContent("Fake procesmodel");
    expect(
      within(row).getByRole("link", { name: "actie.bewerken" }),
    ).toHaveAttribute("href", "/admin/parameters/uuid-Fake-zaaktype");
  });

  it("reads the configurations again when the beheerder returns to the list, so that a configuration that ZAC created meanwhile shows its procesmodel", async () => {
    const zaaktypeConfigurationWithoutProcessDefinition = {
      ...fakeZaaktypeConfigurationOverzicht("Nieuwe versie"),
      processDefinition: undefined,
    };
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [zaaktypeConfigurationWithoutProcessDefinition],
    });
    fixture.destroy();

    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht("Nieuwe versie")],
    });

    expect(screen.getByText("Fake procesmodel")).toBeVisible();
  });

  it("listens to the ZAAKTYPE_CONFIGURATIES screen event while the page is open", async () => {
    await render();

    expect(websocketMock.addListener).toHaveBeenCalledWith(
      "UPDATED",
      "ZAAKTYPE_CONFIGURATIES",
      "zaaktype-configuraties",
      expect.any(Function),
    );
  });

  it("reads the configurations again when ZAC signals ZAAKTYPE_CONFIGURATIES, keeping the shown rows meanwhile, so that a configuration that ZAC created meanwhile shows its procesmodel", async () => {
    const zaaktypeConfigurationWithoutProcessDefinition = {
      ...fakeZaaktypeConfigurationOverzicht("Nieuwe versie"),
      processDefinition: undefined,
    };
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [zaaktypeConfigurationWithoutProcessDefinition],
    });

    websocketMock.sendZaaktypeConfiguratiesEvent();
    const refreshRequest = await expectPageRequest();
    fixture.detectChanges();

    expect(
      screen.getByRole("row", { name: /Nieuwe versie/ }),
    ).not.toHaveTextContent("Fake procesmodel");

    await answer(refreshRequest, {
      resultaten: [fakeZaaktypeConfigurationOverzicht("Nieuwe versie")],
    });

    expect(
      screen.getByRole("row", { name: /Nieuwe versie/ }),
    ).toHaveTextContent("Fake procesmodel");
  });

  it("does not read the configurations again when the beheerder comes back to the browser tab", async () => {
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
    });

    focusManager.setFocused(false);
    focusManager.setFocused(true);
    await sleep();

    httpTestingController.expectNone(
      ({ url }) => url.split("?")[0] === "/rest/zaakafhandelparameters",
    );
  });

  it("stops listening to the ZAAKTYPE_CONFIGURATIES screen event when the page is closed", async () => {
    await render();

    fixture.destroy();

    expect(websocketMock.removeListener).toHaveBeenCalledWith(
      websocketMock.listener,
    );
  });

  it("reads the next page when the beheerder goes to the next page", async () => {
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Next page" }));
    fixture.detectChanges();

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({ page: "1", rows: "25" }),
    );
  });

  it("keeps showing the rows of the current page while the next page is being read", async () => {
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Next page" }));
    fixture.detectChanges();
    const nextPageRequest = await expectPageRequest();
    fixture.detectChanges();

    expect(
      screen.getByRole("row", { name: /Fake zaaktype/ }),
    ).toBeInTheDocument();

    await answer(nextPageRequest, {
      resultaten: [fakeZaaktypeConfigurationOverzicht("Volgend zaaktype")],
      totaal: 30,
    });

    expect(
      screen.getByRole("row", { name: /Volgend zaaktype/ }),
    ).toBeInTheDocument();
  });

  it("shows the cached rows of a page while it reads that page again because the cache is stale", async () => {
    const sixMinutesAgo = Date.now() - 6 * 60 * 1000;
    testQueryClient.setQueryData(
      TestBed.inject(ZaakafhandelParametersService).listZaakafhandelParameters(
        toZaaktypeConfigurationListQuery({
          ...new ZaakafhandelParametersListParameters("geldig", "desc"),
          page: 1,
        }),
      ).queryKey,
      fakePage({
        resultaten: [fakeZaaktypeConfigurationOverzicht("Bewaard zaaktype")],
        totaal: 30,
      }),
      { updatedAt: sixMinutesAgo },
    );
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "Next page" }));
    fixture.detectChanges();
    const refreshRequest = await expectPageRequest();
    fixture.detectChanges();

    expect(
      screen.getByRole("row", { name: /Bewaard zaaktype/ }),
    ).toBeInTheDocument();

    await answer(refreshRequest, {
      resultaten: [fakeZaaktypeConfigurationOverzicht("Nieuw zaaktype")],
      totaal: 30,
    });

    expect(
      screen.getByRole("row", { name: /Nieuw zaaktype/ }),
    ).toBeInTheDocument();
  });

  it("reads the first page again, sorted on omschrijving, when the beheerder sorts on omschrijving", async () => {
    restoreStoredFilters({ page: 1 });
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
    });

    const user = userEvent.setup();
    await user.click(screen.getByText("zaaktype-omschrijving"));
    fixture.detectChanges();

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({
        page: "0",
        sortField: "OMSCHRIJVING",
        sortOrder: "ASC",
      }),
    );
  });

  it("reads the last page that has rows, and remembers it, when the stored page lies past the last page", async () => {
    restoreStoredFilters({ page: 1 });
    const setItem = jest.spyOn(SessionStorageUtil, "setItem");
    await render();

    await answer(await expectPageRequest(), { resultaten: [], totaal: 25 });

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({ page: "0" }),
    );
    expect(setItem).toHaveBeenCalledWith(
      "zaakafhandelParametersListParameters",
      expect.objectContaining({ page: 0 }),
    );
  });

  it.each([
    { geldigheid: "beginGeldigheid", position: 0 },
    { geldigheid: "eindeGeldigheid", position: 1 },
  ] as const)(
    "reads the first page of the zaaktypes whose $geldigheid lies in the typed range",
    async ({ geldigheid, position }) => {
      restoreStoredFilters({ page: 1 });
      await render();
      await answer(await expectPageRequest(), {
        resultaten: [fakeZaaktypeConfigurationOverzicht()],
        totaal: 30,
      });

      const user = userEvent.setup();
      await user.type(
        screen.getAllByPlaceholderText("zoeken.filter.van")[position],
        "01-01-2025",
      );
      await user.tab();
      fixture.detectChanges();

      const query = requestedQuery(await expectPageRequest());
      expect(query).toEqual(
        expect.objectContaining({
          page: "0",
          [`${geldigheid}From`]: "2025-01-01",
        }),
      );
      expect(query).not.toHaveProperty(`${geldigheid}Through`);
    },
  );

  it("offers the zaaktypes of the response to filter on, and reads and remembers the first page of the chosen zaaktype", async () => {
    restoreStoredFilters({ page: 1 });
    const setItem = jest.spyOn(SessionStorageUtil, "setItem");
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
      filterZaaktypes: [
        { identificatie: "fakeIdentificatie", omschrijving: "Fake zaaktype" },
      ],
    });

    const user = userEvent.setup();
    await user.click(screen.getAllByRole("combobox")[0]);
    fixture.detectChanges();
    await user.click(
      await screen.findByRole("option", { name: "Fake zaaktype" }),
    );
    fixture.detectChanges();

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({
        page: "0",
        zaaktypeIdentificatie: "fakeIdentificatie",
      }),
    );
    expect(setItem).toHaveBeenCalledWith(
      "zaakafhandelParametersListParameters",
      expect.objectContaining({
        page: 0,
        zaaktype: {
          identificatie: "fakeIdentificatie",
          omschrijving: "Fake zaaktype",
        },
      }),
    );
  });

  it("offers the procesmodellen of the response to filter on, and reads the first page of the chosen procesmodel", async () => {
    restoreStoredFilters({ page: 1 });
    await render();
    await answer(await expectPageRequest(), {
      resultaten: [fakeZaaktypeConfigurationOverzicht()],
      totaal: 30,
      filterProcessDefinitions: [{ key: "fakeKey", naam: "Fake procesmodel" }],
    });

    const user = userEvent.setup();
    await user.click(screen.getAllByRole("combobox")[1]);
    fixture.detectChanges();
    await user.click(
      await screen.findByRole("option", { name: "Fake procesmodel" }),
    );
    fixture.detectChanges();

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({ page: "0", processDefinitionKey: "fakeKey" }),
    );
  });

  it("shows the stored zaaktype and procesmodel as chosen, and reads the page with them", async () => {
    restoreStoredFilters({
      zaaktype: {
        identificatie: "fakeIdentificatie",
        omschrijving: "Fake zaaktype",
      },
      processDefinition: { key: "fakeKey", naam: "Fake procesmodel" },
    });
    await render();

    const request = await expectPageRequest();
    expect(requestedQuery(request)).toEqual(
      expect.objectContaining({
        zaaktypeIdentificatie: "fakeIdentificatie",
        processDefinitionKey: "fakeKey",
      }),
    );
    await answer(request, {
      filterZaaktypes: [
        { identificatie: "fakeIdentificatie", omschrijving: "Fake zaaktype" },
      ],
      filterProcessDefinitions: [{ key: "fakeKey", naam: "Fake procesmodel" }],
    });

    await sleep();
    fixture.detectChanges();

    const [zaaktypeFilter, procesmodelFilter] = screen.getAllByRole("combobox");
    expect(zaaktypeFilter).toHaveTextContent("Fake zaaktype");
    expect(procesmodelFilter).toHaveTextContent("Fake procesmodel");
  });

  it("reads the first page sorted on geldig descending, not on the sort that the page stored before it read pages", async () => {
    sessionStorage.setItem(
      "parameterFilters",
      JSON.stringify({
        ...new ZaakafhandelParametersListParameters("valide", "asc"),
      }),
    );
    await render();

    expect(requestedQuery(await expectPageRequest())).toEqual(
      expect.objectContaining({ sortField: "GELDIG", sortOrder: "DESC" }),
    );
  });
});

describe(`${ParametersComponent.name} on a second visit`, () => {
  it("shows the cached rows of the page it showed before", async () => {
    await configureTestingModule();
    sessionStorage.clear();
    mockWebsocketListener();
    testQueryClient.setQueryData(
      TestBed.inject(ZaakafhandelParametersService).listZaakafhandelParameters(
        toZaaktypeConfigurationListQuery(
          new ZaakafhandelParametersListParameters("geldig", "desc"),
        ),
      ).queryKey,
      fakePage({ resultaten: [fakeZaaktypeConfigurationOverzicht()] }),
    );

    const fixture = TestBed.createComponent(ParametersComponent);
    fixture.detectChanges();
    await sleep();
    fixture.detectChanges();

    expect(
      screen.getByRole("row", { name: /Fake zaaktype/ }),
    ).toBeInTheDocument();
  });
});

describe(toZaaktypeConfigurationListQuery.name, () => {
  it.each([
    [ToggleSwitchOptions.CHECKED, true],
    [ToggleSwitchOptions.UNCHECKED, false],
    [ToggleSwitchOptions.INDETERMINATE, null],
  ])(
    "filters on valide and geldig %s as %s",
    (toggleSwitchOption, expected) => {
      const query = toZaaktypeConfigurationListQuery({
        ...new ZaakafhandelParametersListParameters("geldig", "desc"),
        valide: toggleSwitchOption,
        geldig: toggleSwitchOption,
      });

      expect(query.isValide).toBe(expected);
      expect(query.isNuGeldig).toBe(expected);
    },
  );

  it.each([
    ["valide", "VALIDE"],
    ["omschrijving", "OMSCHRIJVING"],
    ["model", "PROCESS_DEFINITION"],
    ["geldig", "GELDIG"],
    ["beginGeldigheid", "BEGIN_GELDIGHEID"],
    ["eindeGeldigheid", "EINDE_GELDIGHEID"],
  ])("sorts the column %s on the field %s", (column, sortField) => {
    expect(
      toZaaktypeConfigurationListQuery(
        new ZaakafhandelParametersListParameters(column, "asc"),
      ).sortField,
    ).toBe(sortField);
  });

  it("sends the dates of a range as the date filter gives them", () => {
    const query = toZaaktypeConfigurationListQuery({
      ...new ZaakafhandelParametersListParameters("geldig", "desc"),
      beginGeldigheid: { from: "2025-01-01", through: "2025-01-31" },
    });

    expect(query.beginGeldigheidFrom).toBe("2025-01-01");
    expect(query.beginGeldigheidThrough).toBe("2025-01-31");
  });

  it("sends no date filter for a range stored before the date range used from and through", () => {
    const query = toZaaktypeConfigurationListQuery(
      JSON.parse(
        JSON.stringify({
          ...new ZaakafhandelParametersListParameters("geldig", "desc"),
          beginGeldigheid: { van: "2024-12-31T23:00:00.000Z", tot: null },
        }),
      ),
    );

    expect(query.beginGeldigheidFrom).toBeNull();
    expect(query.beginGeldigheidThrough).toBeNull();
  });

  it("keeps the order of the catalogus when the sort is cleared", () => {
    expect(
      toZaaktypeConfigurationListQuery(
        new ZaakafhandelParametersListParameters("omschrijving", ""),
      ).sortField,
    ).toBeNull();
  });
});
