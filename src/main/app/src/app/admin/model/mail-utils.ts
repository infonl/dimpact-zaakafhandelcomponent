/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";
import { toI18nKey } from "../../shared/utils/i18n-key";

export function getBeschikbareMailtemplateKoppelingen() {
  return [
    "ZAAK_ONTVANKELIJK",
    "ZAAK_NIET_ONTVANKELIJK",
    "ZAAK_AFGEHANDELD",
    "TAAK_AANVULLENDE_INFORMATIE",
    "TAAK_ONTVANGSTBEVESTIGING",
    "TAAK_ADVIES_EXTERN",
    "ZAAK_ALGEMEEN",
  ] as const satisfies GeneratedType<"Mail">[];
}

export function mailSelectList() {
  return getBeschikbareMailtemplateKoppelingen().map((koppeling) => ({
    label: toI18nKey(`mail.${koppeling}`),
    value: koppeling,
  }));
}
