/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, inject, OnInit, ViewChild } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatCardModule } from "@angular/material/card";
import { MatDialog } from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { MatToolbarModule } from "@angular/material/toolbar";
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
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
import { ReferentieTabelEditDialogComponent } from "./referentie-tabel-edit-dialog/referentie-tabel-edit-dialog.component";
import { ReferentieTabelItemComponent } from "./referentie-tabel-item/referentie-tabel-item.component";
import { ReferentieTabelValueDialogComponent } from "./referentie-tabel-value-dialog/referentie-tabel-value-dialog.component";

/**
 * Experimental accordion-based reference table administration, offered next to
 * the existing screens so the two can be compared side by side. Each table is a
 * collapsible row; its values are lazily loaded when the row is expanded.
 */
@Component({
  templateUrl: "./referentie-tabellen-v2.component.html",
  styleUrls: ["./referentie-tabellen-v2.component.less"],
  standalone: true,
  imports: [
    MatSidenavModule,
    MatCardModule,
    MatButtonModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatTooltipModule,
    ReactiveFormsModule,
    TranslateModule,
    SideNavComponent,
    ReferentieTabelItemComponent,
  ],
})
export class ReferentieTabellenV2Component
  extends AdminComponent
  implements OnInit
{
  @ViewChild("sideNavContainer")
  protected sideNavContainer!: MatSidenavContainer;
  @ViewChild("menuSidenav") protected menuSidenav!: MatSidenav;

  protected tabellen: GeneratedType<"RestReferenceTable">[] = [];
  protected loading = false;
  protected showCreateForm = false;
  protected expandedId: number | null = null;
  protected loadingTabelId: number | null = null;

  protected readonly form = new FormGroup({
    code: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required],
    }),
    naam: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  private readonly loadedTabellen = new Map<
    number,
    GeneratedType<"RestReferenceTable">
  >();

  private readonly service = inject(ReferentieTabelService);
  private readonly dialog = inject(MatDialog);

  constructor() {
    super(inject(UtilService), inject(ConfiguratieService));
  }

  ngOnInit() {
    this.setupMenu("title.referentietabellen.v2");
    this.laadReferentieTabellen();
  }

  protected laadReferentieTabellen() {
    this.loading = true;
    this.service.listReferentieTabellen().subscribe((tabellen) => {
      this.tabellen = tabellen;
      this.loading = false;
    });
  }

  protected openCreateForm() {
    this.showCreateForm = true;
  }

  protected closeCreateForm() {
    this.showCreateForm = false;
    this.form.reset();
  }

  protected addReferentieTabel() {
    if (this.form.invalid) {
      return;
    }
    const { code, naam } = this.form.getRawValue();
    this.service
      .createReferentieTabel({ code, naam, systeem: false, waarden: [] })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.referentietabel.toegevoegd", {
          tabel: code,
        });
        this.closeCreateForm();
        this.laadReferentieTabellen();
      });
  }

  protected verwijderReferentieTabel(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.tabel.verwijderen.bevestigen",
            args: { tabel: tabel.code },
          },
          this.service.deleteReferentieTabel(tabel.id),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.tabel.verwijderen.uitgevoerd", {
            tabel: tabel.code,
          });
          this.laadReferentieTabellen();
        }
      });
  }

  protected toggle(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    if (this.expandedId === tabel.id) {
      this.expandedId = null;
      return;
    }
    this.expandedId = tabel.id;
    this.loadTabel(tabel.id);
  }

  protected editReferentieTabel(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    // Load the full table (with its values) so the name update does not wipe them.
    this.service.readReferentieTabel(tabel.id).subscribe((geladenTabel) => {
      this.dialog
        .open(ReferentieTabelEditDialogComponent, {
          data: geladenTabel,
          width: "500px",
        })
        .afterClosed()
        .subscribe((naam?: string) => {
          if (naam) {
            this.saveReferentieTabelNaam(geladenTabel, naam);
          }
        });
    });
  }

  private saveReferentieTabelNaam(
    tabel: GeneratedType<"RestReferenceTable">,
    naam: string,
  ) {
    if (tabel.id == null) {
      return;
    }
    this.service
      .updateReferentieTabel(tabel.id, {
        code: tabel.code,
        naam,
        waarden: tabel.waarden ?? [],
      })
      .subscribe(() => {
        this.utilService.openSnackbar("msg.referentietabel.gewijzigd", {
          tabel: tabel.code,
        });
        this.loadedTabellen.delete(tabel.id!);
        this.laadReferentieTabellen();
      });
  }

  protected addReferentieTabelWaarde(
    tabel: GeneratedType<"RestReferenceTable">,
  ) {
    this.dialog
      .open(ReferentieTabelValueDialogComponent, {
        data: {
          naam: "",
          titel: "referentietabel.waarde.toevoegen.titel",
          icoon: "add_circle",
        },
        width: "500px",
      })
      .afterClosed()
      .subscribe((naam?: string) => {
        if (!naam) {
          return;
        }
        const waarden = [...(tabel.waarden ?? []), { naam }];
        this.persistWaarden(
          tabel,
          waarden,
          "msg.referentietabel.waarde.toegevoegd",
          { waarde: naam },
        );
      });
  }

  protected editReferentieTabelWaarde(
    tabel: GeneratedType<"RestReferenceTable">,
    waarde: GeneratedType<"RestReferenceTableValue">,
  ) {
    if (tabel.id == null) {
      return;
    }
    this.dialog
      .open(ReferentieTabelValueDialogComponent, {
        data: {
          naam: waarde.naam,
          titel: "referentietabel.waarde.wijzigen.titel",
          icoon: "edit",
        },
        width: "500px",
      })
      .afterClosed()
      .subscribe((naam?: string) => {
        if (!naam) {
          return;
        }
        const waarden = (tabel.waarden ?? []).map((current) =>
          current.id === waarde.id ? { ...current, naam } : current,
        );
        this.persistWaarden(
          tabel,
          waarden,
          "msg.referentietabel.waarde.gewijzigd",
          { waarde: naam },
        );
      });
  }

  protected deleteReferentieTabelWaarde(
    tabel: GeneratedType<"RestReferenceTable">,
    waarde: GeneratedType<"RestReferenceTableValue">,
  ) {
    if (tabel.id == null) {
      return;
    }
    const waarden = (tabel.waarden ?? []).filter(
      (current) => current.id !== waarde.id,
    );
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData(
          {
            key: "msg.referentietabel.waarde.verwijderen.bevestigen",
            args: { waarde: waarde.naam },
          },
          this.service.updateReferentieTabel(tabel.id, {
            code: tabel.code,
            naam: tabel.naam,
            waarden,
          }),
        ),
      })
      .afterClosed()
      .subscribe((result) => {
        if (result) {
          this.utilService.openSnackbar("msg.referentietabel.waarde.verwijderd", {
            waarde: waarde.naam,
          });
          this.refreshTabel(tabel.id!);
        }
      });
  }

  private persistWaarden(
    tabel: GeneratedType<"RestReferenceTable">,
    waarden: GeneratedType<"RestReferenceTableValue">[],
    snackbarKey: string,
    snackbarArgs: Record<string, string>,
  ) {
    if (tabel.id == null) {
      return;
    }
    this.service
      .updateReferentieTabel(tabel.id, {
        code: tabel.code,
        naam: tabel.naam,
        waarden,
      })
      .subscribe(() => {
        this.utilService.openSnackbar(snackbarKey, snackbarArgs);
        this.refreshTabel(tabel.id!);
      });
  }

  private refreshTabel(id: number) {
    this.service.readReferentieTabel(id).subscribe((geladenTabel) => {
      this.loadedTabellen.set(id, geladenTabel);
    });
    this.laadReferentieTabellen();
  }

  private loadTabel(id: number) {
    if (this.loadedTabellen.has(id)) {
      return;
    }
    this.loadingTabelId = id;
    this.service.readReferentieTabel(id).subscribe((geladenTabel) => {
      this.loadedTabellen.set(id, geladenTabel);
      this.loadingTabelId = null;
    });
  }

  protected isLoadingWaarden(tabel: GeneratedType<"RestReferenceTable">) {
    return tabel.id != null && this.loadingTabelId === tabel.id;
  }

  protected getLoadedTabel(tabel: GeneratedType<"RestReferenceTable">) {
    return (tabel.id != null && this.loadedTabellen.get(tabel.id)) || null;
  }
}
