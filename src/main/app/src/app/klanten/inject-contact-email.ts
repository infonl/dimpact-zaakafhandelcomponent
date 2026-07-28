/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { computed, effect, inject, Signal } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { GeneratedType } from "../shared/utils/generated-types";
import { KlantenService } from "./klanten.service";

/**
 * Resolves the contact e-mail address for a zaak as a signal. The zaak-specific
 * contact e-mail address takes precedence over the initiator's e-mail address.
 *
 * Must be called within an injection context (constructor or field initializer).
 */
export function injectContactEmail(
  zaak: () => GeneratedType<"RestZaak"> | undefined,
): Signal<string | null> {
  const klantenService = inject(KlantenService);

  const zaakSpecificEmail = computed(
    () => zaak()?.zaakSpecificContactDetails?.emailAddress ?? null,
  );
  const temporaryPersonId = computed(
    () => zaak()?.initiatorIdentificatie?.temporaryPersonId,
  );

  const contactDetailsQuery = injectQuery(() => ({
    ...klantenService.getContactDetailsForPersonQuery(
      temporaryPersonId() ?? "",
    ),
    enabled: !zaakSpecificEmail() && !!temporaryPersonId(),
  }));

  effect(() => {
    const error = contactDetailsQuery.error();
    if (error) {
      console.error("Failed to resolve contact email address", error);
    }
  });

  return computed(() => {
    const specificEmail = zaakSpecificEmail();
    if (specificEmail) return specificEmail;
    if (contactDetailsQuery.isError()) return null;
    return contactDetailsQuery.data()?.emailadres ?? null;
  });
}
