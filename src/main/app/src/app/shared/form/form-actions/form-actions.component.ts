/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, computed, ElementRef, inject, input } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { CreateMutationResult } from "@tanstack/angular-query-experimental";

@Component({
  selector: "zac-form-actions",
  templateUrl: "./form-actions.component.html",
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
    input.required<Pick<FormGroup, "valid" | "disabled">>();
  protected readonly mutation =
    input.required<Pick<CreateMutationResult, "isPending">>();

  protected readonly onCancel = (event: MouseEvent) => {
    const cancelEvent = new CustomEvent("cancel", {
      bubbles: true,
      cancelable: true,
      detail: { originalEvent: event },
    });

    this.hostForm()?.dispatchEvent(cancelEvent);
  };
}
