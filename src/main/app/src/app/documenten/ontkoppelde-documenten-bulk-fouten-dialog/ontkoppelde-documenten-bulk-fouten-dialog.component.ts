/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import {
  MAT_DIALOG_DATA,
  MatDialogActions,
  MatDialogClose,
  MatDialogContent,
  MatDialogTitle,
} from "@angular/material/dialog";
import { MatDividerModule } from "@angular/material/divider";
import { MatToolbarModule } from "@angular/material/toolbar";
import { TranslatePipe } from "@ngx-translate/core";
import { I18nKeyPipe } from "../../shared/pipes/i18n-key.pipe";
import { GeneratedType } from "../../shared/utils/generated-types";

export type OntkoppeldeDocumentenBulkFoutenDialogData = {
  actie: "verwijderen" | "koppelen";
  mislukt: GeneratedType<"RestDetachedDocumentBulkFailure">[];
};

@Component({
  templateUrl: "./ontkoppelde-documenten-bulk-fouten-dialog.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatToolbarModule,
    MatDialogTitle,
    MatDialogContent,
    MatDialogActions,
    MatDialogClose,
    MatDividerModule,
    MatButtonModule,
    TranslatePipe,
    I18nKeyPipe,
  ],
})
export class OntkoppeldeDocumentenBulkFoutenDialogComponent {
  protected readonly data =
    inject<OntkoppeldeDocumentenBulkFoutenDialogData>(MAT_DIALOG_DATA);
}
