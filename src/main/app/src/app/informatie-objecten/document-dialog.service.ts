/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { inject, Injectable } from "@angular/core";
import { MatDialog } from "@angular/material/dialog";
import { TranslateService } from "@ngx-translate/core";
import { Observable } from "rxjs";
import {
  ConfirmDialogComponent,
  ConfirmDialogData,
} from "../shared/confirm-dialog/confirm-dialog.component";
import {
  RedenDialogComponent,
  RedenDialogData,
} from "../shared/dialog/reden-dialog/reden-dialog.component";

/**
 * Opens the document confirmation dialogs. Presentation config and message keys
 * live here; callers pass the dynamic values and business logic, and handle
 * `afterClosed()`.
 */
@Injectable({ providedIn: "root" })
export class DocumentDialogService {
  private readonly dialog = inject(MatDialog);
  private readonly translateService = inject(TranslateService);

  ontkoppelDocument(
    melding: string,
    callback: (reden: string) => Observable<unknown>,
  ) {
    return this.dialog.open<RedenDialogComponent, RedenDialogData>(
      RedenDialogComponent,
      {
        data: {
          titleKey: "actie.document.ontkoppelen",
          icon: "link_off",
          label: "reden",
          multiline: true,
          maxlength: 200,
          melding,
          confirmButtonActionKey: "actie.document.ontkoppelen",
          callback,
        },
      },
    );
  }

  /**
   * Verwijderen with a reason when the document belongs to a zaak, otherwise a
   * plain confirm (ontkoppelde documenten have no reden field).
   */
  verwijderDocument(params: {
    hasZaak: boolean;
    documentTitel?: string;
    delete: (reden?: string) => Observable<unknown>;
  }) {
    if (params.hasZaak) {
      return this.dialog.open<RedenDialogComponent, RedenDialogData>(
        RedenDialogComponent,
        {
          data: {
            titleKey: "actie.document.verwijderen",
            icon: "delete",
            label: "actie.document.verwijderen.reden",
            maxlength: 100,
            melding: this.translateService.instant(
              "msg.document.verwijderen.bevestigen",
              { document: params.documentTitel },
            ),
            confirmButtonActionKey: "actie.document.verwijderen",
            callback: (reden) => params.delete(reden),
          },
        },
      );
    }

    return this.dialog.open(ConfirmDialogComponent, {
      data: new ConfirmDialogData(
        {
          key: "msg.document.verwijderen.bevestigen",
          args: { document: params.documentTitel },
        },
        params.delete(),
      ),
    });
  }
}
