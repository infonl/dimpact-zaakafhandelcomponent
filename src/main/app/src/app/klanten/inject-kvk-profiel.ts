/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HttpErrorResponse } from "@angular/common/http";
import { computed, inject } from "@angular/core";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { KlantenService } from "./klanten.service";

export type KvkProfielAanvraag =
  | { vestigingsnummer: string }
  | { kvkNummer: string }
  | null;

/**
 * The vestigingsprofiel of a vestiging, or the basisprofiel of a rechtspersoon.
 *
 * Must be called within an injection context (constructor or field initializer).
 */
export function injectKvkProfiel(aanvraag: () => KvkProfielAanvraag) {
  const klantenService = inject(KlantenService);

  const vestigingsnummer = computed(() => {
    const profielAanvraag = aanvraag();
    return profielAanvraag && "vestigingsnummer" in profielAanvraag
      ? profielAanvraag.vestigingsnummer
      : null;
  });

  const kvkNummer = computed(() => {
    const profielAanvraag = aanvraag();
    return profielAanvraag && "kvkNummer" in profielAanvraag
      ? profielAanvraag.kvkNummer
      : null;
  });

  const vestigingsprofielQuery = injectQuery(() => ({
    ...klantenService.readVestigingsprofiel(vestigingsnummer() ?? ""),
    meta: { reportErrors: false },
    enabled: !!vestigingsnummer(),
  }));

  const basisprofielQuery = injectQuery(() => ({
    ...klantenService.readBasisprofiel(kvkNummer() ?? ""),
    meta: { reportErrors: false },
    enabled: !!kvkNummer(),
  }));

  const profiel = computed(() => {
    if (vestigingsnummer()) return vestigingsprofielQuery.data() ?? null;
    if (kvkNummer()) return basisprofielQuery.data() ?? null;
    return null;
  });

  const foutmelding = computed(() => {
    if (vestigingsnummer())
      return toKvkFoutmelding(vestigingsprofielQuery.error());
    if (kvkNummer()) return toKvkFoutmelding(basisprofielQuery.error());
    return null;
  });

  return {
    profiel,
    adressen: computed(() =>
      foutmelding() ? [] : (profiel()?.adressen ?? []),
    ),
    foutmelding,
  };
}

function toKvkFoutmelding(error: HttpErrorResponse | null) {
  if (!error) return null;
  return error.status === 404
    ? "msg.hint.kvk.outdated"
    : "msg.hint.kvk.other-error";
}
