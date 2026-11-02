/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  TestRequest,
} from "@angular/common/http/testing";
import { signal } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { provideTranslateService } from "@ngx-translate/core";
import { lastValueFrom } from "rxjs";
import { sleep } from "../../../../setupJest";
import { FoutDetailedDialogComponent } from "../../fout-afhandeling/dialog/fout-detailed-dialog.component";
import { injectLastSuccessfulSearch } from "./last-successful-search";

type FakeParams = { page: number; filter?: string };
type FakeResult = { rows: string[] };

const FAKE_SEARCH_URL = "/rest/fake-search";
const WAARSCHUWING = "msg.zoeken.mislukt.vorige.zoekopdracht";

describe(injectLastSuccessfulSearch.name, () => {
  const dialog = { open: jest.fn(), closeAll: jest.fn() };

  function setup() {
    TestBed.configureTestingModule({
      providers: [
        provideTranslateService(),
        { provide: MatDialog, useValue: dialog },
      ],
    });
    const httpClient = TestBed.inject(HttpClient);
    const requested = signal<FakeParams>({ page: 0 });
    const onRevert = jest.fn();
    const search = TestBed.runInInjectionContext(() =>
      injectLastSuccessfulSearch<FakeParams, FakeResult>({
        requested,
        query: (params) => ({
          queryKey: [FAKE_SEARCH_URL, params],
          queryFn: () =>
            lastValueFrom(httpClient.put<FakeResult>(FAKE_SEARCH_URL, params)),
          staleTime: Infinity,
        }),
        onRevert,
      }),
    );
    const navigate = jest
      .spyOn(TestBed.inject(Router), "navigate")
      .mockResolvedValue(true);

    return { search, requested, onRevert, navigate };
  }

  async function settle() {
    TestBed.tick();
    await sleep();
    TestBed.tick();
    await sleep();
    TestBed.tick();
  }

  async function searchFor(page: number) {
    await settle();
    return TestBed.inject(HttpTestingController).expectOne(
      (request) =>
        request.url === FAKE_SEARCH_URL &&
        (request.body as FakeParams).page === page,
    );
  }

  async function succeed(request: TestRequest, rows: string[]) {
    request.flush({ rows } satisfies FakeResult);
    await settle();
  }

  async function fail(request: TestRequest) {
    request.flush(
      { message: "fakeMessage", exception: "fakeException" },
      { status: 400, statusText: "Bad Request" },
    );
    await settle();
  }

  async function expireSession(request: TestRequest) {
    request.error(new ProgressEvent("error"), { status: 0 });
    await settle();
  }

  describe("given a search that succeeded", () => {
    let context: ReturnType<typeof setup>;

    beforeEach(async () => {
      context = setup();
      await succeed(await searchFor(0), ["ZAAK-001"]);
    });

    it("shows its rows together with the parameters they were searched with", () => {
      expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
      expect(context.search.params()).toEqual({ page: 0 });
    });

    it("shows the rows of the next search once that one succeeds", async () => {
      context.search.request({ page: 1 });
      await succeed(await searchFor(1), ["ZAAK-002"]);

      expect(context.search.data()).toEqual({ rows: ["ZAAK-002"] });
      expect(context.search.params()).toEqual({ page: 1 });
    });

    it("keeps showing its rows while the next search is on its way", async () => {
      context.search.request({ page: 1 });
      const nextSearch = await searchFor(1);

      expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
      expect(context.search.params()).toEqual({ page: 0 });
      await succeed(nextSearch, ["ZAAK-002"]);
    });

    describe("when a search with other parameters fails", () => {
      beforeEach(async () => {
        context.search.request({ page: 1, filter: "fakeFilter" });
        await fail(await searchFor(1));
      });

      it("keeps showing the rows of the last successful search", () => {
        expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
        expect(context.search.params()).toEqual({ page: 0 });
      });

      it("puts the requested parameters back on those of the shown rows, so the page and the filters bound to them follow the rows", () => {
        expect(context.search.requested()).toEqual({ page: 0 });
      });

      it("tells the caller which parameters it went back to, so a copy of them elsewhere can follow", () => {
        expect(context.onRevert).toHaveBeenCalledWith({ page: 0 });
      });

      it("opens one dialog, titled that the search failed, that says the previous results and filters are shown again, with the error of the server", () => {
        expect(dialog.open).toHaveBeenCalledTimes(1);
        expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
          data: expect.objectContaining({
            titel: "dialoog.error.titel.zoeken.mislukt",
            waarschuwing: WAARSCHUWING,
            error: "fakeMessage",
            details: "fakeException",
          }),
        });
      });

      it("searches again when the failed parameters are asked for once more", async () => {
        context.search.request({ page: 1, filter: "fakeFilter" });
        await succeed(await searchFor(1), ["ZAAK-002"]);

        expect(context.search.data()).toEqual({ rows: ["ZAAK-002"] });
      });
    });

    it("goes back to the parameters the shown rows were searched with, also when the requested ones were changed in place", async () => {
      context.requested().filter = "fakeFilterWrittenInPlace";
      context.search.request({ page: 1 });
      await fail(await searchFor(1));

      expect(context.search.params()).toEqual({ page: 0 });
      expect(context.search.requested()).toEqual({ page: 0 });
    });

    describe("when a search with other parameters fails because the session has expired", () => {
      beforeEach(async () => {
        context.search.request({ page: 1 });
        await expireSession(await searchFor(1));
      });

      it("sends the user to the error page, as every other read does", () => {
        expect(context.navigate).toHaveBeenCalledWith(["/fout"]);
      });

      it("opens no dialog", () => {
        expect(dialog.open).not.toHaveBeenCalled();
      });
    });

    it("searches with what was written into the requested parameters in place, once they are asked for", async () => {
      context.requested().filter = "fakeFilterWrittenInPlace";
      context.search.request({ ...context.requested() });
      const request = await searchFor(0);

      expect(request.request.body).toEqual({
        page: 0,
        filter: "fakeFilterWrittenInPlace",
      });
      await succeed(request, ["ZAAK-002"]);
    });

    describe("when the same parameters are asked for again", () => {
      it("searches again, so that a change on the server shows", async () => {
        context.search.request({ page: 0 });
        await succeed(await searchFor(0), ["ZAAK-003"]);

        expect(context.search.data()).toEqual({ rows: ["ZAAK-003"] });
      });

      it("keeps its rows and only logs the error when that search fails, as for every other read that still has data", async () => {
        context.search.request({ page: 0 });
        await fail(await searchFor(0));

        expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
        expect(dialog.open).not.toHaveBeenCalled();
        expect(console.error).toHaveBeenCalled();
      });

      it("sends the user to the error page when that search fails because the session has expired", async () => {
        context.search.request({ page: 0 });
        await expireSession(await searchFor(0));

        expect(context.navigate).toHaveBeenCalledWith(["/fout"]);
      });
    });

    describe("given overlapping searches", () => {
      it("goes back to the shown rows when the latest search fails, also when an earlier one has not answered yet", async () => {
        context.search.request({ page: 1 });
        const earlierSearch = await searchFor(1);
        context.search.request({ page: 2 });
        const latestSearch = await searchFor(2);

        await fail(latestSearch);

        expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
        expect(context.search.requested()).toEqual({ page: 0 });
        await succeed(earlierSearch, ["ZAAK-002"]);
        expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
      });
    });

    it("shows nothing once it is cleared, until the next search succeeds", async () => {
      context.search.clear();
      expect(context.search.data()).toBeUndefined();

      context.search.request({ page: 1 });
      await succeed(await searchFor(1), ["ZAAK-002"]);
      expect(context.search.data()).toEqual({ rows: ["ZAAK-002"] });
    });
  });

  describe("given a first search that fails", () => {
    let context: ReturnType<typeof setup>;

    beforeEach(async () => {
      context = setup();
      await fail(await searchFor(0));
    });

    it("opens the error dialog without saying that previous results are shown, since there are none", () => {
      expect(dialog.open).toHaveBeenCalledTimes(1);
      expect(dialog.open).toHaveBeenCalledWith(FoutDetailedDialogComponent, {
        data: expect.objectContaining({
          titel: undefined,
          waarschuwing: undefined,
          details: "fakeException",
        }),
      });
    });

    it("stays on the requested parameters", () => {
      expect(context.search.requested()).toEqual({ page: 0 });
      expect(context.onRevert).not.toHaveBeenCalled();
    });

    it("searches again when the same parameters are asked for again", async () => {
      context.search.request({ page: 0 });
      await succeed(await searchFor(0), ["ZAAK-001"]);

      expect(context.search.data()).toEqual({ rows: ["ZAAK-001"] });
    });
  });
});
