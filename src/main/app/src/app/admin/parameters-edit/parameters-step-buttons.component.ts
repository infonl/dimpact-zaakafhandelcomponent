/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from "@angular/core";
import { MatButtonModule } from "@angular/material/button";
import { MatStepperModule } from "@angular/material/stepper";
import { TranslatePipe } from "@ngx-translate/core";

@Component({
  selector: "zac-parameters-step-buttons",
  standalone: true,
  imports: [MatButtonModule, MatStepperModule, TranslatePipe],
  styles: [
    `
      .button-group {
        margin-top: 16px;
        padding: 10px;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <div class="button-group flex-row flex-wrap">
      @if (!isFirst()) {
        <button mat-raised-button type="button" matStepperPrevious>
          {{ "actie.terug" | translate }}
        </button>
      }
      @if (!isLast()) {
        <button mat-raised-button type="button" matStepperNext>
          {{ "actie.volgende" | translate }}
        </button>
      }
      <button
        mat-raised-button
        type="button"
        color="primary"
        [disabled]="!canSave()"
        (click)="save.emit()"
      >
        {{ "actie.opslaan" | translate }}
      </button>
    </div>
  `,
})
export class ParametersStepButtonsComponent {
  readonly isFirst = input(false);
  readonly isLast = input(false);
  readonly canSave = input.required<boolean>();
  readonly save = output();
}
