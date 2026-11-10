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

import {
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
  untracked,
  viewChild,
} from "@angular/core";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import {
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { rowOf } from "../../shared/utils/table-row";
import {
  VersionComponent,
  VersionLayout,
} from "../../shared/version/version.component";
import { AdminComponent } from "../admin/admin.component";
import { HealthCheckService } from "../health-check.service";

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
export class InrichtingscheckComponent
  extends AdminComponent
  implements OnInit
{
  private readonly healthCheckService = inject(HealthCheckService);

  protected readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  protected readonly versionLayout = VersionLayout;
  protected readonly rowOf = rowOf;
  protected readonly bevindingen = inrichtingscheckBevindingen;
  protected dataSource: MatTableDataSource<
    GeneratedType<"RestZaaktypeInrichtingscheck">
  > = new MatTableDataSource<GeneratedType<"RestZaaktypeInrichtingscheck">>();
  protected columnsToDisplay = [
    "valide",
    "expand",
    "zaaktypeOmschrijving",
    "zaaktypeDoel",
    "beginGeldigheid",
  ];
  protected readonly expandedRow =
    signal<GeneratedType<"RestZaaktypeInrichtingscheck"> | null>(null);
  protected readonly valideFilter = signal<ToggleSwitchOptions>(
    ToggleSwitchOptions.UNCHECKED,
  );
  private filterValue = "";
  private readonly zaaktypeInrichtingschecksQuery = injectQuery(() =>
    this.healthCheckService.listZaaktypeInrichtingschecks(),
  );
  protected readonly loadingZaaktypes = computed(() =>
    this.zaaktypeInrichtingschecksQuery.isFetching(),
  );
  protected readonly zaaktypeInrichtingschecks = computed(() =>
    this.loadingZaaktypes()
      ? []
      : (this.zaaktypeInrichtingschecksQuery.data() ?? []),
  );
  private readonly bestaatCommunicatiekanaalEformulierQuery = injectQuery(() =>
    this.healthCheckService.readBestaatCommunicatiekanaalEformulier(),
  );
  protected readonly loadingCommunicatiekanaal = computed(() =>
    this.bestaatCommunicatiekanaalEformulierQuery.isPending(),
  );
  protected readonly bestaatCommunicatiekanaalEformulier = computed(() =>
    Boolean(this.bestaatCommunicatiekanaalEformulierQuery.data()),
  );

  constructor() {
    super();

    this.dataSource.filterPredicate = (data, filter: string) => {
      if (
        this.valideFilter() === ToggleSwitchOptions.CHECKED &&
        this.needsAttention(data)
      ) {
        return false;
      }
      if (
        this.valideFilter() === ToggleSwitchOptions.UNCHECKED &&
        !this.needsAttention(data)
      ) {
        return false;
      }
      const dataString = (data.zaaktype.omschrijving + " " + data.zaaktype.doel)
        .trim()
        .toLowerCase();
      return dataString.indexOf(filter.trim().toLowerCase()) !== -1;
    };

    effect(() => {
      const zaaktypeInrichtingschecks = this.zaaktypeInrichtingschecks();
      untracked(() => this.showZaaktypes(zaaktypeInrichtingschecks));
    });
  }

  ngOnInit(): void {
    this.setupMenu("title.inrichtingscheck");
  }

  protected needsAttention(
    zaaktypeInrichtingscheck: GeneratedType<"RestZaaktypeInrichtingscheck">,
  ) {
    return (
      !zaaktypeInrichtingscheck.isValide ||
      Boolean(zaaktypeInrichtingscheck.hasWaarschuwingen)
    );
  }

  protected rowStateClass(
    zaaktypeInrichtingscheck: GeneratedType<"RestZaaktypeInrichtingscheck">,
  ) {
    if (!zaaktypeInrichtingscheck.isValide) return "error";
    return zaaktypeInrichtingscheck.hasWaarschuwingen ? "warning" : "ok";
  }

  protected applyFilter(event?: Event) {
    if (event) {
      this.filterValue = (event.target as HTMLInputElement).value
        .trim()
        .toLowerCase();
    }
    // MatTableDataSource skips the filterPredicate, and so the valide filter, for an empty filter
    this.dataSource.filter = " " + this.filterValue;
  }

  private showZaaktypes(
    zaaktypeInrichtingschecks: GeneratedType<"RestZaaktypeInrichtingscheck">[],
  ) {
    this.dataSource.data = [...zaaktypeInrichtingschecks].sort((a, b) =>
      (a.zaaktype.omschrijving ?? "").localeCompare(
        b.zaaktype.omschrijving ?? "",
      ),
    );
    this.applyFilter();
  }

  protected sortData(sort: Sort) {
    if (!sort.active || sort.direction === "") {
      return;
    }

    this.dataSource.data = this.dataSource.data.slice().sort((a, b) => {
      const isAsc = sort.direction === "asc";
      switch (sort.active) {
        case "zaaktypeOmschrijving":
          return this.compare(
            a.zaaktype.omschrijving ?? "",
            b.zaaktype.omschrijving ?? "",
            isAsc,
          );
        case "zaaktypeDoel":
          return this.compare(
            a.zaaktype.doel ?? "",
            b.zaaktype.doel ?? "",
            isAsc,
          );
        case "beginGeldigheid":
          return this.compare(
            a.zaaktype.beginGeldigheid ?? "",
            b.zaaktype.beginGeldigheid ?? "",
            isAsc,
          );
        case "valide":
          return this.compare(a.isValide ?? false, b.isValide ?? false, isAsc);
        default:
          return 0;
      }
    });
  }

  private compare(
    a: number | string | boolean,
    b: number | string | boolean,
    isAsc: boolean,
  ) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
  }
}
