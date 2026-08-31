/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import {
  HttpTestingController,
  provideHttpClientTesting,
} from "@angular/common/http/testing";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { provideRouter } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { notifyManager } from "@tanstack/query-core";
import { render, screen, within } from "@testing-library/angular";
import userEvent from "@testing-library/user-event";

import { fromPartial } from "src/test-helpers";
import { sleep, testQueryClient } from "../../../setupJest";
import { UtilService } from "../core/service/util.service";
import { WebsocketService } from "../core/websocket/websocket.service";
import { GeneratedType } from "../shared/utils/generated-types";
import { DashboardComponent } from "./dashboard.component";
import { DashboardCardId } from "./model/dashboard-card-id";

type RequestAnimationFrameCallback = (time: number) => void;

const LOGGED_IN_USER_QUERY_KEY = ["/rest/identity/loggedInUser"];
const DASHBOARD_CARDS_URL = "/rest/gebruikersvoorkeuren/dasboardcard/actief";
const DASHBOARD_CARD_URL = "/rest/gebruikersvoorkeuren/dasboardcard";
const SIGNALERING_TYPEN_URL = "/rest/signaleringen/typen/dashboard";

const instellingen: GeneratedType<"RESTDashboardCardInstelling">[] = [
  { cardId: "MIJN_TAKEN", column: 0, row: 0 },
  { cardId: "MIJN_ZAKEN_NIEUW", column: 0, row: 1 },
  { cardId: "MIJN_ZAKEN", column: 1, row: 0 },
  { cardId: "MIJN_DOCUMENTEN_NIEUW", column: 1, row: 1 },
];

class FakeResizeObserver {
  static instances: FakeResizeObserver[] = [];
  callback: ResizeObserverCallback;
  observed = new Set<Element>();

  constructor(callback: ResizeObserverCallback) {
    this.callback = callback;
    FakeResizeObserver.instances.push(this);
  }

  observe(target: Element) {
    this.observed.add(target);
  }

  disconnect() {
    this.observed.clear();
  }

  unobserve(target: Element) {
    this.observed.delete(target);
  }

  fire() {
    const entries = Array.from(this.observed).map(
      (target) =>
        ({
          target,
          contentRect: target.getBoundingClientRect() as DOMRectReadOnly,
        }) as ResizeObserverEntry,
    );
    this.callback(entries, this as unknown as ResizeObserver);
  }
}

