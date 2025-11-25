/*
 * SPDX-FileCopyrightText: 2021-2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  effect,
  EventEmitter,
  inject,
  input,
  OnDestroy,
  viewChild,
} from "@angular/core";

import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormControl } from "@angular/forms";
import { MatPaginator } from "@angular/material/paginator";
import { MatSidenav } from "@angular/material/sidenav";
import { merge, of, Subject, takeUntil } from "rxjs";
import { map, switchMap } from "rxjs/operators";
import { UtilService } from "../../core/service/util.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DocumentZoekObject } from "../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../model/zaken/zaak-zoek-object";
import { DEFAULT_ZOEK_PARAMETERS } from "../model/zoek-parameters";
import { ZoekResultaat } from "../model/zoek-resultaat";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
import { ZoekenService } from "../zoeken.service";

@Component({
  selector: "zac-zoeken",
  templateUrl: "./zoek.component.html",
  styleUrls: ["./zoek.component.less"],
})
export class ZoekComponent implements AfterViewInit, OnDestroy {
  private readonly zoekenService = inject(ZoekenService);
  protected readonly utilService = inject(UtilService);

  private readonly paginator = viewChild.required(MatPaginator);
  protected readonly zoekenSideNav = input<MatSidenav>();

  private readonly destroy$ = new Subject<void>();

  zoekType: ZoekType = ZoekType.ZAC;
  ZoekType = ZoekType;
  ZoekVeld = ZoekVeld;
  zoekResultaat = new ZoekResultaat<
    GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
  >();
  zoekParameters: GeneratedType<"RestZoekParameters"> = DEFAULT_ZOEK_PARAMETERS;
  isLoadingResults = true;
  slow = false;
  zoekveldControl = new FormControl<ZoekVeld>(ZoekVeld.ALLE);
  trefwoordenControl = new FormControl("");
  zoek = new EventEmitter<void>();
  hasSearched = false;
  hasTaken = false;
  hasZaken = false;
  hasDocument = false;
  huidigZoekVeld: ZoekVeld = ZoekVeld.ALLE;

  constructor() {
    effect(() => {
      const trefwoorden = this.zoekenService.trefwoorden();
      if (this.trefwoordenControl.value === trefwoorden) return;
      this.trefwoordenControl.setValue(trefwoorden);
    });

    this.trefwoordenControl.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((trefwoorden) => {
        this.zoekenService.trefwoorden.set(trefwoorden);
      });

    this.zoekenService.reset$
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.reset());
  }

  public ngAfterViewInit() {
    this.zoek.pipe(takeUntil(this.destroy$)).subscribe(() => {
      this.paginator().pageIndex = 0;
    });
    this.zoekenSideNav()
      ?.openedStart.pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.zoek.emit();
      });

    merge(this.paginator().page, this.zoek)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => {
          if (!this.trefwoordenControl.value) {
            this.zoekenSideNav()?.close();
            return of(null);
          }

          this.slow = false;
          setTimeout(() => {
            this.slow = true;
          }, 500);
          this.isLoadingResults = true;
          this.utilService.setLoading(true);
          return this.zoekenService.list(this.getZoekParameters());
        }),
        map((data) => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
          return data;
        }),
      )
      .subscribe((data) => {
        if (!data) return;
        this.paginator().length = data.totaal ?? 0;
        this.hasSearched = true;
        this.zoekenService.hasSearched.set(true);
        this.zoekResultaat = data as ZoekResultaat<
          GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
        >;
        this.bepaalContext();
      });
  }

  private bepaalContext() {
    this.hasZaken = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) => naam === ("ZAAK" satisfies GeneratedType<"ZoekObjectType">),
    )?.aantal;
    this.hasTaken = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) => naam === ("TAAK" satisfies GeneratedType<"ZoekObjectType">),
    )?.aantal;
    this.hasDocument = !!this.zoekResultaat.filters.TYPE?.find(
      ({ naam }) =>
        naam === ("DOCUMENT" satisfies GeneratedType<"ZoekObjectType">),
    )?.aantal;

    if (this.zoekParameters.filters?.TYPE?.values?.length) {
      if (this.hasZaken) {
        this.hasZaken = this.zoekParameters.filters.TYPE.values.includes(
          "ZAAK" satisfies GeneratedType<"ZoekObjectType">,
        );
      }
      if (this.hasTaken) {
        this.hasTaken = this.zoekParameters.filters.TYPE.values.includes(
          "TAAK" satisfies GeneratedType<"ZoekObjectType">,
        );
      }
      if (this.hasDocument) {
        this.hasDocument =
          this.zoekParameters.filters.TYPE?.values.includes(
            "DOCUMENT" satisfies GeneratedType<"ZoekObjectType">,
          ) ?? false;
      }
    }
  }

  private getZoekParameters() {
    if (
      this.zoekveldControl.value &&
      this.trefwoordenControl.value &&
      this.zoekParameters.zoeken
    ) {
      this.zoekParameters.zoeken[this.zoekveldControl.value] =
        this.trefwoordenControl.value;
    }

    this.zoekParameters.page = this.paginator().pageIndex;
    this.zoekParameters.rows = this.paginator().pageSize;
    return this.zoekParameters;
  }

  protected getZaakZoekObject(
    zoekObject: GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
  ): ZaakZoekObject {
    return zoekObject as ZaakZoekObject;
  }

  protected getTaakZoekObject(
    zoekObject: GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
  ): TaakZoekObject {
    return zoekObject as TaakZoekObject;
  }

  protected getDocumentZoekObject(
    zoekObject: GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">,
  ): DocumentZoekObject {
    return zoekObject as DocumentZoekObject;
  }

  protected hasOption(options: GeneratedType<"FilterResultaat">[]) {
    return options.length
      ? !(options.length === 1 && options[0].naam === "-NULL-")
      : false;
  }

  protected keywordsChange() {
    if (
      this.zoekveldControl.value &&
      this.trefwoordenControl.value !==
        this.zoekParameters.zoeken?.[this.zoekveldControl.value]
    ) {
      this.zoek.emit();
    }
  }

  protected originalOrder = () => 0;

  protected setZoektype(zoekType: ZoekType) {
    this.zoekType = zoekType;
    if (zoekType === ZoekType.ZAC) {
      this.trefwoordenControl.enable();
    } else {
      this.trefwoordenControl.disable();
    }
  }

  protected reset() {
    this.zoekenService.hasSearched.set(false);
    this.paginator().length = 0;
    this.trefwoordenControl.setValue("");
    this.zoekveldControl.setValue(ZoekVeld.ALLE);
    this.zoekResultaat = new ZoekResultaat();
    this.zoekParameters = DEFAULT_ZOEK_PARAMETERS;
    this.hasSearched = false;
    this.hasTaken = false;
    this.hasZaken = false;
    this.hasDocument = false;
  }

  protected zoekVeldChanged() {
    delete this.zoekParameters.zoeken?.[this.huidigZoekVeld];
    if (!this.zoekveldControl.value) return;

    this.huidigZoekVeld = this.zoekveldControl.value;
    if (!this.trefwoordenControl.value) return;
    this.zoek.emit();
  }

  protected dateFilterChange(
    key: string,
    value: GeneratedType<"RestDatumRange">,
  ) {
    if (!this.zoekParameters.datums) this.zoekParameters.datums = {};
    this.zoekParameters.datums[key] = value;
    this.zoek.emit();
  }

  protected filterChanged(
    key: string,
    value?: GeneratedType<"FilterParameters">,
  ) {
    if (!this.zoekParameters.filters) this.zoekParameters.filters = {};
    if (!this.zoekParameters.filters[key]) {
      this.zoekParameters.filters[key] = { values: [] };
    }
    if (!value) {
      delete this.zoekParameters.filters[key];
    } else {
      this.zoekParameters.filters[key] = value;
    }
    this.zoek.emit();
  }

  protected betrokkeneActief() {
    return !!(
      this.zoekParameters.zoeken?.ZAAK_BETROKKENEN ||
      this.zoekParameters.zoeken?.ZAAK_INITIATOR ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_BELANGHEBBENDE ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_ADVISEUR ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_BESLISSER ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_KLANTCONTACTER ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_ZAAKCOORDINATOR ||
      this.zoekParameters.zoeken?.ZAAK_BETROKKENE_MEDE_INITIATOR
    );
  }

  public ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
