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
import { NgIf } from "@angular/common";
import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatExpansionModule } from "@angular/material/expansion";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSort, MatSortModule, Sort } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { TranslateModule } from "@ngx-translate/core";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
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
import { HealthCheckService } from "../health-check.service";

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
    NgIf,
    MatSidenavModule,
    MatCardModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    TranslateModule,
    DatumPipe,
    SideNavComponent,
    ToggleFilterComponent,
    VersionComponent,
    ReadMoreComponent,
  ],
})
export class InrichtingscheckComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;
  @ViewChild(MatSort) private sort!: MatSort;

  protected readonly versionLayout = VersionLayout;
  protected dataSource: MatTableDataSource<
    GeneratedType<"RESTZaaktypeInrichtingscheck">
  > = new MatTableDataSource<GeneratedType<"RESTZaaktypeInrichtingscheck">>();
  protected loadingZaaktypes = true;
  protected loadingCommunicatiekanaal = true;
  protected columnsToDisplay = [
    "valide",
    "expand",
    "zaaktypeOmschrijving",
    "zaaktypeDoel",
    "beginGeldigheid",
  ];
  protected expandedRow: GeneratedType<"RESTZaaktypeInrichtingscheck"> | null =
    null;
  protected valideFilter: ToggleSwitchOptions = ToggleSwitchOptions.UNCHECKED;
  private filterValue = "";
  protected bestaatCommunicatiekanaalEformulier = false;
  protected ztcCacheTime = "";

  constructor(
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private healtCheckService: HealthCheckService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.inrichtingscheck");
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
    this.dataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case "zaaktypeOmschrijving":
          return item.zaaktype.omschrijving?.toLowerCase() ?? "";
        case "doel":
          return item.zaaktype.doel ?? "";
        case "beginGeldigheid":
          return item.zaaktype.beginGeldigheid ?? "";
        default:
          return "";
      }
    };
    this.dataSource.sort = this.sort;
    this.dataSource.filterPredicate = (data, filter: string) => {
      if (this.valideFilter === ToggleSwitchOptions.CHECKED && !data.valide) {
        return false;
      }
      if (this.valideFilter === ToggleSwitchOptions.UNCHECKED && data.valide) {
        return false;
      }
      const dataString = (data.zaaktype.omschrijving + " " + data.zaaktype.doel)
        .trim()
        .toLowerCase();
      return dataString.indexOf(filter.trim().toLowerCase()) !== -1;
    };

    this.healtCheckService
      .readBestaatCommunicatiekanaalEformulier()
      .subscribe((value) => {
        this.loadingCommunicatiekanaal = false;
        this.bestaatCommunicatiekanaalEformulier = value;
      });

    this.checkZaaktypes();
    this.healtCheckService.readZTCCacheTime().subscribe((value) => {
      this.ztcCacheTime = value;
    });
  }

  protected applyFilter(event?: Event) {
    if (event) {
      const filterValue = (event.target as HTMLInputElement).value;
      this.filterValue = filterValue.trim().toLowerCase();
      this.dataSource.filter = filterValue;
    } else {
      // toggleSwitch
      this.dataSource.filter = " " + this.filterValue;
    }
  }

  protected clearZTCCache($event: MouseEvent) {
    $event.stopPropagation();
    this.healtCheckService.clearZTCCaches().subscribe((value) => {
      this.ztcCacheTime = value;
      this.checkZaaktypes();
    });
  }

  private checkZaaktypes() {
    this.loadingZaaktypes = true;
    this.dataSource.data = [];
    this.healtCheckService
      .listZaaktypeInrichtingschecks()
      .subscribe((value) => {
        this.loadingZaaktypes = false;
        this.dataSource.data = value.sort((a, b) =>
          (a.zaaktype.omschrijving ?? "").localeCompare(
            b.zaaktype.omschrijving ?? "",
          ),
        );
        this.applyFilter();
      });
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
        case "doel":
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
          return this.compare(a.valide ?? false, b.valide ?? false, isAsc);
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
