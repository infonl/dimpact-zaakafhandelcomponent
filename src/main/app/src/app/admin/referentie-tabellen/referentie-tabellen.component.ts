/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  effect,
  inject,
  OnInit,
  signal,
  ViewChild,
  viewChildren,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { ConfiguratieService } from "../../configuratie/configuratie.service";
import { UtilService } from "../../core/service/util.service";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../../shared/confirm-dialog/confirm-dialog.component";
import { SideNavComponent } from "../../shared/side-nav/side-nav.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AdminComponent } from "../admin/admin.component";
import { ReferentieTabelService } from "../referentie-tabel.service";
import { ReferentieTabelCreateDialogComponent } from "./referentie-tabel-create-dialog/referentie-tabel-create-dialog.component";
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog/referentie-tabel-edit-dialog.component";
import { ReferentieTabelItemComponent } from "./referentie-tabel-item/referentie-tabel-item.component";
import { ReferentieTabelRowDirective } from "./referentie-tabel-row.directive";

@Component({
  templateUrl: "./referentie-tabellen.component.html",
  styleUrls: ["./referentie-tabellen.component.less"],
  standalone: true,
  imports: [
    MatSidenavModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule,
    SideNavComponent,
    ReferentieTabelItemComponent,
    ReferentieTabelRowDirective,
  ],
})
export class ReferentieTabellenComponent
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  protected readonly expandedId = signal<number | null>(null);

  private readonly scrollToId = signal<number | null>(null);
  private readonly rows = viewChildren(ReferentieTabelRowDirective);

  private readonly service = inject(ReferentieTabelService);
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);

  protected readonly tabellenQuery = injectQuery(() =>
    this.service.listReferentieTabellenQuery(),
  );

  protected readonly tabellen = computed(() => this.tabellenQuery.data() ?? []);

  protected readonly tabelDetailQuery = injectQuery(() => {
    const expandedId = this.expandedId();
    return {
      ...this.service.readReferentieTabelQuery(expandedId ?? -1),
      enabled: expandedId != null,
    };
  });

  constructor() {
    super(inject(UtilService), inject(ConfiguratieService));

    // Scroll a freshly created table into view once its row has rendered (the
    // list refetch that adds it is async, so this waits for the row to appear).
    effect(() => {
      const id = this.scrollToId();
      if (id == null) {
        return;
      }
      const row = this.rows().find((candidate) => candidate.tabelId() === id);
      if (!row) {
        return;
      }
      row.element.nativeElement.scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
      this.scrollToId.set(null);
    });
  }

  ngOnInit() {
    this.setupMenu("title.referentietabellen");
  }

  protected toggle(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    this.expandedId.set(this.expandedId() === tabel.id ? null : tabel.id);
  }

  protected isLoadingWaarden(tabel: GeneratedType<"RestReferenceTable">) {
    return this.expandedId() === tabel.id && this.tabelDetailQuery.isLoading();
  }

  protected getLoadedTabel(tabel: GeneratedType<"RestReferenceTable">) {
    return this.expandedId() === tabel.id
      ? (this.tabelDetailQuery.data() ?? null)
      : null;
  }

  protected openCreateDialog() {
    this.dialog
      .open(ReferentieTabelCreateDialogComponent, {
        width: "500px",
        autoFocus: "input:not([disabled])",
      })
      .afterClosed()
      .subscribe((createdId?: number) => {
        if (createdId != null) {
          this.expandedId.set(createdId);
          this.scrollToId.set(createdId);
        }
      });
  }

  protected editReferentieTabel(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    // Load the full table first: the update PUT replaces the entire value list.
    void this.queryClient
      .fetchQuery(this.service.readReferentieTabelQuery(tabel.id))
      .then((loaded) => {
        this.dialog.open(ReferentieTabelEditDialogComponent, {
          data: loaded,
          width: "500px",
          autoFocus: "input:not([disabled])",
        });
      });
  }

  protected verwijderReferentieTabel(
    tabel: GeneratedType<"RestReferenceTable">,
  ) {
    if (tabel.id == null) {
      return;
    }
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.tabel.verwijderen-bevestigen",
            args: { tabel: tabel.code },
          },
          this.service.deleteReferentieTabelWithRefresh(tabel.id),
        ),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.utilService.openSnackbar("msg.tabel.verwijderen.uitgevoerd", {
          tabel: tabel.code,
        });
        if (this.expandedId() === tabel.id) {
          this.expandedId.set(null);
        }
      });
  }
}
