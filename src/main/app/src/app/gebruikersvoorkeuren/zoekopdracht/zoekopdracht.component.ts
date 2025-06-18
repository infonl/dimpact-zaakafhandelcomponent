/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
} from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Subscription } from "rxjs";
import { heeftActieveZoekFilters } from "../../zoeken/model/zoek-parameters";
import { GebruikersvoorkeurenService } from "../gebruikersvoorkeuren.service";
import { Werklijst } from "../model/werklijst";
import { Zoekopdracht } from "../model/zoekopdracht";
import { ZoekopdrachtSaveDialogComponent } from "../zoekopdracht-save-dialog/zoekopdracht-save-dialog.component";
import { ZoekFilters } from "./zoekfilters.model";

@Component({
  selector: "zac-zoekopdracht",
  templateUrl: "./zoekopdracht.component.html",
  styleUrls: ["./zoekopdracht.component.less"],
})
export class ZoekopdrachtComponent implements OnInit, OnDestroy {
  @Input({ required: true }) werklijst!: Werklijst;
  @Input({ required: true }) zoekFilters!: ZoekFilters;
  @Output() zoekopdracht = new EventEmitter<Zoekopdracht>();
  @Input({ required: true }) filtersChanged!: EventEmitter<void>;

  zoekopdrachten: Zoekopdracht[] = [];
  actieveZoekopdracht: Zoekopdracht | null = null;
  actieveFilters = false;
  filtersChangedSubscription$!: Subscription;

  constructor(
    private gebruikersvoorkeurenService: GebruikersvoorkeurenService,
    private dialog: MatDialog,
  ) {}

  ngOnDestroy(): void {
    this.filtersChangedSubscription$.unsubscribe();
  }

  ngOnInit(): void {
    this.filtersChangedSubscription$ = this.filtersChanged.subscribe(() => {
      this.clearActief();
    });
    this.loadZoekopdrachten();
  }

  saveSearch(): void {
    const dialogRef = this.dialog.open(ZoekopdrachtSaveDialogComponent, {
      data: {
        zoekopdrachten: this.zoekopdrachten,
        lijstID: this.werklijst,
        zoekopdracht: this.zoekFilters,
      },
    });
    dialogRef.afterClosed().subscribe((result) => {
      if (result) {
        this.loadZoekopdrachten();
      }
    });
  }

  setActief(zoekopdracht: Zoekopdracht): void {
    this.actieveZoekopdracht = zoekopdracht;
    this.actieveFilters = true;
    this.zoekopdracht.emit(this.actieveZoekopdracht);
    this.gebruikersvoorkeurenService
      .setZoekopdrachtActief(this.actieveZoekopdracht)
      .subscribe();
  }

  deleteZoekopdracht($event: MouseEvent, zoekopdracht: Zoekopdracht): void {
    $event.stopPropagation();
    this.gebruikersvoorkeurenService
      .deleteZoekOpdrachten(zoekopdracht.id)
      .subscribe(() => {
        this.loadZoekopdrachten();
      });
  }

  clearActief(emit?: boolean): void {
    this.actieveZoekopdracht = null;
    this.gebruikersvoorkeurenService
      .removeZoekopdrachtActief(this.werklijst)
      .subscribe();
    if (emit && this.actieveZoekopdracht) {
      this.actieveFilters = false;
      this.zoekopdracht.emit(this.actieveZoekopdracht);
    } else {
      this.actieveFilters = this.heeftActieveFilters();
    }
  }

  private loadZoekopdrachten(): void {
    this.gebruikersvoorkeurenService
      .listZoekOpdrachten(this.werklijst)
      .subscribe((zoekopdrachten) => {
        this.zoekopdrachten = zoekopdrachten;
        this.actieveZoekopdracht = zoekopdrachten.find((z) => z.actief) ?? null;
        this.actieveFilters = this.heeftActieveFilters();

        if (!this.actieveZoekopdracht) return;
        this.zoekopdracht.emit(this.actieveZoekopdracht);
      });
  }

  private heeftActieveFilters(): boolean {
    switch (this.zoekFilters.filtersType) {
      case "ZoekParameters":
        return heeftActieveZoekFilters(this.zoekFilters);
      case "OntkoppeldDocumentListParameters":
        if (this.zoekFilters.zaakID) return true;
        if (this.zoekFilters.ontkoppeldDoor) return true;
        if (this.zoekFilters.ontkoppeldOp?.van) return true;
        if (this.zoekFilters.ontkoppeldOp?.tot) return true;
        if (this.zoekFilters.creatiedatum?.van) return true;
        if (this.zoekFilters.creatiedatum?.tot) return true;
        if (this.zoekFilters.titel) return true;
        if (this.zoekFilters.reden) return true;
        return false;
      case "InboxDocumentListParameters":
        if (this.zoekFilters.identificatie) return true;
        if (this.zoekFilters.creatiedatum?.van) return true;
        if (this.zoekFilters.creatiedatum?.tot) return true;
        if (this.zoekFilters.titel) return true;
        return false;
      default:
        return false;
    }
  }
}
