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
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { TaakZoekObject } from "../../zoeken/model/taken/taak-zoek-object";
import { TakenMijnComponent } from "./taken-mijn.component";

describe(TakenMijnComponent.name, () => {
  let fixture: ComponentFixture<TakenMijnComponent>;
  let setTitle: jest.SpyInstance;
  let httpTestingController: HttpTestingController;

  const user = userEvent.setup();

  beforeEach(async () => {
    sessionStorage.clear();
    setTitle = jest.spyOn(UtilService.prototype, "setTitle");

    const { fixture: renderedFixture } = await render(TakenMijnComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
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
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideQueryClient(testQueryClient),
        provideNativeDateAdapter(),
      ],
    });

    fixture = renderedFixture;
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  async function showTaken(taken: Partial<TaakZoekObject>[]) {
    await sleep();
    httpTestingController
      .match("/rest/zoeken/list")
      .forEach((request) =>
        request.flush({ totaal: taken.length, resultaten: taken, filters: {} }),
      );
    await sleep();
    // the table creates the row views in one pass and binds their cells in the next
    fixture.detectChanges();
    fixture.detectChanges();
  }

  function filterRow() {
    return screen.getAllByRole("row")[1];
  }

  function columnHeaderNames() {
    return screen
      .getAllByRole("columnheader")
      .map((header) => header.textContent?.trim());
  }

  it("sets the page title", () => {
    expect(setTitle).toHaveBeenCalledWith("title.taken.mijn");
  });

  it("shows the default set of columns", async () => {
    await showTaken([]);

    const headers = columnHeaderNames();
    expect(headers).toEqual(
      expect.arrayContaining([
        "naam",
        "zaakIdentificatie",
        "zaakOmschrijving",
        "zaaktype",
        "creatiedatum",
        "fataledatum",
        "dagenTotFataledatum",
        "groep",
      ]),
    );
    expect(headers).not.toContain("zaakToelichting");
    expect(headers).not.toContain("toelichting");
  });

  it("keeps the column actions pinned in their own column", async () => {
    await showTaken([]);

    expect(
      screen.getByRole("button", { name: "actie.kolommen.reset" }),
    ).toBeVisible();
    expect(
      screen.getByRole("button", { name: "actie.kolommen.wijzig" }),
    ).toBeVisible();
  });

  it("tells you there are no taken when the search comes back empty", async () => {
    await showTaken([]);

    expect(screen.getByText("msg.geen.gegevens.gevonden")).toBeVisible();
  });

  it("flags a taak whose fatale datum has passed", async () => {
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
    await showTaken([
      fromPartial<TaakZoekObject>({
        id: "fakeTaakId",
        naam: "fakeTaakNaam",
        fataledatum: "2999-01-01",
      }),
    ]);

    expect(screen.queryByTitle("msg.datum.overschreden")).toBeNull();
  });

  it("searches again when a column filter is changed", async () => {
    await showTaken([]);

    const zaakIdentificatieFilter =
      within(filterRow()).getAllByRole("textbox")[0];
    await user.type(zaakIdentificatieFilter, "fakeZaakIdentificatie");
    await user.tab();
    await sleep();

    const requests = httpTestingController.match("/rest/zoeken/list");
    expect(requests).toHaveLength(1);
    expect(requests[0].request.body).toEqual(
      expect.objectContaining({
        type: "TAAK",
        alleenMijnTaken: true,
        zoeken: expect.objectContaining({
          TAAK_ZAAK_ID: "fakeZaakIdentificatie",
        }),
      }),
    );
    requests[0].flush({ totaal: 0, resultaten: [], filters: {} });
  });

  it("brings a column hidden through the column picker back", async () => {
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
