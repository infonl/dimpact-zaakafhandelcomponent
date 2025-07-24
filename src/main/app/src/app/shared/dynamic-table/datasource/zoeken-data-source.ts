/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { DataSource } from "@angular/cdk/collections";
import { CdkDragDrop, moveItemInArray } from "@angular/cdk/drag-drop";
import { EventEmitter } from "@angular/core";
import { MatPaginator } from "@angular/material/paginator";
import { MatSort, SortDirection } from "@angular/material/sort";
import { BehaviorSubject, Observable, Subscription, merge } from "rxjs";
import { finalize, tap } from "rxjs/operators";
import { UtilService } from "../../../core/service/util.service";
import { FilterResultaat } from "../../../zoeken/model/filter-resultaat";
import { FilterVeld } from "../../../zoeken/model/filter-veld";
import { DEFAULT_ZOEK_PARAMETERS } from "../../../zoeken/model/zoek-parameters";
import { ZoekResultaat } from "../../../zoeken/model/zoek-resultaat";
import { ZoekenService } from "../../../zoeken/zoeken.service";
import {
  SessionStorageUtil,
  WerklijstZoekParameter,
} from "../../storage/session-storage.util";
import { GeneratedType } from "../../utils/generated-types";
import { ColumnPickerValue } from "../column-picker/column-picker-value";
import { ZoekenColumn } from "../model/zoeken-column";

export abstract class ZoekenDataSource<
  OBJECT extends
    GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
