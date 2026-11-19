/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  animate,
  state,
  style,
  transition,
  trigger,
} from "@angular/animations";

import { Component, computed, inject, signal, viewChild } from "@angular/core";
import { toObservable, toSignal } from "@angular/core/rxjs-interop";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatPaginatorModule, PageEvent } from "@angular/material/paginator";
import { MatProgressBarModule } from "@angular/material/progress-bar";
import {
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableModule } from "@angular/material/table";
import { TranslatePipe } from "@ngx-translate/core";
import {
  injectQuery,
  keepPreviousData,
} from "@tanstack/angular-query-experimental";
import { debounceTime, distinctUntilChanged } from "rxjs";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  VersionComponent,
  VersionLayout,
} from "../../shared/version/version.component";
import { AdminComponent } from "../admin/admin.component";
import {
  HealthCheckService,
  InrichtingscheckParameters,
} from "../health-check.service";

export const INRICHTINGSCHECK_PAGE_SIZE = 25;
export const INRICHTINGSCHECK_FILTER_DEBOUNCE_MS = 300;

const STATUS_BY_TOGGLE: Record<
  ToggleSwitchOptions,
  InrichtingscheckParameters["status"]
> = {
  [ToggleSwitchOptions.UNCHECKED]: "AANDACHT_NODIG",
  [ToggleSwitchOptions.CHECKED]: "IN_ORDE",
  [ToggleSwitchOptions.INDETERMINATE]: null,
};

const SORT_FIELD_BY_COLUMN = {
  omschrijving: "OMSCHRIJVING",
  doel: "DOEL",
  beginGeldigheid: "BEGIN_GELDIGHEID",
  valide: "VALIDE",
} as const satisfies Record<
  string,
  NonNullable<InrichtingscheckParameters["sortField"]>
>;

const isSortColumn = (
  column: string,
): column is keyof typeof SORT_FIELD_BY_COLUMN =>
  Object.hasOwn(SORT_FIELD_BY_COLUMN, column);

type ZaaktypeInrichtingscheck = GeneratedType<"RestZaaktypeInrichtingscheck">;

type InrichtingscheckBevinding = {
  categorie: string;
  melding: string;
  parameters?: Record<string, string>;
};

