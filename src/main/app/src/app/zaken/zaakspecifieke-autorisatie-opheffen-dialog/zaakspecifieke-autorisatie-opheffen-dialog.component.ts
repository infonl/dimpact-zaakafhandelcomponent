/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatDialogActions, MatDialogRef } from "@angular/material/dialog";
import { TranslatePipe } from "@ngx-translate/core";
import { GenericDialogComponent } from "../../shared/dialog/generic-dialog/generic-dialog.component";

@Component({
  templateUrl: "./zaakspecifieke-autorisatie-opheffen-dialog.component.html",
  standalone: true,
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    GenericDialogComponent,
    MatButtonModule,
    MatDialogActions,
    TranslatePipe,
  ],
})
export class ZaakspecifiekeAutorisatieOpheffenDialogComponent {
  protected readonly dialogRef =
    inject<
      MatDialogRef<ZaakspecifiekeAutorisatieOpheffenDialogComponent, boolean>
    >(MatDialogRef);
}
