/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, input, output } from "@angular/core";
import { MatIconButton } from "@angular/material/button";
import { MatDialogContent } from "@angular/material/dialog";
import { MatDivider } from "@angular/material/divider";
import { MatIcon } from "@angular/material/icon";
import { MatToolbar } from "@angular/material/toolbar";
import { TranslateModule } from "@ngx-translate/core";

/**
 * Presentational confirm-dialog shell: renders the toolbar (title, optional
 * icon, close button), an optional message/explanation area and projects the
 * form fields via `<ng-content>`. The action buttons are provided separately by
 * `<zac-form-actions>` inside the host `<form>`.
 */
@Component({
  selector: "zac-generic-dialog",
  templateUrl: "./generic-dialog.component.html",
  standalone: true,
  imports: [
    MatToolbar,
    MatIcon,
    MatIconButton,
    MatDivider,
    MatDialogContent,
    TranslateModule,
  ],
})
export class GenericDialogComponent {
  readonly titleKey = input<string>();
  readonly icon = input<string>();
  readonly melding = input<string>();
  readonly uitleg = input<string>();
  readonly loading = input(false);

  readonly cancelled = output<void>();
}
