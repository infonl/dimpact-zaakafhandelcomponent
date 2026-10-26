/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass } from "@angular/common";
import {
  Component,
  computed,
  effect,
  EventEmitter,
  inject,
  input,
  OnDestroy,
  OnInit,
  output,
  signal,
  untracked,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslatePipe } from "@ngx-translate/core";
import { injectQuery, QueryClient } from "@tanstack/angular-query-experimental";
import { Subscription } from "rxjs";
import { injectMutation } from "../../shared/http/inject-mutation";
import { StaleTimes } from "../../shared/http/zac-query-client";
import { ReadMoreComponent } from "../../shared/read-more/read-more.component";
import { GeneratedType } from "../../shared/utils/generated-types";
import { hasActiveSearchFilters } from "../../zoeken/model/zoek-parameters";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";
import { ZoekopdrachtSaveDialogComponent } from "../zoekopdracht-save-dialog/zoekopdracht-save-dialog.component";
import { ZoekFilters } from "./zoekfilters.model";

@Component({
  selector: "zac-zoekopdracht",
  templateUrl: "./zoekopdracht.component.html",
  styleUrls: ["./zoekopdracht.component.less"],
  standalone: true,
  imports: [
    NgClass,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslatePipe,
    ReadMoreComponent,
  ],
})
export class ZoekopdrachtComponent implements OnInit, OnDestroy {
  readonly werklijst = input.required<GeneratedType<"Werklijst">>();
  readonly zoekFilters = input.required<ZoekFilters>();
  readonly zoekopdracht = output<GeneratedType<"RestZoekopdracht">>();
  readonly filtersChanged = input.required<EventEmitter<void>>();

  protected readonly actieveZoekopdracht =
    signal<GeneratedType<"RestZoekopdracht"> | null>(null);
  protected readonly actieveFilters = signal(false);
  private filtersChangedSubscription$!: Subscription;
  private readonly gebruikersvoorkeurenService = inject(
    GebruikersvoorkeurenService,
  );
  private readonly dialog = inject(MatDialog);
  private readonly queryClient = inject(QueryClient);
  private readonly zoekopdrachtenQuery = injectQuery(() => ({
    ...this.gebruikersvoorkeurenService.listZoekOpdrachten(this.werklijst()),
    staleTime: StaleTimes.Instant,
  }));
  protected readonly zoekopdrachten = computed(
    () => this.zoekopdrachtenQuery.data() ?? [],
  );
  private readonly deleteZoekopdrachtMutation = injectMutation(
    () => this.gebruikersvoorkeurenService.deleteZoekOpdrachten(),
    {
      onSuccess: () => this.invalidateZoekopdrachten(),
    },
  );
  private readonly removeZoekopdrachtActiefMutation = injectMutation(() =>
    this.gebruikersvoorkeurenService.removeZoekopdrachtActief(),
  );
  private readonly setZoekopdrachtActiefMutation = injectMutation(() =>
    this.gebruikersvoorkeurenService.setZoekopdrachtActief(),
  );

  constructor() {
    effect(() => {
      const zoekopdrachten = this.zoekopdrachtenQuery.data();
      if (
        !zoekopdrachten ||
        this.zoekopdrachtenQuery.isFetching() ||
        this.zoekopdrachtenQuery.isError()
      )
        return;
      untracked(() => this.applyZoekopdrachten(zoekopdrachten));
    });
  }

  ngOnDestroy() {
    this.filtersChangedSubscription$.unsubscribe();
  }

  ngOnInit() {
    this.filtersChangedSubscription$ = this.filtersChanged().subscribe(() => {
      this.clearActief();
    });
  }

  protected saveSearch() {
    const dialogRef = this.dialog.open(ZoekopdrachtSaveDialogComponent, {
      data: {
        zoekopdrachten: this.zoekopdrachten(),
        lijstID: this.werklijst(),
        zoekopdracht: this.zoekFilters(),
      },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        void this.invalidateZoekopdrachten();
      }
    });
  }

  protected setActief(zoekopdracht: GeneratedType<"RestZoekopdracht">) {
    this.actieveZoekopdracht.set(zoekopdracht);
    this.actieveFilters.set(true);
    this.zoekopdracht.emit(zoekopdracht);
    this.setZoekopdrachtActiefMutation.mutate(zoekopdracht);
  }

  protected deleteZoekopdracht(
    $event: MouseEvent,
    zoekopdracht: GeneratedType<"RestZoekopdracht">,
  ) {
    $event.stopPropagation();
    this.deleteZoekopdrachtMutation.mutate(zoekopdracht.id!);
  }

  protected clearActief(emit?: boolean) {
    this.actieveZoekopdracht.set(null);
    this.removeZoekopdrachtActiefMutation.mutate(this.werklijst());
    const actieveZoekopdracht = this.actieveZoekopdracht();
    if (emit && actieveZoekopdracht) {
      this.actieveFilters.set(false);
      this.zoekopdracht.emit(actieveZoekopdracht);
    } else {
      this.actieveFilters.set(this.heeftActieveFilters());
    }
  }

  private invalidateZoekopdrachten() {
    return this.queryClient.invalidateQueries({
      queryKey: this.gebruikersvoorkeurenService.listZoekOpdrachten(
        this.werklijst(),
      ).queryKey,
    });
  }

  private applyZoekopdrachten(
    zoekopdrachten: GeneratedType<"RestZoekopdracht">[],
  ) {
    const actieveZoekopdracht =
      zoekopdrachten.find((zoekopdracht) => zoekopdracht.actief) ?? null;
    this.actieveZoekopdracht.set(actieveZoekopdracht);
    this.actieveFilters.set(this.heeftActieveFilters());

    if (!actieveZoekopdracht) return;
    this.zoekopdracht.emit(actieveZoekopdracht);
  }

  private heeftActieveFilters(): boolean {
    const zoekFilters = this.zoekFilters();
    switch (zoekFilters.filtersType) {
      case "ZoekParameters":
        return hasActiveSearchFilters(zoekFilters);
      case "DetachedDocumentListParameters":
        if (zoekFilters.zaakID) return true;
        if (zoekFilters.ontkoppeldDoor) return true;
        if (zoekFilters.ontkoppeldOp?.van) return true;
        if (zoekFilters.ontkoppeldOp?.tot) return true;
        if (zoekFilters.creatiedatum?.van) return true;
        if (zoekFilters.creatiedatum?.tot) return true;
        if (zoekFilters.titel) return true;
        if (zoekFilters.reden) return true;
        return false;
      case "InboxDocumentListParameters":
        if (zoekFilters.identificatie) return true;
        if (zoekFilters.creatiedatum?.van) return true;
        if (zoekFilters.creatiedatum?.tot) return true;
        if (zoekFilters.titel) return true;
        return false;
      default:
        return false;
    }
  }
}
