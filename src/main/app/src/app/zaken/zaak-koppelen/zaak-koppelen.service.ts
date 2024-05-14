/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable, OnDestroy } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Router } from "@angular/router";
import { Subject, takeUntil } from "rxjs";
import {
  ActionBarAction,
  ActionEntityType,
} from "../../core/actionbar/model/action-bar-action";
import { UtilService } from "../../core/service/util.service";
import { ViewResourceUtil } from "../../locatie/view-resource.util";
import { ActionIcon } from "../../shared/edit/action-icon";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { Zaak } from "../model/zaak";
import { ZaakKoppelDialogGegevens } from "../model/zaak-koppel-dialog-gegevens";
import { ZaakKoppelenDialogComponent } from "./zaak-koppelen-dialog.component";

@Injectable({
  providedIn: "root",
})
export class ZaakKoppelenService implements OnDestroy {
  destroy$ = new Subject<void>();
  constructor(
    private utilService: UtilService,
    private router: Router,
    private dialog: MatDialog,
  ) {}
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  addTeKoppelenZaak(zaak: Zaak): void {
    if (!this.isReedsTeKoppelen(zaak)) {
      this._koppelenZaak(zaak);
    }
  }

  isReedsTeKoppelen(zaak: Zaak): boolean {
    const teKoppelenZaken = SessionStorageUtil.getItem(
      "teKoppelenZaken",
      [],
    ) as Zaak[];
    return (
      teKoppelenZaken.find((_zaak) => _zaak.uuid === zaak.uuid) !== undefined
    );
  }

  appInit() {
    const zaken = SessionStorageUtil.getItem("teKoppelenZaken", []) as Zaak[];
    zaken.forEach((zaak) => {
      this._koppelenZaak(zaak, true);
    });
  }

  private _koppelenZaak(zaak: Zaak, onInit?: boolean) {
    const dismiss: Subject<void> = new Subject<void>();
    dismiss
      .asObservable()
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.deleteTeKoppelenZaak(zaak);
      });
    const editAction = new Subject<string>();
    editAction
      .asObservable()
      .pipe(takeUntil(this.destroy$))
      .subscribe((url) => {
        const nieuwZaakID = url.split("/").pop();
        this.openDialog(zaak, nieuwZaakID);
      });
    const teKoppelenZaken = SessionStorageUtil.getItem(
      "teKoppelenZaken",
      [],
    ) as Zaak[];
    teKoppelenZaken.push(zaak);
    if (!onInit) {
      SessionStorageUtil.setItem("teKoppelenZaken", teKoppelenZaken);
    }
    const action: ActionBarAction = new ActionBarAction(
      zaak.identificatie,
      ActionEntityType.ZAAK,
      zaak.identificatie,
      new ActionIcon("link", "actie.zaak.koppelen", editAction),
      dismiss,
      () => this.isDisabled(zaak.identificatie),
    );
    this.utilService.addAction(action);
  }

  private openDialog(zaak: Zaak, nieuwZaakID: string) {
    const zaakKoppelGegevens = new ZaakKoppelDialogGegevens();
    zaakKoppelGegevens.bronZaakUuid = zaak.uuid;
    zaakKoppelGegevens.doelZaakIdentificatie = nieuwZaakID;

    this.dialog.open(ZaakKoppelenDialogComponent, {
      data: zaakKoppelGegevens,
    });
  }

  private deleteTeKoppelenZaak(zaak: Zaak) {
    const zaken = SessionStorageUtil.getItem("teKoppelenZaken", []) as Zaak[];
    SessionStorageUtil.setItem(
      "teKoppelenZaken",
      zaken.filter((_zaak) => _zaak.uuid !== zaak.uuid),
    );
  }

  /**
   * @return null als toegestaan, string met reden indien disabled;
   */
  private isDisabled(zaakIdentificatie: string): string | null {
    return ViewResourceUtil.actieveZaak &&
      ViewResourceUtil.actieveZaak.identificatie !== zaakIdentificatie
      ? null
      : "actie.zaak.koppelen.disabled";
  }
}
