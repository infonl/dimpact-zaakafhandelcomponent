/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgClass, NgFor, NgIf } from "@angular/common";
import {
  Component,
  EventEmitter,
  inject,
  input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialog } from "@angular/material/dialog";
import { MatIconModule } from "@angular/material/icon";
import { MatMenuModule } from "@angular/material/menu";
import { MatTooltipModule } from "@angular/material/tooltip";
import { TranslateModule } from "@ngx-translate/core";
import { Subscription } from "rxjs";
import { injectMutation } from "../../shared/http/inject-mutation";
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
    NgIf,
    NgFor,
    NgClass,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatTooltipModule,
    TranslateModule,
    ReadMoreComponent,
  ],
})
export class ZoekopdrachtComponent implements OnInit, OnDestroy {
  readonly werklijst = input.required<GeneratedType<"Werklijst">>();
  readonly zoekFilters = input.required<ZoekFilters>();
  @Output() zoekopdracht = new EventEmitter<
    GeneratedType<"RESTZoekopdracht">
  >();
  readonly filtersChanged = input.required<EventEmitter<void>>();

  protected zoekopdrachten: GeneratedType<"RESTZoekopdracht">[] = [];
  protected actieveZoekopdracht: GeneratedType<"RESTZoekopdracht"> | null =
    null;
  protected actieveFilters = false;
  private filtersChangedSubscription$!: Subscription;
  private readonly gebruikersvoorkeurenService = inject(
    GebruikersvoorkeurenService,
  );
  private readonly dialog = inject(MatDialog);
  private readonly deleteZoekopdrachtMutation = injectMutation(
    () => this.gebruikersvoorkeurenService.deleteZoekOpdrachten(),
    {
      onSuccess: () => this.loadZoekopdrachten(),
    },
  );
  private readonly removeZoekopdrachtActiefMutation = injectMutation(() =>
    this.gebruikersvoorkeurenService.removeZoekopdrachtActief(),
  );

  ngOnDestroy() {
    this.filtersChangedSubscription$.unsubscribe();
  }

  ngOnInit() {
    this.filtersChangedSubscription$ = this.filtersChanged().subscribe(() => {
      this.clearActief();
    });
    this.loadZoekopdrachten();
  }

  protected saveSearch() {
    const dialogRef = this.dialog.open(ZoekopdrachtSaveDialogComponent, {
      data: {
        zoekopdrachten: this.zoekopdrachten,
        lijstID: this.werklijst(),
        zoekopdracht: this.zoekFilters(),
      },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadZoekopdrachten();
      }
    });
  }

  protected setActief(zoekopdracht: GeneratedType<"RESTZoekopdracht">) {
    this.actieveZoekopdracht = zoekopdracht;
    this.actieveFilters = true;
    this.zoekopdracht.emit(this.actieveZoekopdracht);
    this.gebruikersvoorkeurenService
      .setZoekopdrachtActief(this.actieveZoekopdracht)
      .subscribe();
  }

  protected deleteZoekopdracht(
    $event: MouseEvent,
    zoekopdracht: GeneratedType<"RESTZoekopdracht">,
  ) {
    $event.stopPropagation();
    this.deleteZoekopdrachtMutation.mutate(zoekopdracht.id!);
  }

  protected clearActief(emit?: boolean) {
    this.actieveZoekopdracht = null;
    this.removeZoekopdrachtActiefMutation.mutate(this.werklijst());
    if (emit && this.actieveZoekopdracht) {
      this.actieveFilters = false;
      this.zoekopdracht.emit(this.actieveZoekopdracht);
    } else {
      this.actieveFilters = this.heeftActieveFilters();
    }
  }

  private loadZoekopdrachten() {
    this.gebruikersvoorkeurenService
      .listZoekOpdrachten(this.werklijst())
      .subscribe((zoekopdrachten) => {
        this.zoekopdrachten = zoekopdrachten;
        this.actieveZoekopdracht = zoekopdrachten.find((z) => z.actief) ?? null;
        this.actieveFilters = this.heeftActieveFilters();

        if (!this.actieveZoekopdracht) return;
        this.zoekopdracht.emit(this.actieveZoekopdracht);
      });
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
