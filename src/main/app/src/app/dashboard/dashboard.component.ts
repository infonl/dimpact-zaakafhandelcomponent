/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024-2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  CdkDragDrop,
  moveItemInArray,
  transferArrayItem,
} from "@angular/cdk/drag-drop";
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChild,
  ViewChildren,
} from "@angular/core";
import { FormControl } from "@angular/forms";
import { MatMenuTrigger } from "@angular/material/menu";
import moment from "moment";
import { forkJoin, Subscription } from "rxjs";
import { UtilService } from "../core/service/util.service";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren/gebruikersvoorkeuren.service";
import { SessionStorageUtil } from "../shared/storage/session-storage.util";
import { GeneratedType } from "../shared/utils/generated-types";
import { SignaleringenService } from "../signaleringen.service";
import { DashboardCard } from "./model/dashboard-card";
import { DashboardCardId } from "./model/dashboard-card-id";
import { DashboardCardType } from "./model/dashboard-card-type";

@Component({
  templateUrl: "./dashboard.component.html",
  styleUrls: ["./dashboard.component.less"],
  standalone: false,
})
export class DashboardComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild(MatMenuTrigger) menuTrigger!: MatMenuTrigger;
  @ViewChildren("cardElement", { read: ElementRef })
  cardElements!: QueryList<ElementRef<HTMLElement>>;

  private resizeObserver?: ResizeObserver;
  private cardElementsChangesSub?: Subscription;
  private syncScheduled = false;
  private suppressObserverUntil = 0;
  private pendingSyncTimer?: ReturnType<typeof setTimeout>;
  private static readonly TRANSITION_DURATION_MS = 200;
  // Must match the `@media (max-width: 1200px)` breakpoint in the .less file
  // where cards stack into a single column.
  private static readonly STACK_LAYOUT_MEDIA_QUERY = "(max-width: 1200px)";

  /** all cards that may be put on the dashboard */
  private cards = [
    new DashboardCard(
      DashboardCardId.MIJN_TAKEN,
      DashboardCardType.TAAK_ZOEKEN,
    ),
    new DashboardCard(
      DashboardCardId.MIJN_TAKEN_NIEUW,
      DashboardCardType.TAKEN,
      "TAAK_OP_NAAM",
    ),
    new DashboardCard(
      DashboardCardId.MIJN_ZAKEN,
      DashboardCardType.ZAAK_ZOEKEN,
    ),
    new DashboardCard(
      DashboardCardId.MIJN_ZAKEN_NIEUW,
      DashboardCardType.ZAKEN,
      "ZAAK_OP_NAAM",
    ),
    new DashboardCard(
      DashboardCardId.MIJN_DOCUMENTEN_NIEUW,
      DashboardCardType.ZAKEN,
      "ZAAK_DOCUMENT_TOEGEVOEGD",
    ),
    new DashboardCard(
      DashboardCardId.MIJN_ZAKEN_WAARSCHUWING,
      DashboardCardType.ZAAK_WAARSCHUWINGEN,
      "ZAAK_VERLOPEND",
    ),
  ];

  dashboardCardType = DashboardCardType;
  columnsInDashboard = 0;
  editMode = new FormControl(false);
  showHint = false;

  instellingen: GeneratedType<"RESTDashboardCardInstelling">[] = []; // the last loaded card settings
  available: DashboardCard[] = []; // cards that are not on the dashboard
  grid: Array<DashboardCard[]> = []; // cards that are on the dashboard

  constructor(
    private readonly utilService: UtilService,
    private readonly signaleringenService: SignaleringenService,
    private readonly gebruikersvoorkeurenService: GebruikersvoorkeurenService,
  ) {}

  ngOnInit() {
    this.utilService.setTitle("title.dashboard");
    this.loadCards(SessionStorageUtil.getItem("dashboardWidth", 3));
    // TODO instead of session storage use userpreferences in a db
    SessionStorageUtil.setItem("dashboardOpened", moment());
    this.signaleringenService.updateSignaleringen();
  }

  private readonly onWindowResize = () => this.scheduleRowSync();

  ngAfterViewInit() {
    this.resizeObserver = new ResizeObserver(() => {
      const now = performance.now();
      if (now < this.suppressObserverUntil) {
        // A real size change (e.g. a card finished loading) happened while we
        // were suppressing observer feedback from our own min-height writes.
        // Schedule a sync once the suppression window ends so we don't drop it.
        if (this.pendingSyncTimer) return;
        this.pendingSyncTimer = setTimeout(() => {
          this.pendingSyncTimer = undefined;
          this.scheduleRowSync();
        }, this.suppressObserverUntil - now);
        return;
      }
      this.scheduleRowSync();
    });
    this.observeCards();
    this.cardElementsChangesSub = this.cardElements.changes.subscribe(() =>
      this.observeCards(),
    );
    window.addEventListener("resize", this.onWindowResize);
  }

  ngOnDestroy() {
    this.resizeObserver?.disconnect();
    this.cardElementsChangesSub?.unsubscribe();
    if (this.pendingSyncTimer) clearTimeout(this.pendingSyncTimer);
    window.removeEventListener("resize", this.onWindowResize);
  }

  private observeCards() {
    if (!this.resizeObserver) return;
    this.resizeObserver.disconnect();
    this.cardElements.forEach((ref) =>
      this.resizeObserver!.observe(ref.nativeElement),
    );
    this.scheduleRowSync();
  }

  private scheduleRowSync() {
    if (this.syncScheduled) return;
    this.syncScheduled = true;
    requestAnimationFrame(() => {
      this.syncScheduled = false;
      this.syncRowHeights();
    });
  }

  private isStackedLayout() {
    return window.matchMedia(DashboardComponent.STACK_LAYOUT_MEDIA_QUERY)
      .matches;
  }

  private syncRowHeights() {
    if (!this.cardElements) return;
    const els = this.cardElements.toArray().map((r) => r.nativeElement);
    const previousMins = els.map((el) => el.style.minHeight);

    // Disable transitions while we briefly reset min-height to read each
    // card's natural content height.
    els.forEach((el) => el.classList.add("dashboard-card--measuring"));
    els.forEach((el) => (el.style.minHeight = "0px"));

    this.suppressObserverUntil =
      performance.now() + DashboardComponent.TRANSITION_DURATION_MS;

    if (this.isStackedLayout()) {
      els.forEach((el) => el.classList.remove("dashboard-card--measuring"));
      return;
    }

    const rendered: { row: number; el: HTMLElement }[] = [];
    let idx = 0;
    for (const column of this.grid) {
      if (!this.editMode.value && column.length === 0) continue;
      for (let r = 0; r < column.length && idx < els.length; r++) {
        rendered.push({ row: r, el: els[idx++] });
      }
    }

    const rowMax = new Map<number, number>();
    rendered.forEach(({ row, el }) => {
      const h = el.getBoundingClientRect().height;
      rowMax.set(row, Math.max(rowMax.get(row) ?? 0, h));
    });

    // Restore previous min-heights before re-enabling transitions and committing
    // them via a forced layout, so the browser's transition starting point is
    // the actual previous value.
    els.forEach((el, i) => (el.style.minHeight = previousMins[i]));
    void els[0]?.offsetHeight;
    els.forEach((el) => el.classList.remove("dashboard-card--measuring"));

    rendered.forEach(({ row, el }) => {
      const h = rowMax.get(row);
      if (h != null) el.style.minHeight = `${h}px`;
    });
  }

  private loadCards(width: number) {
    while (this.grid.length < width) {
      this.grid.push([]);
    }
    forkJoin([
      this.gebruikersvoorkeurenService.listDashboardCards(),
      this.signaleringenService.listDashboardSignaleringTypen(),
    ]).subscribe(([dashboardInstellingen, signaleringInstellingen]) => {
      this.instellingen = dashboardInstellingen;
      this.addExistingCards(dashboardInstellingen, signaleringInstellingen);
      this.addNewCards(signaleringInstellingen);
      this.updateWidth();
      this.updateAvailable();
    });
  }

  // add configured cards (except when disabled by corresponding signaleringen settings)
  private addExistingCards(
    dashboardInstellingen: typeof this.instellingen,
    signaleringenInstellingen: GeneratedType<"Type">[],
  ) {
    dashboardInstellingen.forEach((instelling) => {
      const card = this.cards.find((c) => c.id === instelling.cardId);
      if (!card) return;
      if (card.signaleringType == null) {
        this.putCard(card, instelling.column ?? undefined);
        return;
      }
      const i = signaleringenInstellingen.indexOf(card.signaleringType);
      if (i < 0) return;
      this.putCard(card, instelling.column ?? undefined);
      signaleringenInstellingen.splice(i, 1);
    });
  }

  // add unconfigured cards (when enabled by the corresponding signaleringen settings)
  private addNewCards(signaleringenInstellingen: GeneratedType<"Type">[]) {
    signaleringenInstellingen.forEach((signaleringType) => {
      const card = this.cards.find(
        (c) => c.signaleringType === signaleringType,
      );
      if (!card) return;
      this.addCard(card);
    });
  }

  // find a good position for a new card
  private addCard(card: DashboardCard) {
    let shortest = 0;
    this.grid.forEach((_column, idx) => {
      if (this.grid[idx].length < this.grid[shortest].length) {
        shortest = idx;
      }
    });
    return this.putCard(card, shortest);
  }

  private putCard(card: DashboardCard, column = 0) {
    return new Position(column, this.grid[column].push(card) - 1);
  }

  private updateWidth() {
    this.columnsInDashboard = this.grid.reduce(
      (count, column) => count + (column.length > 0 ? 1 : 0),
      0,
    );
    this.scheduleRowSync();
  }

  private updateAvailable() {
    this.available = this.cards.filter((card) => this.isAvailable(card));
    this.showHint = this.available.length === this.cards.length;
  }

  private isAvailable(card: DashboardCard) {
    return !this.grid.some((column) =>
      column.some((row) => row.id === card.id),
    );
  }

  move(event: CdkDragDrop<DashboardCard[]>) {
    const sameColumn = event.previousContainer.data === event.container.data;
    const sameRow = event.previousIndex === event.currentIndex;
    if (sameColumn && sameRow) return;
    if (sameColumn) {
      moveItemInArray(
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    } else {
      transferArrayItem(
        event.previousContainer.data,
        event.container.data,
        event.previousIndex,
        event.currentIndex,
      );
    }
    this.saveCards();
    this.updateWidth();
  }

  hint() {
    this.editMode.setValue(true);
    setTimeout(() => {
      this.menuTrigger.openMenu();
    }, 666);
  }

  add(card: DashboardCard) {
    const position = this.addCard(card);
    this.saveCard(card, position.column, position.row);
    this.updateWidth();
    this.updateAvailable();
  }

  delete(card: DashboardCard) {
    const colIdx = this.grid.findIndex((column) =>
      column.some((c) => c.id === card.id),
    );
    if (colIdx === -1) return;
    const rowIdx = this.grid[colIdx].findIndex((c) => c.id === card.id);
    if (rowIdx === -1) return;
    this.grid[colIdx].splice(rowIdx, 1);
    this.deleteCard(card);
    this.updateWidth();
    this.updateAvailable();
  }

  private saveCards() {
    this.gebruikersvoorkeurenService
      .updateDashboardCards(this.getInstellingen())
      .subscribe((dashboardInstellingen) => {
        this.instellingen = dashboardInstellingen;
      });
  }

  private saveCard(card: DashboardCard, column: number, row: number) {
    this.gebruikersvoorkeurenService
      .addDashboardCard(this.getInstellingAt(card, column, row))
      .subscribe((dashboardInstellingen) => {
        this.instellingen = dashboardInstellingen;
      });
  }

  private deleteCard(card: DashboardCard) {
    this.gebruikersvoorkeurenService
      .deleteDashboardCard(this.getInstelling(card))
      .subscribe((dashboardInstellingen) => {
        this.instellingen = dashboardInstellingen;
      });
  }

  private getInstellingen() {
    return this.grid.flatMap((rows, column) =>
      rows.map((row, rowIndex) => this.getInstellingAt(row, column, rowIndex)),
    );
  }

  private getInstellingAt(card: DashboardCard, column: number, row: number) {
    const instelling = this.getInstelling(card);
    instelling.column = column;
    instelling.row = row;
    return instelling;
  }

  private getInstelling(card: DashboardCard) {
    const existing = this.instellingen.find((inst) => inst.cardId === card.id);
    if (existing) {
      return { ...existing, signaleringType: card.signaleringType };
    }
    return {
      cardId: card.id,
      signaleringType: card.signaleringType,
      // Add other required properties with default values if needed
    } satisfies GeneratedType<"RESTDashboardCardInstelling">;
  }
}

class Position {
  constructor(
    public column: number,
    public row: number,
  ) {}
}
