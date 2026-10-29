/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { GeneratedType } from "../../shared/utils/generated-types";

/**
 * The existing zaak that a new zaak is linked to when it is created.
 * `relatieType` is the role of the new zaak: `DEELZAAK` makes the new zaak a deelzaak of `zaak`.
 */
export type ZaakCreateKoppeling = {
  zaak: GeneratedType<"RestZaak">;
  relatieType: Extract<
    GeneratedType<"RelatieType">,
    "GERELATEERD" | "DEELZAAK"
  >;
};

export function zaaktypenForKoppeling(
  zaaktypen: GeneratedType<"RestZaaktype">[],
  koppeling: ZaakCreateKoppeling | null,
) {
  if (koppeling?.relatieType !== "DEELZAAK") return zaaktypen;

  const deelzaaktypeUuids = new Set(
    (koppeling.zaak.zaaktype.zaaktypeRelaties ?? [])
      .filter(({ relatieType }) => relatieType === "DEELZAAK")
      .map(({ zaaktypeUuid }) => zaaktypeUuid),
  );
  return zaaktypen.filter(({ uuid }) => deelzaaktypeUuids.has(uuid));
}

export function initiatorAllowedByZaaktype(
  initiator: GeneratedType<"BetrokkeneIdentificatie">,
  zaaktype: GeneratedType<"RestZaaktype">,
) {
  const betrokkeneKoppelingen =
    zaaktype.zaakafhandelparameters?.betrokkeneKoppelingen;
  return initiator.type === "BSN"
    ? Boolean(betrokkeneKoppelingen?.isBrpKoppelenEnabled)
    : Boolean(betrokkeneKoppelingen?.isKvkKoppelenEnabled);
}
