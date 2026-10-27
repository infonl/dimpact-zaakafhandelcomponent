/*
 * SPDX-FileCopyrightText: 2021-2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KeyValuePipe, NgClass } from "@angular/common";
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  input,
  OnDestroy,
  signal,
  untracked,
} from "@angular/core";

import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { FormControl, ReactiveFormsModule } from "@angular/forms";
import { MatIconButton } from "@angular/material/button";
import { MatDividerModule } from "@angular/material/divider";
import {
  MatFormField,
  MatLabel,
  MatSuffix,
} from "@angular/material/form-field";
import { MatIcon } from "@angular/material/icon";
import { MatInput } from "@angular/material/input";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressBar } from "@angular/material/progress-bar";
import { MatOption, MatSelect } from "@angular/material/select";
import { MatSidenav } from "@angular/material/sidenav";

import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { Subject, takeUntil } from "rxjs";
import { BagZoekComponent } from "../../bag/bag-zoek/bag-zoek.component";
import { UtilService } from "../../core/service/util.service";
import { BedrijfZoekComponent } from "../../klanten/zoek/bedrijven/bedrijf-zoek.component";
import { PersoonZoekComponent } from "../../klanten/zoek/personen/persoon-zoek.component";
import { PolicyService } from "../../policy/policy.service";
import { injectLastSuccessfulSearch } from "../../shared/http/last-successful-search";
import { GeneratedType } from "../../shared/utils/generated-types";
import { DocumentZoekObject } from "../model/documenten/document-zoek-object";
import { TaakZoekObject } from "../model/taken/taak-zoek-object";
import { ZaakZoekObject } from "../model/zaken/zaak-zoek-object";
import { getDefaultZoekParameters } from "../model/zoek-parameters";
import { ZoekResultaat } from "../model/zoek-resultaat";
import { ZoekType } from "../model/zoek-type";
import { ZoekVeld } from "../model/zoek-veld";
import { DocumentZoekObjectComponent } from "../zoek-object/document-zoek-object/document-zoek-object.component";
import { TaakZoekObjectComponent } from "../zoek-object/taak-zoek-object/taak-zoek-object.component";
import { ZaakZoekObjectComponent } from "../zoek-object/zaak-zoek-object/zaak-zoek-object.component";
import { ZoekenService } from "../zoeken.service";
import { DateFilterComponent } from "./filters/date-filter/date-filter.component";
import { MultiFacetFilterComponent } from "./filters/multi-facet-filter/multi-facet-filter.component";
import { ZaakBetrokkeneFilterComponent } from "./filters/zaak-betrokkene-filter/zaak-betrokkene-filter.component";

type Zoekopdracht = {
  zoekParameters: GeneratedType<"RestZoekParameters">;
  zoekVeld: ZoekVeld;
  trefwoorden: string;
};

const GEEN_RESULTAAT: ZoekResultaat<
  GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
> = {
  resultaten: [],
  totaal: 0,
  filters: {},
  foutmelding: "",
};

@Component({
  selector: "zac-zoeken",
  templateUrl: "./zoek.component.html",
  styleUrls: ["./zoek.component.less"],
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    BagZoekComponent,
    BedrijfZoekComponent,
    DateFilterComponent,
    DocumentZoekObjectComponent,
    KeyValuePipe,
    NgClass,
    MatDividerModule,
    MatFormField,
    MatIcon,
    MatIconButton,
    MatInput,
    MatLabel,
    MatOption,
    MatPaginatorModule,
    MatProgressBar,
    MatSelect,
    MatSuffix,
    MultiFacetFilterComponent,
    PersoonZoekComponent,
    ReactiveFormsModule,
    TaakZoekObjectComponent,
    TranslatePipe,
    ZaakBetrokkeneFilterComponent,
    ZaakZoekObjectComponent,
  ],
})
export class ZoekComponent implements AfterViewInit, OnDestroy {
  private readonly zoekenService = inject(ZoekenService);
  protected readonly utilService = inject(UtilService);
  private readonly policyService = inject(PolicyService);

  protected readonly brpRechtenQuery = injectQuery(() =>
    this.policyService.readBrpRechten(),
  );

  protected readonly zoekenSideNav = input<MatSidenav>();

  private readonly destroy$ = new Subject<void>();

  protected zoekType: ZoekType = ZoekType.ZAC;
  protected readonly ZoekType = ZoekType;
  protected readonly ZoekVeld = ZoekVeld;
  protected zoekveldControl = new FormControl<ZoekVeld>(ZoekVeld.ALLE);
  protected trefwoordenControl = new FormControl("");
  protected hasSearched = false;

  private readonly requestedZoekopdracht = signal<Zoekopdracht>({
    zoekParameters: { ...getDefaultZoekParameters(), rows: 10 },
    zoekVeld: ZoekVeld.ALLE,
    trefwoorden: "",
  });

  /**
   * The filters read their value once, when they are created. Creating them
   * again after a failed search shows the filters of the shown results.
   */
  protected readonly filterRevisie = signal(0);

  protected readonly search = injectLastSuccessfulSearch({
    requested: this.requestedZoekopdracht,
    query: ({ zoekParameters, zoekVeld, trefwoorden }) => ({
      ...this.zoekenService.list({
        ...zoekParameters,
        zoeken: { ...zoekParameters.zoeken, [zoekVeld]: trefwoorden },
      }),
      enabled: !!trefwoorden,
    }),
    onRevert: () => this.filterRevisie.update((revisie) => revisie + 1),
  });

  protected readonly zoekParameters = computed(
    () => this.search.requested().zoekParameters,
  );

  /** A search without trefwoorden shows no results, rather than the last ones. */
  private readonly zonderTrefwoorden = signal(false);

  protected readonly zoekResultaat = computed(() => {
    if (this.zonderTrefwoorden()) return GEEN_RESULTAAT;
    return (this.search.data() ?? new ZoekResultaat()) as ZoekResultaat<
      GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
    >;
  });

  /** A search that has not run yet has no `totaal`. */
  protected readonly aantalResultaten = computed(
    () => this.zoekResultaat().totaal ?? 0,
  );

  protected readonly isLoadingResults = computed(() =>
    this.search.query.isFetching(),
  );
  protected readonly slow = signal(false);

  private readonly context = computed(() =>
    this.bepaalContext(
      this.zoekResultaat(),
      this.search.params()?.zoekParameters,
    ),
  );
  protected readonly hasZaken = computed(() => this.context().hasZaken);
  protected readonly hasTaken = computed(() => this.context().hasTaken);
  protected readonly hasDocument = computed(() => this.context().hasDocument);

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

    effect((onCleanup) => {
      if (!this.isLoadingResults()) {
        this.slow.set(false);
        return;
      }
      const timeout = setTimeout(() => this.slow.set(true), 500);
      onCleanup(() => clearTimeout(timeout));
    });

    effect(() => {
      if (!this.search.data()) return;
      untracked(() => {
        this.hasSearched = true;
        this.zoekenService.hasSearched.set(true);
      });
    });
  }

  ngAfterViewInit() {
    this.zoekenSideNav()
      ?.openedStart.pipe(takeUntil(this.destroy$))
      .subscribe(() => this.zoeken());

    queueMicrotask(() => {
      if (this.zoekenSideNav()?.opened) this.zoeken();
    });
  }

  /** Searches for the first page of the results. */
  protected zoeken(zoekParameters = this.zoekParameters()) {
    this.searchPage({ ...zoekParameters, page: 0 });
  }

  protected pageChanged({ pageIndex, pageSize }: PageEvent) {
    const shownZoekopdracht = this.search.params() ?? this.search.requested();
    this.search.request({
      ...shownZoekopdracht,
      zoekParameters: {
        ...shownZoekopdracht.zoekParameters,
        page: pageIndex,
        rows: pageSize,
      },
    });
  }

  private searchPage(zoekParameters: GeneratedType<"RestZoekParameters">) {
    const trefwoorden = this.trefwoordenControl.value ?? "";
    const zoekopdracht: Zoekopdracht = {
      zoekParameters,
      zoekVeld: this.zoekveldControl.value ?? ZoekVeld.ALLE,
      trefwoorden,
    };
    this.zonderTrefwoorden.set(!trefwoorden);
    if (!trefwoorden) {
      this.search.clear();
      // A search query without trefwoorden is not enabled, and `request` would
      // search it again all the same.
      this.requestedZoekopdracht.set({
        ...zoekopdracht,
        zoekParameters: { ...zoekParameters, page: 0 },
      });
      return;
    }
    this.search.request(zoekopdracht);
  }

  private bepaalContext(
    zoekResultaat: ZoekResultaat<
      GeneratedType<"AbstractRestZoekObjectExtendsAbstractRestZoekObject">
    >,
    zoekParameters?: GeneratedType<"RestZoekParameters">,
  ) {
    const heeftType = (type: GeneratedType<"ZoekObjectType">) =>
      !!zoekResultaat.filters?.TYPE?.find(({ naam }) => naam === type)?.aantal;
    const gekozenTypen = zoekParameters?.filters?.TYPE?.values;
    const isGekozen = (type: GeneratedType<"ZoekObjectType">) =>
      !gekozenTypen?.length || gekozenTypen.includes(type);

    return {
      hasZaken: heeftType("ZAAK") && isGekozen("ZAAK"),
      hasTaken: heeftType("TAAK") && isGekozen("TAAK"),
      hasDocument: heeftType("DOCUMENT") && isGekozen("DOCUMENT"),
    };
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
    const { zoekVeld, trefwoorden } = this.search.requested();
    if (
      this.zoekveldControl.value &&
      (this.trefwoordenControl.value !== trefwoorden ||
        this.zoekveldControl.value !== zoekVeld)
    ) {
      this.zoeken();
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
    this.trefwoordenControl.setValue("");
    this.zoekveldControl.setValue(ZoekVeld.ALLE);
    this.zonderTrefwoorden.set(false);
    this.requestedZoekopdracht.set({
      zoekParameters: { ...getDefaultZoekParameters(), rows: 10 },
      zoekVeld: ZoekVeld.ALLE,
      trefwoorden: "",
    });
    this.search.clear();
    this.hasSearched = false;
  }

  protected zoekVeldChanged() {
    if (!this.zoekveldControl.value) return;
    if (!this.trefwoordenControl.value) return;
    this.zoeken();
  }

  protected dateFilterChange(
    key: string,
    value: GeneratedType<"RestDatumRange">,
  ) {
    const zoekParameters = this.zoekParameters();
    this.zoeken({
      ...zoekParameters,
      datums: { ...zoekParameters.datums, [key]: value },
    });
  }

  protected filterChanged(
    key: string,
    value?: GeneratedType<"FilterParameters">,
  ) {
    const filters = { ...this.zoekParameters().filters };
    if (value) {
      filters[key] = value;
    } else {
      delete filters[key];
    }
    this.zoeken({ ...this.zoekParameters(), filters });
  }

  protected betrokkeneActief() {
    const zoeken = this.zoekParameters().zoeken;
    return !!(
      zoeken?.ZAAK_BETROKKENEN ||
      zoeken?.ZAAK_INITIATOR ||
      zoeken?.ZAAK_BETROKKENE_BELANGHEBBENDE ||
      zoeken?.ZAAK_BETROKKENE_ADVISEUR ||
      zoeken?.ZAAK_BETROKKENE_BESLISSER ||
      zoeken?.ZAAK_BETROKKENE_KLANTCONTACTER ||
      zoeken?.ZAAK_BETROKKENE_ZAAKCOORDINATOR ||
      zoeken?.ZAAK_BETROKKENE_MEDE_INITIATOR
    );
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
