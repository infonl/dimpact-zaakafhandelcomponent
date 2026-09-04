/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "../../../../test-helpers";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  allowBedrijf,
  allowedToAddBetrokkene,
  allowPersoon,
  hasAfleidingswijzeBrondatumEigenschap,
  hasZaakData,
  initiatorViewType,
  showBetrokkeneKoppelingen,
  showInitiator,
} from "./zaak-view.predicates";

type Koppelingen = { brpKoppelen?: boolean; kvkKoppelen?: boolean };

type InitiatorType = NonNullable<
  NonNullable<GeneratedType<"RestZaak">["initiatorIdentificatie"]>["type"]
>;

type Afleidingswijze = NonNullable<
  NonNullable<
    NonNullable<GeneratedType<"RestZaak">["resultaat"]>["resultaattype"]
  >["bronArchiefprocedure"]
>["afleidingswijze"];

function createZaak({
  koppelingen,
  rechten = {},
  initiatorType,
  contactDetails,
  zaakdata,
  afleidingswijze,
}: {
  koppelingen?: Koppelingen;
  rechten?: Partial<GeneratedType<"RestZaakRechten">>;
  initiatorType?: InitiatorType;
  contactDetails?: { telephoneNumber?: string; emailAddress?: string };
  zaakdata?: Record<string, unknown>;
  afleidingswijze?: Lowercase<NonNullable<Afleidingswijze>>;
} = {}) {
  return fromPartial<GeneratedType<"RestZaak">>({
    zaaktype: fromPartial({
      zaakafhandelparameters: koppelingen
        ? fromPartial({ betrokkeneKoppelingen: fromPartial(koppelingen) })
        : undefined,
    }),
    rechten: fromPartial(rechten),
    initiatorIdentificatie: initiatorType
      ? fromPartial({ type: initiatorType })
      : undefined,
    zaakSpecificContactDetails: contactDetails
      ? fromPartial(contactDetails)
      : undefined,
    zaakdata,
    resultaat: afleidingswijze
      ? fromPartial({
          resultaattype: fromPartial({
            // The backend sends this field lowercase, which the generated union does not model.
            bronArchiefprocedure: fromPartial({
              afleidingswijze: afleidingswijze as Afleidingswijze,
            }),
          }),
        })
      : undefined,
  });
}

describe(hasZaakData.name, () => {
  it("is false when the zaak has no zaakdata at all", () => {
    expect(hasZaakData(createZaak())).toBe(false);
  });

  it("is false when the zaakdata object is empty", () => {
    expect(hasZaakData(createZaak({ zaakdata: {} }))).toBe(false);
  });

  it("is true when the zaakdata object holds at least one key", () => {
    expect(
      hasZaakData(createZaak({ zaakdata: { fakeKey: "fakeValue" } })),
    ).toBe(true);
  });
});

describe(hasAfleidingswijzeBrondatumEigenschap.name, () => {
  it("matches the lowercase value the backend returns, which the generated types spell uppercase", () => {
    expect(
      hasAfleidingswijzeBrondatumEigenschap(
        createZaak({ afleidingswijze: "eigenschap" }),
      ),
    ).toBe(true);
  });

  it("is false for any other afleidingswijze", () => {
    expect(
      hasAfleidingswijzeBrondatumEigenschap(
        createZaak({ afleidingswijze: "afgehandeld" }),
      ),
    ).toBe(false);
  });

  it("is false when the zaak has no resultaat", () => {
    expect(hasAfleidingswijzeBrondatumEigenschap(createZaak())).toBe(false);
  });
});

describe(showInitiator.name, () => {
  it("is false when the zaaktype configures no betrokkene koppelingen", () => {
    expect(showInitiator(createZaak())).toBe(false);
  });

  it("is false when both koppelingen are switched off", () => {
    expect(
      showInitiator(
        createZaak({ koppelingen: { brpKoppelen: false, kvkKoppelen: false } }),
      ),
    ).toBe(false);
  });

  it.each([
    ["brpKoppelen", { brpKoppelen: true }],
    ["kvkKoppelen", { kvkKoppelen: true }],
  ])("is true when %s is switched on", (_name, koppelingen) => {
    expect(showInitiator(createZaak({ koppelingen }))).toBe(true);
  });

  it("is true on zaak specific contact details even when no koppelingen are configured", () => {
    expect(
      showInitiator(
        createZaak({ contactDetails: { emailAddress: "fake@example.com" } }),
      ),
    ).toBe(true);
  });
});

