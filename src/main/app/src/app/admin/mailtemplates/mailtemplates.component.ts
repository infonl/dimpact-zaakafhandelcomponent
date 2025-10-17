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
import { AfterViewInit, Component, OnInit, ViewChild } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { MatSidenav, MatSidenavContainer } from "@angular/material/sidenav";
import { Sort } from "@angular/material/sort";
import { MatTableDataSource } from "@angular/material/table";
import { TranslateService } from "@ngx-translate/core";
import { forkJoin } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { MailtemplateBeheerService } from "../mailtemplate-beheer.service";
import { MailtemplateKoppelingService } from "../mailtemplate-koppeling.service";

@Component({
  templateUrl: "./mailtemplates.component.html",
  styleUrls: ["./mailtemplates.component.less"],
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
})
export class MailtemplatesComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  @ViewChild("sideNavContainer") sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") menuSidenav!: MatSidenav;

  isLoadingResults = false;
  columns = ["mailTemplateNaam", "mail", "defaultMailtemplate", "id"] as const;
  columnsToDisplay = [
    "expand",
    "mailTemplateNaam",
    "mail",
    "defaultMailtemplate",
    "id",
  ] as const;
  dataSource = new MatTableDataSource<GeneratedType<"RESTMailtemplate">>();
  mailKoppelingen: GeneratedType<"RESTMailtemplateKoppeling">[] = [];
  filterValue = "";
  expandedRow: GeneratedType<"RESTMailtemplate"> | null = null;

  constructor(
    public dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private mailtemplateBeheerService: MailtemplateBeheerService,
    private translate: TranslateService,
    private mailtemplateKoppelingService: MailtemplateKoppelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.mailtemplates");
    this.laadMailtemplates();
  }

  laadMailtemplates(): void {
    this.isLoadingResults = true;
    forkJoin([
      this.mailtemplateBeheerService.listMailtemplates(),
      this.mailtemplateKoppelingService.listMailtemplateKoppelingen(),
    ]).subscribe(([mailtemplates, koppelingen]) => {
      this.dataSource.data = mailtemplates;
      this.mailKoppelingen = koppelingen;
      this.isLoadingResults = false;
    });
  }

  isDisabled(mailtemplate: GeneratedType<"RESTMailtemplate">): boolean {
    return this.getMailtemplateKoppeling(mailtemplate) != null;
  }

  verwijderMailtemplate(mailtemplate: GeneratedType<"RESTMailtemplate">): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          "msg.mailtemplate.verwijderen.bevestigen",
          this.mailtemplateBeheerService.deleteMailtemplate(mailtemplate.id!),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar(
            "msg.mailtemplate.verwijderen.uitgevoerd",
          );
          this.laadMailtemplates();
        }
      });
  }

  getKoppelingen(mailtemplate: GeneratedType<"RESTMailtemplate">) {
    return this.mailKoppelingen.reduce((acc, koppeling) => {
      if (koppeling.mailtemplate?.id === mailtemplate.id) {
        acc.push(koppeling);
      }
      return acc;
    }, [] as GeneratedType<"RESTMailtemplateKoppeling">[]);
  }

  private getMailtemplateKoppeling(
    mailtemplate: GeneratedType<"RESTMailtemplate">,
  ) {
    return this.mailKoppelingen.find(
      (koppeling) => koppeling.mailtemplate?.id === mailtemplate.id,
    );
  }

  applyFilter(event?: Event) {
    if (event) {
      const filterValue = (event.target as HTMLInputElement).value;
      this.filterValue = filterValue.trim().toLowerCase();
      this.dataSource.filter = filterValue;
    } else {
      // toggleSwitch
      this.dataSource.filter = " " + this.filterValue;
    }
  }

  sortData(sort: Sort) {
    if (!sort.active || sort.direction === "") {
      return;
    }

    this.dataSource.data = this.dataSource.data.slice().sort((a, b) => {
      const isAsc = sort.direction === "asc";
      switch (sort.active) {
        case "mail":
          return this.compare(isAsc, a.mail, b.mail);
        case "mailTemplateNaam":
          return this.compare(isAsc, a.mailTemplateNaam, b.mailTemplateNaam);
        default:
          return 0;
      }
    });
  }

  compare(isAsc: boolean, a?: number | string, b?: number | string) {
    const direction = isAsc ? 1 : -1;

    if (!a || !b) return direction;
    return (a < b ? -1 : 1) * direction;
  }
}
