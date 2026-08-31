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
import {
  AfterViewInit,
  Component,
  DestroyRef,
  OnInit,
  ViewChild,
  inject,
} from "@angular/core";
import { takeUntilDestroyed } from "@angular/core/rxjs-interop";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog, MatDialogModule } from "@angular/material/dialog";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatSortModule, Sort } from "@angular/material/sort";
import { MatTableDataSource, MatTableModule } from "@angular/material/table";
import { RouterModule } from "@angular/router";
import { TranslateModule } from "@ngx-translate/core";
import { finalize, forkJoin } from "rxjs";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { injectMutation } from "../../shared/http/inject-mutation";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
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
  standalone: true,
  imports: [
    NgIf,
    MatSidenavModule,
    MatTableModule,
    MatSortModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatDialogModule,
    RouterModule,
    TranslateModule,
    SideNavComponent,
    ReadMoreComponent,
  ],
})
export class MailtemplatesComponent
  extends AdminComponent
  implements OnInit, AfterViewInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  private readonly destroyRef = inject(DestroyRef);
  private readonly deleteMailtemplateMutation = injectMutation(
    () => this.mailtemplateBeheerService.deleteMailtemplate(),
    {
      onSuccess: () => this.laadMailtemplates(),
    },
  );

  protected isLoadingResults = false;
  protected columns = [
    "mailTemplateNaam",
    "mail",
    "defaultMailtemplate",
    "id",
  ] as const;
  protected columnsToDisplay = [
    "expand",
    "mailTemplateNaam",
    "mail",
    "defaultMailtemplate",
    "id",
  ] as const;
  protected dataSource = new MatTableDataSource<
    GeneratedType<"RestMailtemplate">
  >();
  private mailKoppelingen: GeneratedType<"RESTMailtemplateKoppeling">[] = [];
  private filterValue = "";
  protected expandedRow: GeneratedType<"RestMailtemplate"> | null = null;

  constructor(
    private dialog: MatDialog,
    public utilService: UtilService,
    public configuratieService: ConfiguratieService,
    private mailtemplateBeheerService: MailtemplateBeheerService,
    private mailtemplateKoppelingService: MailtemplateKoppelingService,
  ) {
    super(utilService, configuratieService);
  }

  ngOnInit(): void {
    this.setupMenu("title.mailtemplates");
    this.laadMailtemplates();
  }

  protected laadMailtemplates(): void {
    this.isLoadingResults = true;
    this.utilService.setLoading(true);
    forkJoin([
      this.mailtemplateBeheerService.listMailtemplates(),
      this.mailtemplateKoppelingService.listMailtemplateKoppelingen(),
    ])
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.isLoadingResults = false;
          this.utilService.setLoading(false);
        }),
      )
      .subscribe(([mailtemplates, koppelingen]) => {
        this.dataSource.data = mailtemplates;
        this.mailKoppelingen = koppelingen;
      });
  }

  protected isDisabled(
    mailtemplate: GeneratedType<"RestMailtemplate">,
  ): boolean {
    return this.getMailtemplateKoppeling(mailtemplate) != null;
  }

  protected verwijderMailtemplate(
    mailtemplate: GeneratedType<"RestMailtemplate">,
  ): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData("msg.mailtemplate.verwijderen.bevestigen"),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.deleteMailtemplateMutation.mutate(mailtemplate.id ?? -1);
        }
      });
  }

  protected getKoppelingen(mailtemplate: GeneratedType<"RestMailtemplate">) {
    return this.mailKoppelingen.reduce((acc, koppeling) => {
      if (koppeling.mailtemplate?.id === mailtemplate.id) {
        acc.push(koppeling);
      }
      return acc;
    }, [] as GeneratedType<"RESTMailtemplateKoppeling">[]);
  }

  private getMailtemplateKoppeling(
    mailtemplate: GeneratedType<"RestMailtemplate">,
  ) {
    return this.mailKoppelingen.find(
      (koppeling) => koppeling.mailtemplate?.id === mailtemplate.id,
    );
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

  protected sortData(sort: Sort) {
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

  private compare(isAsc: boolean, a?: number | string, b?: number | string) {
    const direction = isAsc ? 1 : -1;

    if (!a || !b) return direction;
    return (a < b ? -1 : 1) * direction;
  }
}
