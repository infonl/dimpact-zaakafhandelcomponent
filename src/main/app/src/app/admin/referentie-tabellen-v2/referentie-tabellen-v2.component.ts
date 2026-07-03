/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  computed,
  inject,
  OnInit,
  signal,
  ViewChild,
} from "@angular/core";
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
import {
  MatSidenav,
  MatSidenavContainer,
  MatSidenavModule,
} from "@angular/material/sidenav";
import { MatToolbarModule } from "@angular/material/toolbar";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  injectQuery,
  QueryClient,
} from "@tanstack/angular-query-experimental";
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

  protected readonly showCreateForm = signal(false);
  protected readonly expandedId = signal<number | null>(null);

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

  private readonly service = inject(ReferentieTabelService);
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);

  protected readonly tabellenQuery = injectQuery(() =>
    this.service.listReferentieTabellenQuery(),
  );

  protected readonly tabellen = computed(
    () => this.tabellenQuery.data() ?? [],
  );

  protected readonly tabelDetailQuery = injectQuery(() => ({
    ...this.service.readReferentieTabelQuery(this.expandedId()!),
    enabled: this.expandedId() != null,
  }));

  private readonly createMutation = injectMutation(() => ({
    ...this.service.createReferentieTabelMutation(),
    onSuccess: () => this.invalidateTabellen(),
  }));

  private readonly updateMutation = injectMutation(() => ({
    mutationFn: (variables: {
      id: number;
      body: GeneratedType<"RestReferenceTableUpdate">;
    }) => this.service.updateReferentieTabelAsync(variables.id, variables.body),
    onSuccess: (_data, variables) => {
      this.invalidateTabellen();
      void this.queryClient.invalidateQueries({
        queryKey: this.service.readReferentieTabelQuery(variables.id).queryKey,
      });
    },
  }));

  private readonly deleteMutation = injectMutation(() => ({
    mutationFn: (id: number) => this.service.deleteReferentieTabelAsync(id),
    onSuccess: () => this.invalidateTabellen(),
  }));

  constructor() {
    super(inject(UtilService), inject(ConfiguratieService));
  }

  ngOnInit() {
    this.setupMenu("title.referentietabellen.v2");
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

  protected openCreateForm() {
    this.showCreateForm.set(true);
  }

  protected closeCreateForm() {
    this.showCreateForm.set(false);
    this.form.reset();
  }

  protected addReferentieTabel() {
    if (this.form.invalid) {
      return;
    }
    const { code, naam } = this.form.getRawValue();
    this.createMutation.mutate(
      { code, naam, systeem: false, waarden: [] },
      {
        onSuccess: () => {
          this.utilService.openSnackbar("msg.referentietabel.toegevoegd", {
            tabel: code,
          });
          this.closeCreateForm();
        },
      },
    );
  }

  protected editReferentieTabel(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    // Fetch the full table (with its values) so the name update does not wipe them.
    void this.queryClient
      .fetchQuery(this.service.readReferentieTabelQuery(tabel.id))
      .then((loaded) => {
        this.dialog
          .open(ReferentieTabelEditDialogComponent, {
            data: loaded,
            width: "500px",
          })
          .afterClosed()
          .subscribe((naam?: string) => {
            if (!naam || loaded.id == null) {
              return;
            }
            this.updateMutation.mutate(
              {
                id: loaded.id,
                body: { code: loaded.code, naam, waarden: loaded.waarden ?? [] },
              },
              {
                onSuccess: () =>
                  this.utilService.openSnackbar("msg.referentietabel.gewijzigd", {
                    tabel: loaded.code,
                  }),
              },
            );
          });
      });
  }

  protected verwijderReferentieTabel(tabel: GeneratedType<"RestReferenceTable">) {
    if (tabel.id == null) {
      return;
    }
    this.dialog
      .open(ConfirmDialogComponent, {
        data: new ConfirmDialogData({
          key: "msg.tabel.verwijderen.bevestigen",
          args: { tabel: tabel.code },
        }),
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (!confirmed) {
          return;
        }
        this.deleteMutation.mutate(tabel.id!, {
          onSuccess: () => {
            this.utilService.openSnackbar("msg.tabel.verwijderen.uitgevoerd", {
              tabel: tabel.code,
            });
            if (this.expandedId() === tabel.id) {
              this.expandedId.set(null);
            }
          },
        });
      });
  }

  private invalidateTabellen() {
    void this.queryClient.invalidateQueries({
      queryKey: this.service.listReferentieTabellenQuery().queryKey,
    });
  }
}
