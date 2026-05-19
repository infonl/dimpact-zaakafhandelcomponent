/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ElementRef, QueryList } from "@angular/core";
import { Subject } from "rxjs";
import { DashboardComponent } from "./dashboard.component";
import { DashboardCard } from "./model/dashboard-card";
import { DashboardCardId } from "./model/dashboard-card-id";
import { DashboardCardType } from "./model/dashboard-card-type";

type RequestAnimationFrameCallback = (time: number) => void;

class FakeResizeObserver {
  static lastInstance: FakeResizeObserver | null = null;
  callback: ResizeObserverCallback;
  observed = new Set<Element>();

  constructor(callback: ResizeObserverCallback) {
    this.callback = callback;
    FakeResizeObserver.lastInstance = this;
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

  // helper to fire the callback as if the browser detected a resize
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

function makeCardElement(naturalHeight: number): HTMLElement {
  const element = document.createElement("mat-card");
  element.classList.add("dashboard-card");
  element.getBoundingClientRect = jest.fn(() => {
    const minHeightPx = parseFloat(element.style.minHeight) || 0;
    return { height: Math.max(naturalHeight, minHeightPx) } as DOMRect;
  });
  return element;
}

function makeQueryList<T>(items: T[]): QueryList<T> {
  const list = new QueryList<T>();
  list.reset(items);
  return list;
}

describe("DashboardComponent row-height sync", () => {
  let component: DashboardComponent;
  let originalResizeObserver: typeof ResizeObserver;
  let originalRequestAnimationFrame: typeof requestAnimationFrame;
  let pendingRequestAnimationFrames: RequestAnimationFrameCallback[];
  let stacked: boolean;

  beforeEach(() => {
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

    component = new DashboardComponent({} as never, {} as never, {} as never);
  });

  afterEach(() => {
    component.ngOnDestroy();
    globalThis.ResizeObserver = originalResizeObserver;
    globalThis.requestAnimationFrame = originalRequestAnimationFrame;
    jest.restoreAllMocks();
  });

  function setupCards(grid: number[][], runRequestAnimationFrames = true) {
    // grid[col][row] = natural height of that card in px
    const elementsByPosition = grid.map((column) =>
      column.map((h) => makeCardElement(h)),
    );
    const flatElements = elementsByPosition.flat();

    component.grid = grid.map((column, columnIndex) =>
      column.map(
        (_, rowIndex) =>
          new DashboardCard(
            `card-${columnIndex}-${rowIndex}` as unknown as DashboardCardId,
            DashboardCardType.TAKEN,
          ),
      ),
    );

    const cardElements = makeQueryList(
      flatElements.map((element) => new ElementRef(element)),
    );
    Object.defineProperty(component, "cardElements", {
      configurable: true,
      value: cardElements,
    });

    component.ngAfterViewInit();
    if (runRequestAnimationFrames) flushRequestAnimationFrames();
    return { elementsByPosition, flatElements };
  }

  function flushRequestAnimationFrames() {
    while (pendingRequestAnimationFrames.length > 0) {
      const callback = pendingRequestAnimationFrames.shift()!;
      callback(performance.now());
    }
  }

  it("sets each card's min-height to the tallest card's natural height in its row", () => {
    // 2 columns, 2 rows — row 0 has heights [200, 350], row 1 has [400, 250]
    const { elementsByPosition } = setupCards([
      [200, 400],
      [350, 250],
    ]);

    expect(elementsByPosition[0][0].style.minHeight).toBe("350px");
    expect(elementsByPosition[1][0].style.minHeight).toBe("350px");
    expect(elementsByPosition[0][1].style.minHeight).toBe("400px");
    expect(elementsByPosition[1][1].style.minHeight).toBe("400px");
  });

  it("recomputes natural heights when a previously-applied min-height is no longer valid", () => {
    const { elementsByPosition, flatElements } = setupCards([[200], [500]]);
    expect(elementsByPosition[0][0].style.minHeight).toBe("500px");

    component.grid = [component.grid[0], []];
    const reducedQueryList = makeQueryList([new ElementRef(flatElements[0])]);
    Object.defineProperty(component, "cardElements", {
      configurable: true,
      value: reducedQueryList,
    });
    (component as unknown as { syncRowHeights: () => void }).syncRowHeights();

    expect(elementsByPosition[0][0].style.minHeight).toBe("200px");
  });

  it("clears inline min-heights and skips equalization when stacked", () => {
    stacked = true;

    const { elementsByPosition } = setupCards([
      [200, 400],
      [350, 250],
    ]);

    elementsByPosition.flat().forEach((element) => {
      expect(element.style.minHeight).toBe("");
    });
  });

  it("clears previously equalized heights when transitioning to stacked layout", () => {
    // Start in desktop layout — heights get equalized.
    const { elementsByPosition } = setupCards([
      [200, 400],
      [350, 250],
    ]);
    expect(elementsByPosition[0][0].style.minHeight).not.toBe("");

    // Switch to stacked layout and re-sync; inline min-heights should clear.
    stacked = true;
    (component as unknown as { syncRowHeights: () => void }).syncRowHeights();

    elementsByPosition.flat().forEach((element) => {
      expect(element.style.minHeight).toBe("");
    });
  });

  it("suppresses ResizeObserver-driven re-syncs during the transition window", () => {
    setupCards([[200], [500]]);
    const observer = FakeResizeObserver.lastInstance!;

    const spy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );

    observer.fire();
    expect(spy).not.toHaveBeenCalled();
  });

  it("queues a deferred re-sync when a resize fires during the suppression window", () => {
    setupCards([[200], [500]]);
    const observer = FakeResizeObserver.lastInstance!;
    jest.spyOn(performance, "now").mockReturnValue(0);
    const setTimeoutSpy = jest.spyOn(globalThis, "setTimeout");
    const scheduleSpy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );

    observer.fire();
    expect(scheduleSpy).not.toHaveBeenCalled();
    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);

    observer.fire();
    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);

    const deferred = setTimeoutSpy.mock.calls[0][0] as () => void;
    deferred();
    expect(scheduleSpy).toHaveBeenCalledTimes(1);
  });

  it("re-syncs after the transition window has elapsed", () => {
    setupCards([[200], [500]]);
    const observer = FakeResizeObserver.lastInstance!;

    // Move past the suppression window (200 ms transition).
    jest.spyOn(performance, "now").mockReturnValue(Date.now() + 1000);

    const spy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );
    observer.fire();
    expect(spy).toHaveBeenCalled();
  });

  it("re-runs sync when the cardElements QueryList emits changes", () => {
    setupCards([[200], [500]]);

    const spy = jest.spyOn(
      component as unknown as { observeCards: () => void },
      "observeCards",
    );

    // Simulate Angular emitting a change on the QueryList.
    const changes = component.cardElements.changes as Subject<unknown>;
    changes.next(component.cardElements);
    expect(spy).toHaveBeenCalled();
  });

  it("re-runs sync on window resize", () => {
    setupCards([[200], [500]]);

    const spy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );
    window.dispatchEvent(new Event("resize"));
    expect(spy).toHaveBeenCalled();
  });

  it("schedules a row sync when updateWidth is called", () => {
    setupCards([[200], [500]]);
    const spy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );

    (component as unknown as { updateWidth: () => void }).updateWidth();
    expect(spy).toHaveBeenCalled();
  });

  it("disconnects the ResizeObserver on destroy", () => {
    setupCards([[200]]);
    const observer = FakeResizeObserver.lastInstance!;
    expect(observer.observed.size).toBeGreaterThan(0);

    component.ngOnDestroy();
    expect(observer.observed.size).toBe(0);
  });
});
