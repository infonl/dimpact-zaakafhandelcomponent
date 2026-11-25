/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { expect, test } from "@playwright/test";
import { isSetUpFor } from "./zaakafhandelparameters";
import type { Schemas } from "./zac-api";

const groep = { id: "test-group-a" };

function setUpConfiguration(): Schemas["RestZaaktypeConfiguration"] {
  return {
    isValide: true,
    defaultGroepId: groep.id,
    intakeMail: "BESCHIKBAAR_UIT",
    afrondenMail: "BESCHIKBAAR_UIT",
    betrokkeneKoppelingen: {
      isBrpKoppelenEnabled: true,
      isKvkKoppelenEnabled: true,
    },
    zaakAfzenders: [
      { mail: "GEMEENTE", isDefaultMail: true, replyTo: "GEMEENTE" },
    ],
  } as Schemas["RestZaaktypeConfiguration"];
}

const brpDoelbindingen = {
  zoekWaarde: "BRPACT-ZoekenAlgemeen",
  raadpleegWaarde: "BRPACT-Totaal",
  verwerkingregisterWaarde: "Algemeen",
};

test("given zaakafhandelparameters as the journeys expect them, when they are checked, then they are set up", () => {
  expect(
    isSetUpFor(setUpConfiguration(), groep, {
      isBrpDoelbindingPerZaaktype: false,
    }),
  ).toBe(true);
});

test("given a ZAC that sets the BRP doelbindingen per zaaktype and zaakafhandelparameters with all of them, when they are checked, then they are set up", () => {
  expect(
    isSetUpFor({ ...setUpConfiguration(), brpDoelbindingen }, groep, {
      isBrpDoelbindingPerZaaktype: true,
    }),
  ).toBe(true);
});

test("given a ZAC that sets the BRP doelbindingen per zaaktype and zaakafhandelparameters without one of them, when they are checked, then they are not set up, because a beheerder could not save them", () => {
  expect(
    isSetUpFor(
      {
        ...setUpConfiguration(),
        brpDoelbindingen: { ...brpDoelbindingen, raadpleegWaarde: "" },
      },
      groep,
      { isBrpDoelbindingPerZaaktype: true },
    ),
  ).toBe(false);
});

const changes: [string, Partial<Schemas["RestZaaktypeConfiguration"]>][] = [
  ["are not valid", { isValide: false }],
  ["route new zaken to another groep", { defaultGroepId: "test-group-b" }],
  [
    "have a human task that is off",
    {
      humanTaskParameters: [{ actief: false }],
    } as Partial<Schemas["RestZaaktypeConfiguration"]>,
  ],
  ["send a mail at the intake", { intakeMail: "BESCHIKBAAR_AAN" }],
  ["send a mail at the end", { afrondenMail: "BESCHIKBAAR_AAN" }],
  [
    "cannot add a person from the BRP",
    {
      betrokkeneKoppelingen: {
        isBrpKoppelenEnabled: false,
        isKvkKoppelenEnabled: true,
      },
    },
  ],
  [
    "do not send from the gemeente by default",
    {
      zaakAfzenders: [
        { mail: "GEMEENTE", isDefaultMail: false, replyTo: "GEMEENTE" },
      ],
    },
  ],
];

for (const [difference, change] of changes) {
  test(`given zaakafhandelparameters that ${difference}, when they are checked, then they are not set up`, () => {
    expect(
      isSetUpFor({ ...setUpConfiguration(), ...change }, groep, {
        isBrpDoelbindingPerZaaktype: false,
      }),
    ).toBe(false);
  });
}
