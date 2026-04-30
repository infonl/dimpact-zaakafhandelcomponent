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

type RAFCallback = (time: number) => void;

class FakeResizeObserver {
  static lastInstance: FakeResizeObserver | null = null;
  callback: ResizeObserverCallback;
  observed = new Set<Element>();

  constructor(cb: ResizeObserverCallback) {
    this.callback = cb;
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

function makeCardEl(naturalHeight: number): HTMLElement {
  const el = document.createElement("mat-card");
  el.classList.add("dashboard-card");
  // getBoundingClientRect reflects the larger of natural height and applied
  // min-height — that's how the real browser would size a flex card.
  el.getBoundingClientRect = jest.fn(() => {
    const minHeightPx = parseFloat(el.style.minHeight) || 0;
    return { height: Math.max(naturalHeight, minHeightPx) } as DOMRect;
  });
  return el;
}

function makeQueryList<T>(items: T[]): QueryList<T> {
  const list = new QueryList<T>();
  list.reset(items);
  return list;
}

describe("DashboardComponent row-height sync", () => {
  let component: DashboardComponent;
  let originalRO: typeof ResizeObserver;
  let originalRAF: typeof requestAnimationFrame;
  let pendingRAFs: RAFCallback[];
  let stacked: boolean;

  beforeEach(() => {
    originalRO = globalThis.ResizeObserver;
    (globalThis as { ResizeObserver: unknown }).ResizeObserver =
      FakeResizeObserver;

    originalRAF = globalThis.requestAnimationFrame;
    pendingRAFs = [];
    globalThis.requestAnimationFrame = ((cb: RAFCallback) => {
      pendingRAFs.push(cb);
      return pendingRAFs.length;
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
    globalThis.ResizeObserver = originalRO;
    globalThis.requestAnimationFrame = originalRAF;
    jest.restoreAllMocks();
  });

  function setupCards(grid: number[][], runRAFs = true) {
    // grid[col][row] = natural height of that card in px
    const elsByPosition = grid.map((column) =>
      column.map((h) => makeCardEl(h)),
    );
    const flatEls = elsByPosition.flat();

    component.grid = grid.map((column, colIdx) =>
      column.map(
        (_, rowIdx) =>
          new DashboardCard(
            `card-${colIdx}-${rowIdx}` as unknown as DashboardCardId,
            DashboardCardType.TAKEN,
          ),
      ),
    );

    const cardEls = makeQueryList(flatEls.map((el) => new ElementRef(el)));
    Object.defineProperty(component, "cardEls", {
      configurable: true,
      value: cardEls,
    });

    component.ngAfterViewInit();
    if (runRAFs) flushRAFs();
    return { elsByPosition, flatEls };
  }

  function flushRAFs() {
    while (pendingRAFs.length > 0) {
      const cb = pendingRAFs.shift()!;
      cb(performance.now());
    }
  }

  it("sets each card's min-height to the tallest card's natural height in its row", () => {
    // 2 columns, 2 rows — row 0 has heights [200, 350], row 1 has [400, 250]
    const { elsByPosition } = setupCards([
      [200, 400],
      [350, 250],
    ]);

    expect(elsByPosition[0][0].style.minHeight).toBe("350px");
    expect(elsByPosition[1][0].style.minHeight).toBe("350px");
    expect(elsByPosition[0][1].style.minHeight).toBe("400px");
    expect(elsByPosition[1][1].style.minHeight).toBe("400px");
  });

  it("recomputes natural heights when a previously-applied min-height is no longer valid", () => {
    const { elsByPosition, flatEls } = setupCards([[200], [500]]);
    expect(elsByPosition[0][0].style.minHeight).toBe("500px");

    // Simulate the tall card being removed: drop its data + element, re-sync.
    component.grid = [component.grid[0], []];
    const reducedQueryList = makeQueryList([new ElementRef(flatEls[0])]);
    Object.defineProperty(component, "cardEls", {
      configurable: true,
      value: reducedQueryList,
    });
    (component as unknown as { syncRowHeights: () => void }).syncRowHeights();

    // The remaining card should shrink back to its natural height (200).
    expect(elsByPosition[0][0].style.minHeight).toBe("200px");
  });

  it("resets min-heights to 0px and skips equalization when stacked", () => {
    stacked = true;

    const { elsByPosition } = setupCards([
      [200, 400],
      [350, 250],
    ]);

    elsByPosition.flat().forEach((el) => {
      expect(el.style.minHeight).toBe("0px");
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
    // Force performance.now() inside the suppression window.
    jest.spyOn(performance, "now").mockReturnValue(0);
    const setTimeoutSpy = jest.spyOn(globalThis, "setTimeout");
    const scheduleSpy = jest.spyOn(
      component as unknown as { scheduleRowSync: () => void },
      "scheduleRowSync",
    );

    observer.fire();
    expect(scheduleSpy).not.toHaveBeenCalled();
    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);

    // A second fire while a timer is already pending should not schedule again.
    observer.fire();
    expect(setTimeoutSpy).toHaveBeenCalledTimes(1);

    // Run the deferred callback — it should call scheduleRowSync.
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

  it("re-runs sync when the cardEls QueryList emits changes", () => {
    setupCards([[200], [500]]);

    const spy = jest.spyOn(
      component as unknown as { observeCards: () => void },
      "observeCards",
    );

    // Simulate Angular emitting a change on the QueryList.
    const changes = component.cardEls.changes as Subject<unknown>;
    changes.next(component.cardEls);
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
