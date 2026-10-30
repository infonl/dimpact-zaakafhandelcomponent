/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  OnInit,
  viewChild,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatSelectChange, MatSelectModule } from "@angular/material/select";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSort, MatSortModule } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterLink } from "@angular/router";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
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
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    RouterLink,
    TranslatePipe,
    MatSidenavModule,
    MatCardModule,
    MatTableModule,
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
  protected readonly menuSidenav =
    viewChild.required<MatSidenav>("menuSidenav");
  private readonly parametersSort =
    viewChild.required<MatSort>("parametersSort");

  protected filterParameters!: ZaakafhandelParametersListParameters;
  protected parameters = new MatTableDataSource<
    GeneratedType<"RestZaaktypeConfiguration">
  >();

  private readonly parametersQuery = injectQuery(() =>
    this.zaakafhandelParametersService.listZaakafhandelParameters(),
  );
  protected readonly loading = computed(() => this.parametersQuery.isPending());

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
    effect(() => {
      this.parameters.data = this.parametersQuery.data() ?? [];
    });
  }

  ngOnInit() {
    this.setupMenu("title.parameters");
    this.filterParameters = SessionStorageUtil.getItem(
      this.storedParameterFilters,
      new ZaakafhandelParametersListParameters("valide", "asc"),
    );
    this.applyFilter();
  }

  ngAfterViewInit() {
    super.ngAfterViewInit();
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
            parsedFilter.zaaktype as GeneratedType<"RestZaaktypeOverzicht">,
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
  }

  protected applyFilter(options?: {
    event: ToggleSwitchOptions | MatSelectChange | string | number | undefined;
    filter?: keyof ZaakafhandelParametersListParameters;
  }) {
    if (options) {
      const value =
        typeof options.event === "object"
          ? options.event?.value
          : options.event;

      if (options.filter) {
        this.filterParameters[options.filter] = value as never;
      }
    }

    this.parameters.filter = JSON.stringify(this.filterParameters);
    SessionStorageUtil.setItem(
      this.storedParameterFilters,
      this.filterParameters,
    );
  }

  protected compareZaaktype = (
    zaaktype1?: GeneratedType<"RestZaaktype">,
    zaaktype2?: GeneratedType<"RestZaaktype">,
  ) => {
    return zaaktype1?.identificatie === zaaktype2?.identificatie;
  };

  protected compareProcessDefinition = (
    processDefinition1?: GeneratedType<"RestProcessDefinition">,
    processDefinition2?: GeneratedType<"RestProcessDefinition">,
  ) => {
    return processDefinition1?.key === processDefinition2?.key;
  };
}
