/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { Observable } from "rxjs";
import {
  RedenDialogComponent,
  RedenDialogData,
} from "../shared/dialog/reden-dialog/reden-dialog.component";
import { GeneratedType } from "../shared/utils/generated-types";
import {
  ZaakAfbrekenDialogComponent,
  ZaakAfbrekenDialogData,
} from "./zaak-afbreken-dialog/zaak-afbreken-dialog.component";

type RedenCallback = (reden: string) => Observable<unknown>;

/**
 * Opens the zaak confirmation dialogs (a reden field + confirm/cancel).
 * The presentation config (title, icon, message keys) lives here; callers pass
 * only the dynamic values and the business callback, and handle `afterClosed()`.
 */
@Injectable({ providedIn: "root" })
export class ZaakDialogService {
  private readonly dialog = inject(MatDialog);
  private readonly translateService = inject(TranslateService);

  private openReden(data: RedenDialogData) {
    return this.dialog.open<RedenDialogComponent, RedenDialogData>(
      RedenDialogComponent,
      { data },
    );
  }

  afbreken(
    options: Observable<GeneratedType<"RestZaakbeeindigReden">[]>,
    callback: (
      reden: GeneratedType<"RestZaakbeeindigReden">,
    ) => Observable<unknown>,
  ) {
    return this.dialog.open<
      ZaakAfbrekenDialogComponent,
      ZaakAfbrekenDialogData
    >(ZaakAfbrekenDialogComponent, { data: { options, callback } });
  }

  heropenen(callback: RedenCallback) {
    return this.openReden({
      titleKey: "actie.zaak.heropenen",
      icon: "restart_alt",
      label: "actie.zaak.heropenen.reden",
      maxlength: 100,
      confirmButtonActionKey: "actie.zaak.heropenen",
      callback,
    });
  }

  hervatten(
    meldingArgs: { duur: number; verwachteDuur?: number | null },
    callback: RedenCallback,
  ) {
    return this.openReden({
      titleKey: "actie.zaak.hervatten",
      icon: "play_circle",
      label: "reden",
      maxlength: 200,
      melding: this.translateService.instant("msg.zaak.hervatten", meldingArgs),
      confirmButtonActionKey: "actie.zaak.hervatten",
      callback,
    });
  }

  wijzigInitiator(naam: string | null | undefined, callback: RedenCallback) {
    return this.openReden({
      titleKey: "actie.initiator.wijzigen",
      icon: "link",
      label: "reden",
      multiline: true,
      melding: this.translateService.instant("msg.initiator.bevestigen", {
        naam,
      }),
      confirmButtonActionKey: "actie.initiator.wijzigen",
      callback,
    });
  }

  ontkoppelInitiator(callback: RedenCallback) {
    return this.openReden({
      titleKey: "actie.initiator.ontkoppelen",
      icon: "link_off",
      label: "reden",
      multiline: true,
      melding: this.translateService.instant(
        "msg.initiator.ontkoppelen.bevestigen",
      ),
      confirmButtonActionKey: "actie.initiator.ontkoppelen",
      callback,
    });
  }

  ontkoppelBetrokkene(betrokkene: string, callback: RedenCallback) {
    return this.openReden({
      titleKey: "actie.betrokkene.ontkoppelen",
      icon: "link_off",
      label: "reden",
      multiline: true,
      melding: this.translateService.instant(
        "msg.betrokkene.ontkoppelen.bevestigen",
        { betrokkene },
      ),
      confirmButtonActionKey: "actie.betrokkene.ontkoppelen",
      callback,
    });
  }

  verwijderBagObject(
    omschrijving: string | undefined,
    callback: RedenCallback,
  ) {
    return this.openReden({
      titleKey: "actie.bagObject.ontkoppelen",
      icon: "link_off",
      label: "reden",
      maxlength: 80,
      uitleg: this.translateService.instant(
        "msg.bagObject.ontkoppelen.bevestigen",
        { omschrijving },
      ),
      confirmButtonActionKey: "actie.bagObject.ontkoppelen",
      callback,
    });
  }
}
