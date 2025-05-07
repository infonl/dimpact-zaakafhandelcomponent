/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { Subject } from "rxjs";
import { UtilService } from "../../core/service/util.service";
import { ViewResourceUtil } from "../../locatie/view-resource.util";
import { SessionStorageUtil } from "../../shared/storage/session-storage.util";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZaakKoppelDialogGegevens } from "../model/zaak-koppel-dialog-gegevens";

@Injectable({
  providedIn: "root",
})
export class ZaakKoppelenService {
  constructor(
    private utilService: UtilService,
    private dialog: MatDialog,
  ) {}

  addTeKoppelenZaak(zaak: GeneratedType<"RestZaak">): void {
    if (!this.isReedsTeKoppelen(zaak)) {
      this._koppelenZaak(zaak);
    }
  }

  isReedsTeKoppelen(zaak: GeneratedType<"RestZaak">): boolean {
    const teKoppelenZaken = SessionStorageUtil.getItem<
      GeneratedType<"RestZaak">[]
    >("teKoppelenZaken", []);
    return (
      teKoppelenZaken.find((_zaak) => _zaak.uuid === zaak.uuid) !== undefined
    );
  }

  appInit() {
    const zaken = SessionStorageUtil.getItem<GeneratedType<"RestZaak">[]>(
      "teKoppelenZaken",
      [],
    );
    zaken.forEach((zaak) => {
      this._koppelenZaak(zaak, true);
    });
  }

  private _koppelenZaak(zaak: GeneratedType<"RestZaak">, onInit?: boolean) {
    const dismiss: Subject<void> = new Subject<void>();
    dismiss.asObservable().subscribe(() => {
      this.deleteTeKoppelenZaak(zaak);
    });
    const editAction = new Subject<string>();
    editAction.asObservable().subscribe((url) => {
      const nieuwZaakID = url.split("/").pop();
      if (!nieuwZaakID) return;
      this.openDialog(zaak, nieuwZaakID);
    });
    const teKoppelenZaken = SessionStorageUtil.getItem<
      GeneratedType<"RestZaak">[]
    >("teKoppelenZaken", []);
    teKoppelenZaken.push(zaak);
    if (!onInit) {
      SessionStorageUtil.setItem("teKoppelenZaken", teKoppelenZaken);
    }

    // Prevent the OLD handling/dialog from being opened (other code should still be preserved for the time being)
    // Commented out code below triggers showing the action bar
    //
    // const action: ActionBarAction = new ActionBarAction(
    //   zaak.identificatie,
    //   ActionEntityType.ZAAK,
    //   zaak.identificatie,
    //   new ActionIcon("link", "actie.zaak.koppelen", editAction),
    //   dismiss,
    //   () => this.isDisabled(zaak.identificatie),
    // );
    // this.utilService.addAction(action);
  }

  private openDialog(zaak: GeneratedType<"RestZaak">, nieuwZaakID: string) {
    const zaakKoppelGegevens = new ZaakKoppelDialogGegevens();
    zaakKoppelGegevens.bronZaakUuid = zaak.uuid;
    zaakKoppelGegevens.doelZaakIdentificatie = nieuwZaakID;

    // Prevent the OLD handling/dialog from being opened (other code should still be preserved for the time being)
    // Commented out code below trigger the actual linking dialog
    //
    // this.dialog.open(ZaakKoppelenDialogComponent, {
    //   data: zaakKoppelGegevens,
    // });
  }

  private deleteTeKoppelenZaak(zaak: GeneratedType<"RestZaak">) {
    const zaken = SessionStorageUtil.getItem<GeneratedType<"RestZaak">[]>(
      "teKoppelenZaken",
      [],
    );
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
      ViewResourceUtil.actieveZaak?.identificatie !== zaakIdentificatie
      ? null
      : "actie.zaak.koppelen.disabled";
  }
}
