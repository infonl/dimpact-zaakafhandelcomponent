/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos, 2024-2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MatSelectChange } from "@angular/material/select";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { MatSort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import { ClientMatcher } from "../../shared/dynamic-table/filter/clientMatcher";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { ToggleSwitchOptions } from "../../shared/table-zoek-filters/toggle-filter/toggle-switch-options";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ZaakafhandelParametersService } from "../zaakafhandel-parameters.service";
import { ZaakafhandelParametersListParameters } from "./zaakafhandel-parameters-list-parameters";

@Component({
  templateUrl: "./parameters.component.html",
  styleUrls: ["./parameters.component.less"],
})
export class ParametersComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;
  @ViewChild("parametersSort") parametersSort!: MatSort;

  filterParameters!: ZaakafhandelParametersListParameters;
  parameters = new MatTableDataSource<
    GeneratedType<"RestZaakafhandelParameters">
  >();
  loading = false;

  private storedParameterFilters = "parameterFilters";

  zaaktypes: GeneratedType<"RestZaaktypeOverzicht">[] = [];
  caseDefinitions: GeneratedType<"RESTCaseDefinition">[] = [];

  constructor(
    public readonly utilService: UtilService,
    public readonly configuratieService: ConfiguratieService,
    private readonly zaakafhandelParametersService: ZaakafhandelParametersService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit() {
    this.setupMenu("title.parameters");
    this.getZaakafhandelParameters();
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
          return String(item.caseDefinition?.naam);
        case "geldig":
          return Number(item.zaaktype.nuGeldig);
        case "beginGeldigheid":
          return String(item.zaaktype.beginGeldigheid);
        case "eindeGeldigheid":
          return String(item.zaaktype.eindeGeldigheid);
        default:
          return String(item[property as keyof typeof item]);
      }
    };

    this.parameters.sort = this.parametersSort;
    this.parameters.filterPredicate = (data, filter) => {
      let match = true;

      const parsedFilter = JSON.parse(
        filter,
      ) as ZaakafhandelParametersListParameters;

      if (parsedFilter.valide !== ToggleSwitchOptions.INDETERMINATE) {
        match =
          match &&
          ClientMatcher.matchBoolean(
            Boolean(data.valide),
            parsedFilter.valide === ToggleSwitchOptions.CHECKED,
          );
      }

      if (parsedFilter.geldig !== ToggleSwitchOptions.INDETERMINATE) {
        match =
          match &&
          ClientMatcher.matchBoolean(
            Boolean(data.zaaktype.nuGeldig),
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

      if (parsedFilter.caseDefinition) {
        match =
          match &&
          ClientMatcher.matchObject(
            "key",
            data.caseDefinition,
            parsedFilter.caseDefinition,
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

  applyFilter(options?: {
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

  private getZaakafhandelParameters() {
    this.loading = true;
    this.zaakafhandelParametersService
      .listZaakafhandelParameters()
      .subscribe((parameters) => {
        this.loading = false;
        this.parameters.data = parameters;
        this.zaaktypes = this.utilService.getUniqueItemsList(
          parameters,
          "zaaktype",
          "identificatie",
          "omschrijving",
        );
        this.caseDefinitions = this.utilService.getUniqueItemsList(
          parameters,
          "caseDefinition",
          "key",
          "naam",
        );
      });
  }

  compareZaaktype = (
    zaaktype1?: GeneratedType<"RestZaaktype">,
    zaaktype2?: GeneratedType<"RestZaaktype">,
  ) => {
    return zaaktype1?.identificatie === zaaktype2?.identificatie;
  };
  compareCaseDefinition = (
    caseDefinition1?: GeneratedType<"RESTCaseDefinition">,
    caseDefinition2?: GeneratedType<"RESTCaseDefinition">,
  ) => {
    return caseDefinition1?.key === caseDefinition2?.key;
  };
}
