/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgTemplateOutlet } from "@angular/common";
import {
  Component,
  computed,
  ElementRef,
  inject,
  input,
  Signal,
} from "@angular/core";
import { FormGroup } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatDialogActions } from "@angular/material/dialog";
import { MatExpansionPanelActionRow } from "@angular/material/expansion";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  selector: "zac-form-actions",
  templateUrl: "./form-actions.component.html",
  standalone: true,
  imports: [
    NgTemplateOutlet,
    MatDialogActions,
    MatExpansionPanelActionRow,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    TranslateModule,
  ],
})
export class ZacFormActions {
  private readonly host = inject(ElementRef<HTMLElement>);

  protected readonly submitLabel = input<string>("actie.verstuur");
  protected readonly cancelLabel = input<string>("actie.annuleren");

  /**
   * The closest form element that contains the host element (= `<zac-form-actions>`).
   */
  private readonly hostForm = computed(() => {
    const form = this.host.nativeElement.closest("form");
    if (form instanceof HTMLFormElement) return form;
    return null;
  });

  /**
   * The wrapper component that will be used to wrap the actions.
   *
   * - `dialog`: The actions will be wrapped in a `mat-dialog-actions` component.
   * - `form`: The actions will be wrapped in a `fieldset` component with a `mat-action-row` component.
   *
   * @default form
   */
  protected readonly wrapper = input<"dialog" | "form">("form");

  protected readonly form =
    input.required<Pick<FormGroup, "valid" | "disabled" | "dirty">>();
  protected readonly mutation = input.required<{
    isPending: Signal<boolean>;
  }>();

  protected readonly onCancel = (event: MouseEvent) => {
    const cancelEvent = new CustomEvent("cancel", {
      bubbles: true,
      cancelable: true,
      detail: { originalEvent: event },
    });

    this.hostForm()?.dispatchEvent(cancelEvent);
  };
}
