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
import { Type } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../../setupJest";
import { GeneratedType } from "../utils/generated-types";

type ZoekParameters = GeneratedType<"RestZoekParameters">;

export type WerklijstColumnFilter =
  | {
      column: string;
      kind: "tekst";
      veld: keyof NonNullable<ZoekParameters["zoeken"]>;
    }
  | {
      column: string;
      kind: "facet";
      veld: keyof NonNullable<ZoekParameters["filters"]>;
    }
  | {
      column: string;
      kind: "datum";
      veld: keyof NonNullable<ZoekParameters["datums"]>;
    };

const FACET_OPTION = "fakeFacetOption";

/**
 * Renders a werklijst and drives one of its column filters the way a user does,
 * returning the search request that the filter caused.
 */
export async function searchThroughColumnFilter(
  component: Type<unknown>,
  filter: WerklijstColumnFilter,
): Promise<ZoekParameters> {
  const user = userEvent.setup();
  const { fixture } = await render(component, {
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
      provideMomentDateAdapter(),
    ],
  });
  const httpTestingController = TestBed.inject(HttpTestingController);

  async function runEffects() {
    await sleep();
    fixture.detectChanges();
    await sleep();
  }

  function answerSearches() {
    const requests = httpTestingController.match("/rest/zoeken/list");
    const filters =
      filter.kind === "facet"
        ? { [filter.veld]: [{ naam: FACET_OPTION, aantal: 1 }] }
        : {};
    requests.forEach((request) =>
      request.flush({ totaal: 0, resultaten: [], filters }),
    );
    return requests;
  }

  function columnHeaderNames() {
    const [headerRow] = screen.getAllByRole("row");
    return within(headerRow)
      .getAllByRole("columnheader")
      .map((header) => header.textContent?.trim());
  }

  async function showColumn(column: string) {
    if (columnHeaderNames().includes(column)) return;
    const columnPicker = screen.getByRole("button", {
      name: "actie.kolommen.wijzig",
    });
    await user.click(columnPicker);
    await user.click(
      within(screen.getByRole("listbox")).getByRole("option", { name: column }),
    );
    await user.click(columnPicker);
    fixture.detectChanges();
  }

  function filterCell(column: string) {
    const [, filterRow] = screen.getAllByRole("row");
    return within(filterRow).getAllByRole("columnheader")[
      columnHeaderNames().indexOf(column)
    ];
  }

  await runEffects();
  answerSearches();
  await runEffects();
  fixture.detectChanges();
  await showColumn(filter.column);

  const cell = filterCell(filter.column);
  switch (filter.kind) {
    case "tekst":
      await user.type(within(cell).getByRole("textbox"), "fakeTekst");
      await user.tab();
      break;
    case "facet":
      await user.click(within(cell).getByRole("combobox"));
      await user.click(screen.getAllByRole("option").at(-1)!);
      break;
    case "datum":
      await user.type(
        within(cell).getByPlaceholderText("zoeken.filter.van"),
        "01-01-2024",
      );
      await user.type(
        within(cell).getByPlaceholderText("zoeken.filter.tot-en-met"),
        "31-01-2024",
      );
      await user.tab();
      break;
  }
  await runEffects();

  const requests = answerSearches();
  return requests[requests.length - 1]?.request.body;
}

/** The part of the search request that a column filter is expected to set. */
export function expectedSearchFor(filter: WerklijstColumnFilter) {
  switch (filter.kind) {
    case "tekst":
      return { zoeken: { [filter.veld]: "fakeTekst" } };
    case "facet":
      return {
        filters: {
          [filter.veld]: { values: [FACET_OPTION], inverse: false },
        },
      };
    case "datum":
      return {
        datums: {
          [filter.veld]: {
            van: new Date(2024, 0, 1).toISOString(),
            tot: new Date(2024, 0, 31).toISOString(),
          },
        },
      };
  }
}