describe(DashboardComponent.name, () => {
  let fixture: ComponentFixture<DashboardComponent>;
  let httpTestingController: HttpTestingController;
  let originalResizeObserver: typeof ResizeObserver;
  let originalRequestAnimationFrame: typeof requestAnimationFrame;
  let pendingRequestAnimationFrames: RequestAnimationFrameCallback[];
  let stacked: boolean;

  const user = userEvent.setup();

  beforeEach(() => {
    notifyManager.setScheduler((fn) => fn());

    FakeResizeObserver.instances = [];
    originalResizeObserver = globalThis.ResizeObserver;
    (globalThis as { ResizeObserver: unknown }).ResizeObserver =
      FakeResizeObserver;

    originalRequestAnimationFrame = globalThis.requestAnimationFrame;
    pendingRequestAnimationFrames = [];
    globalThis.requestAnimationFrame = ((
      callback: RequestAnimationFrameCallback,
    ) => {
      pendingRequestAnimationFrames.push(callback);
      return pendingRequestAnimationFrames.length;
    }) as typeof requestAnimationFrame;

    stacked = false;
    jest.spyOn(window, "matchMedia").mockImplementation(
      (query: string) =>
        ({
          matches: stacked && query.includes("max-width"),
          media: query,
          addListener: jest.fn(),
          removeListener: jest.fn(),
          addEventListener: jest.fn(),
          removeEventListener: jest.fn(),
          dispatchEvent: jest.fn(),
          onchange: null,
        }) as unknown as MediaQueryList,
    );
  });

  afterEach(() => {
    notifyManager.setScheduler((fn) => Promise.resolve().then(fn));
    globalThis.ResizeObserver = originalResizeObserver;
    globalThis.requestAnimationFrame = originalRequestAnimationFrame;
    jest.restoreAllMocks();
  });

  async function setup() {
    testQueryClient.setQueryData(
      LOGGED_IN_USER_QUERY_KEY,
      fromPartial<GeneratedType<"RestUser">>({
        id: "fakeUserId",
        naam: "fakeUserName",
      }),
    );

    const rendered = await render(DashboardComponent, {
      imports: [NoopAnimationsModule, TranslateModule.forRoot()],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideTanStackQuery(testQueryClient),
        { provide: UtilService, useValue: { setTitle: jest.fn() } },
        { provide: WebsocketService, useValue: { addListener: jest.fn() } },
      ],
    });

    fixture = rendered.fixture;
    httpTestingController = TestBed.inject(HttpTestingController);

    httpTestingController.expectOne(DASHBOARD_CARDS_URL).flush(instellingen);
    httpTestingController
      .expectOne(SIGNALERING_TYPEN_URL)
      .flush(["ZAAK_OP_NAAM", "ZAAK_DOCUMENT_TOEGEVOEGD"]);
    await settle();

    giveNaturalHeight(DashboardCardId.MIJN_TAKEN, 200);
    giveNaturalHeight(DashboardCardId.MIJN_ZAKEN, 350);
    giveNaturalHeight(DashboardCardId.MIJN_ZAKEN_NIEUW, 400);
    giveNaturalHeight(DashboardCardId.MIJN_DOCUMENTEN_NIEUW, 250);
  }

  async function settle() {
    await sleep();
    fixture.detectChanges();
    httpTestingController
      .match(() => true)
      .forEach((request) => request.flush({ resultaten: [], totaal: 0 }));
    await sleep();
    fixture.detectChanges();
    flushAnimationFrames();
  }

  function flushAnimationFrames() {
    while (pendingRequestAnimationFrames.length > 0) {
      const callback = pendingRequestAnimationFrames.shift()!;
      callback(performance.now());
    }
  }

  function cardOf(cardId: DashboardCardId) {
    return screen
      .getByText(`dashboard.card.${cardId}`)
      .closest<HTMLElement>("mat-card")!;
  }

  function minHeightOf(cardId: DashboardCardId) {
    return cardOf(cardId).style.minHeight;
  }

  function giveNaturalHeight(cardId: DashboardCardId, naturalHeight: number) {
    const card = cardOf(cardId);
    card.getBoundingClientRect = jest.fn(() => {
      const appliedMinHeight = parseFloat(card.style.minHeight) || 0;
      return { height: Math.max(naturalHeight, appliedMinHeight) } as DOMRect;
    });
  }

  function dashboardObserver() {
    return FakeResizeObserver.instances.find((observer) =>
      Array.from(observer.observed).some((element) =>
        element.classList.contains("dashboard-card"),
      ),
    )!;
  }

  function resizeWindow() {
    window.dispatchEvent(new Event("resize"));
    flushAnimationFrames();
  }

  async function switchToEditMode() {
    await user.click(
      screen.getByRole("switch", { name: "Dashboard aanpassen" }),
    );
    await sleep();
    fixture.detectChanges();
    flushAnimationFrames();
  }

  async function deleteCard(cardId: DashboardCardId) {
    await user.click(
      within(cardOf(cardId)).getByRole("button", {
        name: "actie.card.verwijderen",
      }),
    );
    await sleep();
    const request = httpTestingController.expectOne(DASHBOARD_CARD_URL);
    request.flush([]);
    await sleep();
    fixture.detectChanges();
    flushAnimationFrames();
    return request;
  }

  it("gives every card in a row the height of the tallest card in that row", async () => {
    await setup();

    resizeWindow();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("350px");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN)).toBe("350px");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN_NIEUW)).toBe("400px");
    expect(minHeightOf(DashboardCardId.MIJN_DOCUMENTEN_NIEUW)).toBe("400px");
  });

  it("measures natural heights again instead of reusing the height a card borrowed earlier", async () => {
    await setup();
    resizeWindow();
    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("350px");

    await switchToEditMode();
    await deleteCard(DashboardCardId.MIJN_ZAKEN);

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("250px");
  });

  it("leaves the cards their own height when they stack into a single column", async () => {
    stacked = true;
    await setup();

    resizeWindow();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN_NIEUW)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_DOCUMENTEN_NIEUW)).toBe("");
  });

  it("drops the equalized heights when the window shrinks to a stacked layout", async () => {
    await setup();
    resizeWindow();
    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).not.toBe("");

    stacked = true;
    resizeWindow();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN_NIEUW)).toBe("");
    expect(minHeightOf(DashboardCardId.MIJN_DOCUMENTEN_NIEUW)).toBe("");
  });

  it("ignores card resizes it caused itself while the height transition is still running", async () => {
    await setup();
    jest.spyOn(performance, "now").mockReturnValue(0);
    resizeWindow();

    giveNaturalHeight(DashboardCardId.MIJN_TAKEN, 500);
    dashboardObserver().fire();
    flushAnimationFrames();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("350px");
  });

  it("catches up with a card that resized during the transition once it has ended", async () => {
    await setup();
    jest.spyOn(performance, "now").mockReturnValue(0);
    resizeWindow();
    const setTimeoutSpy = jest.spyOn(globalThis, "setTimeout");

    giveNaturalHeight(DashboardCardId.MIJN_TAKEN, 500);
    dashboardObserver().fire();
    dashboardObserver().fire();

    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);
    const deferredSync = setTimeoutSpy.mock.calls[0][0] as () => void;
    deferredSync();
    flushAnimationFrames();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("500px");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN)).toBe("500px");
  });

  it("equalizes again once the transition window has elapsed", async () => {
    await setup();
    resizeWindow();

    giveNaturalHeight(DashboardCardId.MIJN_TAKEN, 500);
    jest.spyOn(performance, "now").mockReturnValue(Date.now() + 1000);
    dashboardObserver().fire();
    flushAnimationFrames();

    expect(minHeightOf(DashboardCardId.MIJN_TAKEN)).toBe("500px");
    expect(minHeightOf(DashboardCardId.MIJN_ZAKEN)).toBe("500px");
  });

  it("stops watching a card that the user removed from the dashboard", async () => {
    await setup();
    const removedCard = cardOf(DashboardCardId.MIJN_ZAKEN);
    expect(dashboardObserver().observed).toContain(removedCard);

    await switchToEditMode();
    await deleteCard(DashboardCardId.MIJN_ZAKEN);

    expect(dashboardObserver().observed).not.toContain(removedCard);
    expect(
      screen.queryByText(`dashboard.card.${DashboardCardId.MIJN_ZAKEN}`),
    ).toBeNull();
  });

  it("stops watching every card when the dashboard is destroyed", async () => {
    await setup();
    const observer = dashboardObserver();
    expect(observer.observed.size).toBeGreaterThan(0);

    fixture.destroy();

    expect(observer.observed.size).toBe(0);
  });

  it("removes the instelling of the card the user deleted", async () => {
    await setup();
    await switchToEditMode();

    const request = await deleteCard(DashboardCardId.MIJN_ZAKEN);

    expect(request.request.method).toBe("DELETE");
    expect(request.request.body).toMatchObject({ cardId: "MIJN_ZAKEN" });
  });

  it("deletes the next card with the instellingen the server returned", async () => {
    await setup();
    await switchToEditMode();

    await user.click(
      within(cardOf(DashboardCardId.MIJN_ZAKEN)).getByRole("button", {
        name: "actie.card.verwijderen",
      }),
    );
    await sleep();
    httpTestingController
      .expectOne(DASHBOARD_CARD_URL)
      .flush([
        { cardId: "MIJN_TAKEN", column: 2, row: 7 },
      ] satisfies GeneratedType<"RESTDashboardCardInstelling">[]);
    await sleep();
    fixture.detectChanges();
    flushAnimationFrames();

    const request = await deleteCard(DashboardCardId.MIJN_TAKEN);

    expect(request.request.body).toMatchObject({
      cardId: "MIJN_TAKEN",
      column: 2,
      row: 7,
    });
  });
});
