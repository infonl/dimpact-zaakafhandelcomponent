/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  booleanAttribute,
  Component,
  computed,
  ElementRef,
  inject,
  input,
  Signal,
} from "@angular/core";
import { FormGroup } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TranslateModule } from "@ngx-translate/core";

@Component({
  selector: "zac-form-actions",
  templateUrl: "./form-actions.component.html",
  standalone: true,
  imports: [
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

  protected readonly form =
    input.required<Pick<FormGroup, "valid" | "disabled" | "dirty">>();

  /**
   * Optional TanStack mutation driving the pending state. When omitted, the
   * `loading` input is used instead, so callback-based dialogs can reuse this
   * component without adopting TanStack mutations.
   */
  protected readonly mutation = input<{
    isPending: Signal<boolean>;
    isSuccess: Signal<boolean>;
  }>();
  protected readonly loading = input(false);

  protected readonly disableAfterSuccess = input(false, {
    transform: booleanAttribute,
  });

  protected isPending() {
    return this.mutation()?.isPending() ?? this.loading();
  }

  protected isSuccess() {
    return this.mutation()?.isSuccess() ?? false;
  }

  protected isSubmitDisabled() {
    const form = this.form();
    return (
      form.disabled ||
      !form.valid ||
      !form.dirty ||
      this.isPending() ||
      (this.disableAfterSuccess() && this.isSuccess())
    );
  }

  protected readonly onCancel = (event: MouseEvent) => {
    const cancelEvent = new CustomEvent("cancel", {
      bubbles: true,
      cancelable: true,
      detail: { originalEvent: event },
    });

    this.hostForm()?.dispatchEvent(cancelEvent);
  };
}
