/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { effect, inject } from "@angular/core";
import { AbstractControl } from "@angular/forms";
import { injectQuery } from "@tanstack/angular-query-experimental";
import { GeneratedType } from "../shared/utils/generated-types";
import { ZakenService } from "./zaken.service";

/**
 * Must be called within an injection context (constructor or field initializer).
 */
export function injectAfzenders(
  zaakUuid: () => string,
  verzender: () => AbstractControl<GeneratedType<"RestZaakAfzender"> | null> | null,
) {
  const zakenService = inject(ZakenService);

  const afzendersQuery = injectQuery(() =>
    zakenService.listAfzendersVoorZaak(zaakUuid()),
  );

  effect(() => {
    const afzenders = afzendersQuery.data();
    const control = verzender();
    if (afzenders === undefined || !control || control.dirty) return;

    control.setValue(
      afzenders.find(({ isDefaultMail }) => isDefaultMail) ?? null,
    );
  });

  return afzendersQuery;
}
