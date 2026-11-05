/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
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
import type { Provider, Type } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { provideMomentDateAdapter } from "@angular/material-moment-adapter";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ActivatedRoute, provideRouter } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import type {
  CreateMutationOptions,
  MutationFunctionContext,
} from "@tanstack/angular-query-experimental";
import { provideQueryClient } from "@tanstack/angular-query-experimental";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";
import { of } from "rxjs";
import { sleep, testQueryClient } from "../setupJest";
import { GeneratedType } from "./app/shared/utils/generated-types";
import { ZoekenService } from "./app/zoeken/zoeken.service";

type DeepPartial<T> = T extends null | undefined
  ? T
  : T extends object
    ? { [P in keyof T]?: DeepPartial<T[P]> }
    : T;

export const fromPartial = <T,>(partial: NoInfer<DeepPartial<T>>): T =>
  partial as T;

/**
 * Invokes the `onSuccess` a service attached to its mutation options, without
 * going through TanStack Query.
 */
export function runMutationOnSuccess<
  TData,
  TError,
  TVariables,
  TOnMutateResult,
>(
  options: CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  variables?: TVariables,
  data?: TData,
) {
  return options.onSuccess?.(
    data as TData,
    variables as TVariables,
    undefined as TOnMutateResult,
    fromPartial<MutationFunctionContext>({}),
  );
}

/**
 * Invokes the `onSettled` a service attached to its mutation options, without
 * going through TanStack Query. Pass an `error` to settle it as failed.
 */
export function runMutationOnSettled<
  TData,
  TError,
  TVariables,
  TOnMutateResult,
>(
  options: CreateMutationOptions<TData, TError, TVariables, TOnMutateResult>,
  error: TError | null = null,
  variables?: TVariables,
  data?: TData,
) {
  return options.onSettled?.(
    data,
    error,
    variables as TVariables,
    undefined as TOnMutateResult,
    fromPartial<MutationFunctionContext>({}),
  );
}

export function createMutationOptions<TData, TVariables = void>(data: TData) {
  const mutationFn = jest
    .fn<Promise<TData>, [TVariables]>()
    .mockResolvedValue(data);

  return {
    mutationKey: ["test-mutation"],
    mutationFn,
  };
}

export function createQueryOptions<TData>(data: TData) {
  const queryFn = jest.fn<Promise<TData>, []>().mockResolvedValue(data);

  return {
    queryKey: ["test-query"],
    queryFn,
  };
}

export const flushMicrotasks = () => Promise.resolve();

/**
 * The zoeken service with the retries of its search switched off: a dropped
 * connection is retried 3 times over 7 seconds, which a spec does not wait for.
 */
export function provideZoekenServiceWithoutRetries(): Provider {
  return {
    provide: ZoekenService,
    useFactory: () => {
      const zoekenService = new ZoekenService();
      const list = zoekenService.list.bind(zoekenService);
      zoekenService.list = (body) => ({ ...list(body), retry: false });
      return zoekenService;
    },
  };
}

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
  const werklijst = await renderWerklijst(component, [filter]);
  return werklijst.useFilter(filter);
}

/**
 * Sets a date in one column filter and erases it again, then searches through
 * another column filter, returning the search request of that last filter.
 */
export async function searchAfterErasingTheOnlyDate(
  component: Type<unknown>,
  datumFilter: WerklijstColumnFilter & { kind: "datum" },
  otherFilter: WerklijstColumnFilter,
): Promise<ZoekParameters> {
  const werklijst = await renderWerklijst(component, [
    datumFilter,
    otherFilter,
  ]);
  await werklijst.eraseTheOnlyDate(datumFilter);
  return werklijst.useFilter(otherFilter);
}

async function renderWerklijst(
  component: Type<unknown>,
  usedFilters: WerklijstColumnFilter[],
) {
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
    const filters = Object.fromEntries(
      usedFilters
        .filter((filter) => filter.kind === "facet")
        .map(({ veld }) => [veld, [{ naam: FACET_OPTION, aantal: 1 }]]),
    );
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

  async function lastSearchBody() {
    await runEffects();
    const requests = answerSearches();
    return requests[requests.length - 1]?.request.body;
  }

  async function eraseTheOnlyDate(filter: WerklijstColumnFilter) {
    await showColumn(filter.column);
    const startInput = within(filterCell(filter.column)).getByPlaceholderText(
      "zoeken.filter.van",
    );
    await user.type(startInput, "01-01-2024");
    await user.tab();
    await lastSearchBody();
    await user.clear(startInput);
    await user.tab();
    await lastSearchBody();
  }

  async function useFilter(filter: WerklijstColumnFilter) {
    await showColumn(filter.column);
    await driveFilter(filter);
    return lastSearchBody();
  }

  async function driveFilter(filter: WerklijstColumnFilter) {
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
  }

  await runEffects();
  answerSearches();
  await runEffects();
  fixture.detectChanges();

  return { useFilter, eraseTheOnlyDate };
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
            van: "2024-01-01T00:00:00.000+01:00",
            tot: "2024-01-31T00:00:00.000+01:00",
          },
        },
      };
  }
}
