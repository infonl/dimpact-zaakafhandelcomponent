/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Subject } from "rxjs";
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
export class ZaakKoppelenService {
  private koppelZaakCallback: Function | null;

  constructor(
    private utilService: UtilService,
    private dialog: MatDialog,
  ) {}

  addTeKoppelenZaak(zaak: Zaak, callback?: Function): void {
    if (!this.isReedsTeKoppelen(zaak)) {
      this._koppelenZaak(zaak);
      this.koppelZaakCallback = callback;
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
    dismiss.asObservable().subscribe(() => {
      this.koppelZaakCallback?.();
      this.deleteTeKoppelenZaak(zaak);
    });
    const editAction = new Subject<string>();
    editAction.asObservable().subscribe((url) => {
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

    this.dialog
      .open(ZaakKoppelenDialogComponent, {
        data: zaakKoppelGegevens,
      })
      .afterClosed()
      .subscribe(() => {
        this.koppelZaakCallback?.();
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