export function inrichtingscheckBevindingen(
  check: ZaaktypeInrichtingscheck,
): InrichtingscheckBevinding[] {
  const bevindingen: InrichtingscheckBevinding[] = [];
  const add = (
    isOfToepassing: boolean,
    categorie: string,
    melding: string,
    parameters?: Record<string, string>,
  ) => {
    if (isOfToepassing) bevindingen.push({ categorie, melding, parameters });
  };
  const prefix = "health-check.zaaktype.";

  add(
    !check.isZaakafhandelParametersValide,
    "zaakafhandelparameters",
    `${prefix}zaakafhandel-parameters-ongeldig`,
  );
  add(
    !check.isStatustypeIntakeAanwezig,
    "statustype",
    `${prefix}geen-statustype-intake-aanwezig`,
  );
  add(
    !check.isStatustypeInBehandelingAanwezig,
    "statustype",
    `${prefix}geen-statustype-in-behandeling-aanwezig`,
  );
  add(
    !check.isStatustypeHeropendAanwezig,
    "statustype",
    `${prefix}geen-statustype-heropend-aanwezig`,
  );
  add(
    !check.isStatustypeAanvullendeInformatieVereist,
    "statustype",
    `${prefix}geen-statustype-aanvullende-informatie-vereist`,
  );
  add(
    !check.isStatustypeAfgerondAanwezig,
    "statustype",
    `${prefix}geen-statustype-afgerond-aanwezig`,
  );
  add(
    Boolean(check.isStatustypeAfgerondAanwezig) &&
      !check.isStatustypeAfgerondLaatsteVolgnummer,
    "statustype",
    `${prefix}statustype-afgerond-laatste-volgnummer`,
  );
  add(
    !check.isResultaattypeAanwezig,
    "resultaattype",
    `${prefix}geen-resultaattype-aanwezig`,
  );
  add(
    !check.aantalInitiatorroltypen,
    "rol",
    `${prefix}geen-rol-initiator-aanwezig`,
  );
  add(
    !check.aantalBehandelaarroltypen,
    "rol",
    `${prefix}geen-rol-behandelaar-aanwezig`,
  );
  add(
    (check.aantalInitiatorroltypen ?? 0) > 1,
    "rol",
    `${prefix}te-veel-rol-initiator-aanwezig`,
  );
  add(
    (check.aantalBehandelaarroltypen ?? 0) > 1,
    "rol",
    `${prefix}te-veel-rol-behandelaar-aanwezig`,
  );
  add(!check.isRolOverigeAanwezig, "rol", `${prefix}geen-rol-overige-aanwezig`);
  add(
    !check.isInformatieobjecttypeEmailAanwezig,
    "informatieobjecttype",
    `${prefix}geen-informatieobjecttype-email-aanwezig`,
  );
  add(
    Boolean(check.resultaattypesMetVerplichtBesluit?.length) &&
      !check.isBesluittypeAanwezig,
    "besluittype",
    `${prefix}geen-besluittype-aanwezig`,
    {
      resultaattypes: (check.resultaattypesMetVerplichtBesluit ?? []).join(
        ", ",
      ),
    },
  );
  add(
    Boolean(check.resultaattypesMetNietOndersteundeBrondatum?.length),
    "resultaattype",
    `${prefix}niet-ondersteunde-brondatum`,
    {
      resultaattypes: (
        check.resultaattypesMetNietOndersteundeBrondatum ?? []
      ).join(", "),
    },
  );
  add(
    !check.isBrpInstellingenCorrect,
    "brp-protocollering",
    `${prefix}onjuiste-brp-headerwaarde`,
  );
  add(
    Boolean(check.isZaakspecifiekeAutorisatieEigenschapAanwezig) &&
      !check.isZaakspecifiekeAutorisatieRoltypeAanwezig,
    "rol",
    `${prefix}zaakspecifieke-autorisatie.roltype-ontbreekt`,
  );
  add(
    Boolean(check.isZaakspecifiekeAutorisatieRoltypeAanwezig) &&
      !check.isZaakspecifiekeAutorisatieEigenschapAanwezig,
    "eigenschap",
    `${prefix}zaakspecifieke-autorisatie.eigenschap-ontbreekt`,
  );

  return bevindingen;
}

@Component({
  templateUrl: "./inrichtingscheck.component.html",
  styleUrls: ["./inrichtingscheck.component.less"],
  animations: [
    trigger("detailExpand", [
      state("collapsed", style({ height: "0px", minHeight: "0" })),
      state("expanded", style({ height: "*" })),
      transition(
        "expanded <=> collapsed",
        animate("225ms cubic-bezier(0.4, 0.0, 0.2, 1)"),
      ),
    ]),
  ],
  standalone: true,
  imports: [
    MatSidenavModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
    TypedMatCellDefDirective,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    TranslatePipe,
    DatumPipe,
    SideNavComponent,
    ToggleFilterComponent,
    VersionComponent,
    ReadMoreComponent,
  ],
})
export class InrichtingscheckComponent extends AdminComponent {
  private readonly healthCheckService = inject(HealthCheckService);

  protected readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  protected readonly versionLayout = VersionLayout;
  protected readonly bevindingen = inrichtingscheckBevindingen;
  protected readonly pageSize = INRICHTINGSCHECK_PAGE_SIZE;
  protected readonly columnsToDisplay = [
    "valide",
    "expand",
    "zaaktypeOmschrijving",
    "zaaktypeDoel",
    "beginGeldigheid",
  ];
  protected readonly expandedZaaktypeUuid = signal<string | null>(null);
  protected readonly valideFilter = signal<ToggleSwitchOptions>(
    ToggleSwitchOptions.UNCHECKED,
  );
  protected readonly zoektermFilter = signal("");
  protected readonly sort = signal<Sort>({ active: "", direction: "" });
  protected readonly pageIndex = signal(0);