describe(initiatorViewType.name, () => {
  it("is PERSON for a BSN initiator when brpKoppelen is on", () => {
    expect(
      initiatorViewType(
        createZaak({
          koppelingen: { brpKoppelen: true },
          initiatorType: "BSN",
        }),
      ),
    ).toBe("PERSON");
  });

  it.each(["VN", "RSIN"] as const)(
    "is COMPANY for a %s initiator when kvkKoppelen is on",
    (initiatorType) => {
      expect(
        initiatorViewType(
          createZaak({ koppelingen: { kvkKoppelen: true }, initiatorType }),
        ),
      ).toBe("COMPANY");
    },
  );

  it("is ADD for a BSN initiator when brpKoppelen is off", () => {
    expect(
      initiatorViewType(
        createZaak({
          koppelingen: { kvkKoppelen: true },
          initiatorType: "BSN",
        }),
      ),
    ).toBe("ADD");
  });

  it("is CONTACT_DETAILS when there is no matching initiator but the zaak has contact details", () => {
    expect(
      initiatorViewType(
        createZaak({
          koppelingen: { brpKoppelen: true },
          contactDetails: { telephoneNumber: "0612345678" },
        }),
      ),
    ).toBe("CONTACT_DETAILS");
  });

  it("prefers the identified initiator over the zaak specific contact details", () => {
    expect(
      initiatorViewType(
        createZaak({
          koppelingen: { brpKoppelen: true },
          initiatorType: "BSN",
          contactDetails: { telephoneNumber: "0612345678" },
        }),
      ),
    ).toBe("PERSON");
  });

  it("is ADD when nothing is configured and nothing is known", () => {
    expect(initiatorViewType(createZaak())).toBe("ADD");
  });
});

describe(allowBedrijf.name, () => {
  it("is true only when the recht and kvkKoppelen agree", () => {
    expect(
      allowBedrijf(
        createZaak({
          koppelingen: { kvkKoppelen: true },
          rechten: { toevoegenInitiatorBedrijf: true },
        }),
      ),
    ).toBe(true);
  });

  it("is false when kvkKoppelen is off despite the recht", () => {
    expect(
      allowBedrijf(
        createZaak({
          koppelingen: { kvkKoppelen: false },
          rechten: { toevoegenInitiatorBedrijf: true },
        }),
      ),
    ).toBe(false);
  });

  it("is false when the recht is missing despite kvkKoppelen", () => {
    expect(
      allowBedrijf(
        createZaak({
          koppelingen: { kvkKoppelen: true },
          rechten: { toevoegenInitiatorBedrijf: false },
        }),
      ),
    ).toBe(false);
  });
});

describe(allowPersoon.name, () => {
  const zaak = createZaak({
    koppelingen: { brpKoppelen: true },
    rechten: { toevoegenInitiatorPersoon: true },
  });

  it("is true when the recht, brpKoppelen and the BRP search permission all agree", () => {
    expect(allowPersoon(zaak, true)).toBe(true);
  });

  it("is false without the BRP search permission", () => {
    expect(allowPersoon(zaak, false)).toBe(false);
  });

  it("is false when brpKoppelen is off", () => {
    expect(
      allowPersoon(
        createZaak({
          koppelingen: { brpKoppelen: false },
          rechten: { toevoegenInitiatorPersoon: true },
        }),
        true,
      ),
    ).toBe(false);
  });
});

describe(allowedToAddBetrokkene.name, () => {
  it("is true via the BRP route when the recht, brpKoppelen and the BRP search permission agree", () => {
    expect(
      allowedToAddBetrokkene(
        createZaak({
          koppelingen: { brpKoppelen: true },
          rechten: { toevoegenInitiatorPersoon: true },
        }),
        true,
      ),
    ).toBe(true);
  });

  it("is false on the BRP route without the BRP search permission", () => {
    expect(
      allowedToAddBetrokkene(
        createZaak({
          koppelingen: { brpKoppelen: true },
          rechten: { toevoegenInitiatorPersoon: true },
        }),
        false,
      ),
    ).toBe(false);
  });

  it("is true via the KVK route without needing the BRP search permission", () => {
    expect(
      allowedToAddBetrokkene(
        createZaak({
          koppelingen: { kvkKoppelen: true },
          rechten: { toevoegenInitiatorBedrijf: true },
        }),
        false,
      ),
    ).toBe(true);
  });

  it("is false when no koppeling is configured", () => {
    expect(
      allowedToAddBetrokkene(
        createZaak({
          rechten: {
            toevoegenInitiatorPersoon: true,
            toevoegenInitiatorBedrijf: true,
          },
        }),
        true,
      ),
    ).toBe(false);
  });
});

describe(showBetrokkeneKoppelingen.name, () => {
  const zaak = createZaak({ koppelingen: { brpKoppelen: true } });

  it("is false when the zaak has no betrokkenen, even with a koppeling configured", () => {
    expect(showBetrokkeneKoppelingen(zaak, 0)).toBe(false);
  });

  it("is true when a koppeling is configured and the zaak has betrokkenen", () => {
    expect(showBetrokkeneKoppelingen(zaak, 2)).toBe(true);
  });

  it("is false when no koppeling is configured, even with betrokkenen", () => {
    expect(showBetrokkeneKoppelingen(createZaak(), 2)).toBe(false);
  });
});
