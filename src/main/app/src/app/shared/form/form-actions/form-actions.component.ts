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
import { AbstractControl } from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { MatProgressSpinnerModule } from "@angular/material/progress-spinner";
import { TranslatePipe } from "@ngx-translate/core";
import { trackControl } from "../control-state";

@Component({
  selector: "zac-form-actions",
  templateUrl: "./form-actions.component.html",
  styleUrls: ["./form-actions.component.less"],
  standalone: true,
  imports: [
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    TranslatePipe,
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

  protected readonly form = input.required<AbstractControl>();

  private readonly readForm = trackControl(this.form);
  private readonly formDisabled = this.readForm((form) => form.disabled);
  private readonly formValid = this.readForm((form) => form.valid);
  private readonly formDirty = this.readForm((form) => form.dirty);

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

  protected readonly isPending = computed(
    () => this.mutation()?.isPending() ?? this.loading(),
  );

  private readonly isSuccess = computed(
    () => this.mutation()?.isSuccess() ?? false,
  );

  protected readonly isSubmitDisabled = computed(
    () =>
      !!this.formDisabled() ||
      !this.formValid() ||
      !this.formDirty() ||
      this.isPending() ||
      (this.disableAfterSuccess() && this.isSuccess()),
  );

  protected readonly onCancel = (event: MouseEvent) => {
    const cancelEvent = new CustomEvent("cancel", {
      bubbles: true,
      cancelable: true,
      detail: { originalEvent: event },
    });

    this.hostForm()?.dispatchEvent(cancelEvent);
  };
}
