/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectChange, MatSelectModule } from "@angular/material/select";
import {
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { TypedMatCellDefDirective } from "../../shared/directives/typed-mat-cell-def.directive";
import { ClientMatcher } from "../../shared/dynamic-table/filter/clientMatcher";
import { DatumPipe } from "../../shared/pipes/datum.pipe";
import { EmptyPipe } from "../../shared/pipes/empty.pipe";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { DateRangeFilterComponent } from "../../shared/table-zoek-filters/date-range-filter/date-range-filter.component";
import { ToggleFilterComponent } from "../../shared/table-zoek-filters/toggle-filter/toggle-filter.component";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

@Component({
  templateUrl: "./parameters.component.html",
  styleUrls: ["./parameters.component.less"],
  standalone: true,
  imports: [
    RouterLink,
    TranslatePipe,
    MatSidenavModule,
    MatCardModule,
    MatTableModule,
    TypedMatCellDefDirective,
    MatSortModule,
    MatFormFieldModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    SideNavComponent,
    ToggleFilterComponent,
    DateRangeFilterComponent,
    ReadMoreComponent,
    DatumPipe,
    EmptyPipe,
  ],
})
export class ParametersComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  private readonly zaakafhandelParametersService = inject(
    ZaakafhandelParametersService,
  );

  protected readonly sideNavContainer =
    viewChild.required<MatSidenavContainer>("sideNavContainer");
  private readonly parametersSort =
    viewChild.required<MatSort>("parametersSort");

  protected readonly filterParameters = signal(
    new ZaakafhandelParametersListParameters("valide", "asc"),
  );
  protected parameters = new MatTableDataSource<
    GeneratedType<"RestZaaktypeConfigurationOverzicht">
  >();

  private readonly parametersQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listZaakafhandelParameters(),
  );
  protected readonly loading = computed(() => this.parametersQuery.isPending());
  protected readonly hasNoParameters = computed(
    () => !this.parametersQuery.data()?.length,
  );

  private storedParameterFilters = "parameterFilters";

  protected readonly zaaktypes = computed(() =>
    this.utilService.getUniqueItemsList(
      this.parametersQuery.data() ?? [],
      "zaaktype",
      "identificatie",
      "omschrijving",
    ),
  );
  protected readonly processDefinitions = computed(() =>
    this.utilService.getUniqueItemsList(
      this.parametersQuery.data() ?? [],
      "processDefinition",
      "key",
      "naam",
    ),
  );

  constructor() {
    super();
    this.parameters.filterPredicate = (data, filter) => {
      let match = true;

      const parsedFilter = JSON.parse(
        filter,
      ) as ZaakafhandelParametersListParameters;

      if (parsedFilter.valide !== ToggleSwitchOptions.INDETERMINATE) {
        match =
          match &&
          ClientMatcher.matchBoolean(
            Boolean(data.isValide),
            parsedFilter.valide === ToggleSwitchOptions.CHECKED,
          );
      }

      if (parsedFilter.geldig !== ToggleSwitchOptions.INDETERMINATE) {
        match =
          match &&
          ClientMatcher.matchBoolean(
            Boolean(data.zaaktype.isNuGeldig),
            parsedFilter.geldig === ToggleSwitchOptions.CHECKED,
          );
      }

      if (parsedFilter.zaaktype) {
        match =
          match &&
          ClientMatcher.matchObject(
            "identificatie",
            data.zaaktype,
            parsedFilter.zaaktype as GeneratedType<"RestZaaktypeBeheerOverzicht">,
          );
      }

      if (parsedFilter.processDefinition) {
        match =
          match &&
          ClientMatcher.matchObject(
            "key",
            data.processDefinition,
            parsedFilter.processDefinition,
          );
      }
      if (
        parsedFilter.beginGeldigheid.van !== null ||
        parsedFilter.beginGeldigheid.tot !== null
      ) {
        match =
          match &&
          ClientMatcher.matchDatum(
            data.zaaktype.beginGeldigheid ?? "",
            parsedFilter.beginGeldigheid,
          );
      }

      if (
        parsedFilter.eindeGeldigheid.van !== null ||
        parsedFilter.eindeGeldigheid.tot !== null
      ) {
        match =
          match &&
          ClientMatcher.matchDatum(
            data.zaaktype.eindeGeldigheid ?? "",
            parsedFilter.eindeGeldigheid,
          );
      }

      return match;
    };
    effect(() => {
      this.parameters.data = this.parametersQuery.data() ?? [];
    });
  }

  ngOnInit() {
    this.setupMenu("title.parameters");
    this.filterParameters.set(
      SessionStorageUtil.getItem(
        this.storedParameterFilters,
        new ZaakafhandelParametersListParameters("valide", "asc"),
      ),
    );
    this.applyFilter();
  }

  ngAfterViewInit() {
    this.parameters.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "omschrijving":
          return String(item.zaaktype.omschrijving);
        case "model":
          return String(item.processDefinition?.naam);
        case "valide":
          return Number(item.isValide);
        case "geldig":
          return Number(item.zaaktype.isNuGeldig);
        case "beginGeldigheid":
          return String(item.zaaktype.beginGeldigheid);
        case "eindeGeldigheid":
          return String(item.zaaktype.eindeGeldigheid);
        default:
          return String(item[property as keyof typeof item]);
      }
    };

    this.parameters.sort = this.parametersSort();
  }

  protected applyFilter(options?: {
    event: ToggleSwitchOptions | MatSelectChange | string | number | undefined;
    filter?: keyof ZaakafhandelParametersListParameters;
  }) {
    if (options?.filter) {
      const { event, filter } = options;
      this.setFilter(filter, typeof event === "object" ? event?.value : event);
    }

    this.parameters.filter = JSON.stringify(this.filterParameters());
    SessionStorageUtil.setItem(
      this.storedParameterFilters,
      this.filterParameters(),
    );
  }

  protected applyDateRangeFilter(
    filter: "beginGeldigheid" | "eindeGeldigheid",
    range: GeneratedType<"RestDatumRange">,
  ) {
    this.setFilter(filter, range);
    this.applyFilter();
  }

  private setFilter(
    filter: keyof ZaakafhandelParametersListParameters,
    value: unknown,
  ) {
    this.filterParameters.update((filterParameters) => ({
      ...filterParameters,
      [filter]: value,
    }));
  }

  protected compareZaaktype = (
    zaaktype1?: GeneratedType<"RestZaaktypeBeheerOverzicht">,
    zaaktype2?: GeneratedType<"RestZaaktypeBeheerOverzicht">,
  ) => {
    return zaaktype1?.identificatie === zaaktype2?.identificatie;
  };

  protected compareProcessDefinition = (
    processDefinition1?: GeneratedType<"RestProcessDefinitionOverzicht">,
    processDefinition2?: GeneratedType<"RestProcessDefinitionOverzicht">,
  ) => {
    return processDefinition1?.key === processDefinition2?.key;
  };
}
