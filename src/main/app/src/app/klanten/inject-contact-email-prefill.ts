/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { effect, signal, untracked } from "@angular/core";
import { FormControl } from "@angular/forms";
import { GeneratedType } from "../shared/utils/generated-types";
import { injectContactEmail } from "./inject-contact-email";

/**
 * Must be called within an injection context (constructor or field initializer).
 */
export function injectContactEmailPrefill() {
  const zaak = signal<GeneratedType<"RestZaak"> | undefined>(undefined);
  const contactEmailAddress = injectContactEmail(zaak);
  const controlToPrefill = signal<FormControl<string | null> | null>(null);

  effect(() => {
    const emailAddress = contactEmailAddress();
    const control = controlToPrefill();
    if (!emailAddress || !control) return;

    untracked(() => {
      control.setValue(emailAddress);
      controlToPrefill.set(null);
    });
  });

  return (
    control: FormControl<string | null>,
    zaakToPrefillFrom: GeneratedType<"RestZaak">,
  ) => {
    zaak.set(zaakToPrefillFrom);
    controlToPrefill.set(control);
  };
}
