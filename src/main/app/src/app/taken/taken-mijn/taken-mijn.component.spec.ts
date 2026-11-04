/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  provideHttpClient,
  withInterceptorsFromDi,
  withXhr,
} from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { provideNativeDateAdapter } from "@angular/material/core";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import {
  expectedSearchFor,
  fromPartial,
  searchAfterErasingTheOnlyDate,
  searchThroughColumnFilter,
  WerklijstColumnFilter,
} from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenMijnComponent } from "./taken-mijn.component";

describe(TakenMijnComponent.name, () => {
  let fixture: ComponentFixture<TakenMijnComponent>;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(() => {
    sessionStorage.clear();
  });

  async function renderTakenMijn() {
    const { fixture: renderedFixture } = await render(TakenMijnComponent, {
      imports: [NoopAnimationsModule],
      providers: [
        provideTranslateService(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: fromPartial<ActivatedRoute>({
            data: of({
              tabelGegevens: {
                aantalPerPagina: 10,
                pageSizeOptions: [10, 25, 50],
                werklijstRechten: fromPartial<
                  GeneratedType<"RestWerklijstRechten">
                >({}),
              },
            }),
          }),
        },
        provideHttpClient(withXhr(), withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideNativeDateAdapter(),
      ],
    });

    fixture = renderedFixture;
    httpTestingController = TestBed.inject(HttpTestingController);
  }

  /** The search query starts its request, and hands over its response, in effects. */
  async function runEffects() {
    await sleep();
    fixture.detectChanges();
    await sleep();
  }

  async function showTaken(taken: Partial<TaakZoekObject>[]) {
    await runEffects();
    httpTestingController
      .match("/rest/zoeken/list")
      .forEach((request) =>
        request.flush({ totaal: taken.length, resultaten: taken, filters: {} }),
      );
    await runEffects();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function columnHeaderNames() {
    return screen
      .getAllByRole("columnheader")
      .map((header) => header.textContent?.trim());
  }

  it("shows the default set of columns", async () => {
    await renderTakenMijn();

    await showTaken([]);

    const headers = columnHeaderNames();
    expect(headers).toEqual(
      expect.arrayContaining([
        "naam",
        "zaak-identificatie",
        "zaak-omschrijving",
        "zaaktype",
        "creatiedatum",
        "fataledatum",
        "dagen-tot-fataledatum",
        "groep",
      ]),
    );
    expect(headers).not.toContain("zaak-toelichting");
    expect(headers).not.toContain("toelichting");
  });

  it("keeps the column actions pinned in their own column", async () => {
    await renderTakenMijn();

    await showTaken([]);

    expect(
      screen.getByRole("button", { name: "actie.kolommen.reset" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "actie.kolommen.wijzig" }),
    ).toBeVisible();
  });

  it("tells you there are no taken when the search comes back empty", async () => {
    await renderTakenMijn();

    await showTaken([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("flags a taak whose fatale datum has passed", async () => {
    await renderTakenMijn();

    await showTaken([
      fromPartial<TaakZoekObject>({
        id: "fakeTaakId",
        naam: "fakeTaakNaam",
        fataledatum: "2020-01-01",
      }),
    ]);

    expect(screen.getByTitle("msg.datum.overschreden")).toBeVisible();
  });

  it("does not flag a taak whose fatale datum lies ahead", async () => {
    await renderTakenMijn();

    await showTaken([
      fromPartial<TaakZoekObject>({
        id: "fakeTaakId",
        naam: "fakeTaakNaam",
        fataledatum: "2999-01-01",
      }),
    ]);

    expect(screen.queryByTitle("msg.datum.overschreden")).toBeNull();
  });

  it("brings a column hidden through the column picker back", async () => {
    await renderTakenMijn();

    await showTaken([]);

    const columnPicker = screen.getByRole("button", {
      name: "actie.kolommen.wijzig",
    });
    await user.click(columnPicker);
    await user.click(
      within(screen.getByRole("listbox")).getByRole("option", { name: "naam" }),
    );
    await user.click(columnPicker);
    fixture.detectChanges();

    expect(columnHeaderNames()).not.toContain("naam");

    await user.click(
      screen.getByRole("button", { name: "actie.kolommen.reset" }),
    );
    fixture.detectChanges();

    expect(columnHeaderNames()).toContain("naam");
  });

  it("stores the new page size for this werklijst", async () => {
    await renderTakenMijn();

    await showTaken([]);

    await user.click(screen.getByRole("combobox", { name: /Items per page/i }));
    await user.click(screen.getByRole("option", { name: "25" }));
    await sleep();

    httpTestingController
      .expectOne(
        "/rest/gebruikersvoorkeuren/aantal-per-pagina/WERKVOORRAAD_TAKEN/25",
      )
      .flush(null);
    httpTestingController
      .match("/rest/zoeken/list")
      .forEach((request) =>
        request.flush({ totaal: 0, resultaten: [], filters: {} }),
      );
  });
});

describe(`${TakenMijnComponent.name} column filters`, () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it.each<WerklijstColumnFilter>([
    { column: "naam", kind: "facet", veld: "TAAK_NAAM" },
    { column: "zaak-identificatie", kind: "tekst", veld: "TAAK_ZAAK_ID" },
    { column: "zaak-omschrijving", kind: "tekst", veld: "ZAAK_OMSCHRIJVING" },
    { column: "zaak-toelichting", kind: "tekst", veld: "ZAAK_TOELICHTING" },
    { column: "zaaktype", kind: "facet", veld: "ZAAKTYPE" },
    { column: "creatiedatum", kind: "datum", veld: "TAAK_CREATIEDATUM" },
    { column: "fataledatum", kind: "datum", veld: "TAAK_FATALEDATUM" },
    { column: "groep", kind: "facet", veld: "GROEP" },
    { column: "toelichting", kind: "tekst", veld: "TAAK_TOELICHTING" },
  ])("searches on what was put in the $column filter", async (filter) => {
    expect(
      await searchThroughColumnFilter(TakenMijnComponent, filter),
    ).toMatchObject(expectedSearchFor(filter));
  });

  it("leaves an erased date out of a search started from another filter", async () => {
    const toelichtingFilter: WerklijstColumnFilter = {
      column: "toelichting",
      kind: "tekst",
      veld: "TAAK_TOELICHTING",
    };

    const search = await searchAfterErasingTheOnlyDate(
      TakenMijnComponent,
      { column: "creatiedatum", kind: "datum", veld: "TAAK_CREATIEDATUM" },
      toelichtingFilter,
    );

    expect(search).toMatchObject(expectedSearchFor(toelichtingFilter));
    expect(search.datums?.TAAK_CREATIEDATUM?.van ?? null).toBeNull();
  });
});