> extends DataSource<OBJECT> {
  zoekParameters: GeneratedType<"RestZoekParameters">;
  beschikbareFilters: Partial<Record<FilterVeld, FilterResultaat[]>> = {};
  totalItems = 0;
  paginator!: MatPaginator;
  sort!: MatSort;

  filtersChanged$ = new EventEmitter<void>();

  private tableSubject = new BehaviorSubject<OBJECT[]>([]);
  private _defaultColumns = new Map<ZoekenColumn, ColumnPickerValue>();
  private _columns = new Map<ZoekenColumn, ColumnPickerValue>();
  private _sessionKey = "";
  private _visibleColumns: Array<ZoekenColumn> = [];
  private _filterColumns: Array<string> = [];
  private _detailExpandColumns: Array<ZoekenColumn> = [];
  private _drop = false;
  private subscriptions$: Subscription[] = [];

  protected constructor(
    public readonly werklijst: GeneratedType<"Werklijst">,
    private readonly zoekenService: ZoekenService,
    private readonly utilService: UtilService,
  ) {
    super();
    this.zoekParameters = SessionStorageUtil.getItem(
      `${werklijst}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      DEFAULT_ZOEK_PARAMETERS,
    );
  }

  protected abstract initZoekparameters(
    zoekParameters: GeneratedType<"RestZoekParameters">,
  ): GeneratedType<"RestZoekParameters">;

  private updateZoekParameters() {
    this.zoekParameters = this.initZoekparameters(this.zoekParameters);
    this.zoekParameters.page = this.paginator.pageIndex;
    this.zoekParameters.rows = this.paginator.pageSize;
    this.zoekParameters.sorteerRichting = this.sort.direction;
    this.zoekParameters.sorteerVeld = this.sort
      .active as GeneratedType<"SorteerVeld">;

    return SessionStorageUtil.setItem(
      `${this.werklijst}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.zoekParameters,
    );
  }

  connect(): Observable<OBJECT[] | ReadonlyArray<OBJECT>> {
    this.subscriptions$.push(
      this.sort.sortChange.subscribe(() => (this.paginator.pageIndex = 0)),
    );
    this.subscriptions$.push(
      merge(this.sort.sortChange, this.paginator.page)
        .pipe(tap(() => this.load()))
        .subscribe(),
    );
    return this.tableSubject.asObservable() as Observable<OBJECT[]>;
  }

  /**
   *  Called when the table is being destroyed. Use this function, to clean up
   * any open connections or free any held resources that were set up during connect.
   */
  disconnect(): void {
    this.subscriptions$.forEach((s) => {
      s.unsubscribe();
    });
    this.tableSubject.complete();
  }

  load(delay = 0): void {
    setTimeout(() => {
      this.utilService.setLoading(true);
      this.zoekenService
        .list(this.updateZoekParameters())
        .pipe(finalize(() => this.utilService.setLoading(false)))
        .subscribe((zaakResponse) => {
          this.setData(zaakResponse as ZoekResultaat<OBJECT>);
        });
    }, delay);
  }

  clear() {
    this.totalItems = 0;
    this.tableSubject.next([]);
  }

  drop(event: CdkDragDrop<string[]>) {
    this._drop = true;
    setTimeout(() => (this._drop = false), 1000);
    const extraIndex = this.visibleColumns.includes(ZoekenColumn.SELECT)
      ? 1
      : 0;
    moveItemInArray(
      this.visibleColumns,
      event.previousIndex + extraIndex,
      event.currentIndex + extraIndex,
    );
    moveItemInArray(
      this.filterColumns,
      event.previousIndex + extraIndex,
      event.currentIndex + extraIndex,
    );
  }

  private setData(response: ZoekResultaat<OBJECT>) {
    this.totalItems = response.totaal;
    this.tableSubject.next(response.resultaten);
    this.beschikbareFilters = response.filters;
  }

  setViewChilds(paginator: MatPaginator, sort: MatSort) {
    this.paginator = paginator;
    this.sort = sort;
    this.load();
  }

  /**
   * Columns can only be instantiated with the initColumns method
   *
   * @param defaultColumns available columns
   */
  initColumns(defaultColumns: Map<ZoekenColumn, ColumnPickerValue>): void {
    const key = this.werklijst + "Columns";
    const sessionColumnsString = SessionStorageUtil.getItem<string>(key, "");
    const sessionColumns: Map<ZoekenColumn, ColumnPickerValue> | undefined =
      sessionColumnsString !== ""
        ? new Map(JSON.parse(sessionColumnsString))
        : undefined;
    // sometimes we remove / add columns based on the logged in user / policies.
    // to support switching between users within the same session, the default columns must be leading.
    // we only map the ColumnPickerValues from session storage for columns that are in the default column list.
    const mergedEntries = [...defaultColumns.entries()].map(
      ([k, v]) => [k, sessionColumns?.get(k) || v] as const,
    );
    const columns = new Map(mergedEntries);
    this._defaultColumns = defaultColumns;
    this._columns = columns;
    this._sessionKey = key;
    this.updateColumns(columns);
  }

  resetColumns() {
    this._columns = new Map(this._defaultColumns);
    this.updateColumns(this._defaultColumns);
  }

  /**
   * Update column visibility
   *
   * @param columns updated columns
   */
  updateColumns(columns: Map<ZoekenColumn, ColumnPickerValue>): void {
    this._visibleColumns = [...columns.keys()].filter(
      (key) => columns.get(key) !== ColumnPickerValue.HIDDEN,
    );
    this._detailExpandColumns = [...columns.keys()].filter(
      (key) => columns.get(key) === ColumnPickerValue.HIDDEN,
    );
    this._filterColumns = this.visibleColumns.map((c) => c + "_filter");
    this.storeColumns(columns);
  }

  reset() {
    this.zoekParameters = SessionStorageUtil.setItem(
      `${this.werklijst}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      DEFAULT_ZOEK_PARAMETERS,
    );
    if (this.zoekParameters.sorteerVeld)
      this.sort.active = this.zoekParameters.sorteerVeld;
    if (this.zoekParameters.sorteerRichting)
      this.sort.direction = this.zoekParameters
        .sorteerRichting as SortDirection;
    this.paginator.pageIndex = 0;
    this.paginator.pageSize = this.zoekParameters.rows ?? 0;
    this.load();
  }

  filtersChanged() {
    this.paginator.pageIndex = 0;
    this.filtersChanged$.emit();
    this.load();
  }

  private storeColumns(columns: Map<string, ColumnPickerValue>) {
    const columnsString = JSON.stringify(Array.from(columns.entries()));
    SessionStorageUtil.setItem(this._sessionKey, columnsString);
  }

  /* column getters, NO setters!*/
  get columns() {
    return this._columns;
  }

  get visibleColumns() {
    return this._visibleColumns;
  }

  get detailExpandColumns() {
    return this._detailExpandColumns;
  }

  get filterColumns() {
    return this._filterColumns;
  }

  get data(): OBJECT[] {
    return this.tableSubject.value as OBJECT[];
  }

  zoekopdrachtChanged(actieveZoekopdracht: GeneratedType<"RESTZoekopdracht">) {
    if (!this._drop) {
      // view is reinitialized after a drop event, but the data doesn't change, so don't reload the data after a drop event.
      if (actieveZoekopdracht?.json) {
        this.zoekParameters = JSON.parse(actieveZoekopdracht.json);
        if (this.zoekParameters.sorteerVeld)
          this.sort.active = this.zoekParameters.sorteerVeld;
        if (this.zoekParameters.sorteerRichting)
          this.sort.direction = this.zoekParameters
            .sorteerRichting as SortDirection;
        this.load();
      } else if (actieveZoekopdracht === null) {
        this.reset();
      } else {
        this.load();
      }
    }
  }

  zoekopdrachtResetToFirstPage() {
    this.zoekParameters.page = 0;

    SessionStorageUtil.setItem(
      `${this.werklijst}_ZOEKPARAMETERS` satisfies WerklijstZoekParameter,
      this.zoekParameters,
    );
  }
}