  private readonly debouncedZoektermFilter = toSignal(
    toObservable(this.zoektermFilter).pipe(
      debounceTime(INRICHTINGSCHECK_FILTER_DEBOUNCE_MS),
      distinctUntilChanged(),
    ),
    { initialValue: "" },
  );

  private readonly parameters = computed<InrichtingscheckParameters>(() => {
    const { active, direction } = this.sort();
    return {
      page: this.pageIndex(),
      rows: INRICHTINGSCHECK_PAGE_SIZE,
      zoekterm: this.debouncedZoektermFilter().trim() || null,
      status: STATUS_BY_TOGGLE[this.valideFilter()],
      sortField:
        direction && isSortColumn(active)
          ? SORT_FIELD_BY_COLUMN[active]
          : "OMSCHRIJVING",
      sortOrder: direction === "desc" ? "DESC" : "ASC",
    };
  });

  private readonly zaaktypeInrichtingschecksQuery = injectQuery(() => ({
    ...this.healthCheckService.listZaaktypeInrichtingschecks(this.parameters()),
    placeholderData: keepPreviousData,
  }));
  protected readonly loadingZaaktypes = computed(() =>
    this.zaaktypeInrichtingschecksQuery.isFetching(),
  );
  protected readonly zaaktypeInrichtingschecks = computed(
    () => this.zaaktypeInrichtingschecksQuery.data()?.resultaten ?? [],
  );
  protected readonly totaal = computed(
    () => this.zaaktypeInrichtingschecksQuery.data()?.totaal ?? 0,
  );

  private readonly referentietabellenQuery = injectQuery(() =>
    this.healthCheckService.readReferentietabellenInrichtingscheck(),
  );
  protected readonly loadingCommunicatiekanaal = computed(() =>
    this.referentietabellenQuery.isPending(),
  );
  protected readonly bestaatCommunicatiekanaalEformulier = computed(() =>
    Boolean(
      this.referentietabellenQuery.data()
        ?.isCommunicatiekanaalEformulierAanwezig,
    ),
  );

  constructor() {
    super();
    this.setupMenu("title.inrichtingscheck");
  }

  protected needsAttention(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
    return (
      !zaaktypeInrichtingscheck.isValide ||
      Boolean(zaaktypeInrichtingscheck.hasWaarschuwingen)
    );
  }

  protected isExpanded(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
    return (
      this.expandedZaaktypeUuid() === zaaktypeInrichtingscheck.zaaktype.uuid
    );
  }

  protected toggleExpanded(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
    if (!this.needsAttention(zaaktypeInrichtingscheck)) return;
    this.expandedZaaktypeUuid.set(
      this.isExpanded(zaaktypeInrichtingscheck)
        ? null
        : zaaktypeInrichtingscheck.zaaktype.uuid,
    );
  }

  protected rowStateClass(zaaktypeInrichtingscheck: ZaaktypeInrichtingscheck) {
    if (!zaaktypeInrichtingscheck.isValide) return "error";
    return zaaktypeInrichtingscheck.hasWaarschuwingen ? "warning" : "ok";
  }

  protected zoektermFilterChanged(event: Event) {
    this.zoektermFilter.set((event.target as HTMLInputElement).value);
    this.pageIndex.set(0);
  }

  protected valideFilterChanged(valideFilter: ToggleSwitchOptions) {
    this.valideFilter.set(valideFilter);
    this.pageIndex.set(0);
  }

  protected sortChanged(sort: Sort) {
    this.sort.set(sort);
    this.pageIndex.set(0);
  }

  protected pageChanged(pageEvent: PageEvent) {
    this.pageIndex.set(pageEvent.pageIndex);
  }
}
