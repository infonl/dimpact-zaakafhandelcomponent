/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import {
  initiatorAllowedByZaaktype,
  ZaakCreateKoppeling,
  zaaktypenForKoppeling,
} from "./zaak-create-koppeling";

const zaaktypeA: GeneratedType<"RestZaaktypeKeuze"> = {
  uuid: "fake-zaaktype-a-uuid",
  omschrijving: "fakeZaaktypeA",
};
const zaaktypeC: GeneratedType<"RestZaaktypeKeuze"> = {
  uuid: "fake-zaaktype-c-uuid",
  omschrijving: "fakeZaaktypeC",
};

const koppelingTo = (
  relatieType: ZaakCreateKoppeling["relatieType"],
): ZaakCreateKoppeling => ({
  zaak: fromPartial<GeneratedType<"RestZaak">>({
    zaaktype: fromPartial<GeneratedType<"RestZaaktype">>({
      zaaktypeRelaties: [
        { zaaktypeUuid: "fake-zaaktype-a-uuid", relatieType: "DEELZAAK" },
        { zaaktypeUuid: "fake-zaaktype-b-uuid", relatieType: "DEELZAAK" },
        { zaaktypeUuid: "fake-zaaktype-c-uuid", relatieType: "VERVOLG" },
      ],
    }),
  }),
  relatieType,
});

describe(zaaktypenForKoppeling.name, () => {
  it("offers every zaaktype the user may start when the new zaak is not linked", () => {
    expect(zaaktypenForKoppeling([zaaktypeA, zaaktypeC], null)).toEqual([
      zaaktypeA,
      zaaktypeC,
    ]);
  });

  it("offers every zaaktype the user may start for a gerelateerde zaak", () => {
    expect(
      zaaktypenForKoppeling([zaaktypeA, zaaktypeC], koppelingTo("GERELATEERD")),
    ).toEqual([zaaktypeA, zaaktypeC]);
  });

  it("offers for a deelzaak only the deelzaaktypen of the zaak that the user may start", () => {
    expect(
      zaaktypenForKoppeling([zaaktypeA, zaaktypeC], koppelingTo("DEELZAAK")),
    ).toEqual([zaaktypeA]);
  });
});

describe(initiatorAllowedByZaaktype.name, () => {
  const settings = (
    koppelingen: Pick<
      GeneratedType<"RestZaaktypeAanmaakInstellingen">,
      "isBrpKoppelenEnabled" | "isKvkKoppelenEnabled"
    >,
  ) =>
    fromPartial<GeneratedType<"RestZaaktypeAanmaakInstellingen">>({
      uuid: "fake-zaaktype-a-uuid",
      ...koppelingen,
    });
  const persoon = fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
    type: "BSN",
  });
  const vestiging = fromPartial<GeneratedType<"BetrokkeneIdentificatie">>({
    type: "VN",
  });

  it.each([
    [
      "allows",
      "a person",
      persoon,
      "the BRP koppeling",
      { isBrpKoppelenEnabled: true },
      true,
    ],
    [
      "refuses",
      "a person",
      persoon,
      "the KVK koppeling",
      { isKvkKoppelenEnabled: true },
      false,
    ],
    [
      "allows",
      "a company",
      vestiging,
      "the KVK koppeling",
      { isKvkKoppelenEnabled: true },
      true,
    ],
    [
      "refuses",
      "a company",
      vestiging,
      "the BRP koppeling",
      { isBrpKoppelenEnabled: true },
      false,
    ],
  ] as const)(
    "%s %s as initiator when only %s of the zaaktype is switched on",
    (_verdict, _kind, initiator, _koppeling, koppelingen, allowed) => {
      expect(initiatorAllowedByZaaktype(initiator, settings(koppelingen))).toBe(
        allowed,
      );
    },
  );
});
