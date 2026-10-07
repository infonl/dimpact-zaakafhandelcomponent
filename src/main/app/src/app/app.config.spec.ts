/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClientTesting } from "@angular/common/http/testing";
import { Provider } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MatPaginatorIntl } from "@angular/material/paginator";
import { TranslateModule } from "@ngx-translate/core";
import { QueryClient } from "@tanstack/angular-query-experimental";
import { waitFor } from "@testing-library/angular";
import { appConfig } from "./app.config";

describe("appConfig", () => {
  it("provides a paginator with translated labels", () => {
    const paginatorProvider = appConfig.providers.find(
      (provider): provider is Provider =>
        typeof provider === "object" &&
        "provide" in provider &&
        provider.provide === MatPaginatorIntl,
    );
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [paginatorProvider!],
    });

    const paginatorIntl = TestBed.inject(MatPaginatorIntl);

    expect(paginatorIntl.itemsPerPageLabel).toBe("msg.pagina.items");
    expect(paginatorIntl.nextPageLabel).toBe("actie.pagina.volgende");
    expect(paginatorIntl.previousPageLabel).toBe("actie.pagina.vorige");
  });

  describe("when the application environment starts", () => {
    let queryClient: QueryClient;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [...appConfig.providers, provideHttpClientTesting()],
      });
      queryClient = TestBed.inject(QueryClient);
    });

    afterEach(() => {
      queryClient.clear();
      window.sessionStorage.clear();
    });

    it("persists cached queries to the session storage", async () => {
      queryClient.setQueryData(["/rest/identity/loggedInUser"], {
        id: "fakeUserId",
      });

      await waitFor(
        () =>
          expect(window.sessionStorage.getItem("zac:tanstack:query")).toContain(
            "fakeUserId",
          ),
        { timeout: 3000 },
      );
    });
  });
});
