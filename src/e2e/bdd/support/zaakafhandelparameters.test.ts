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

test("given zaakafhandelparameters as the journeys expect them, when they are checked, then they are set up", () => {
  expect(isSetUpFor(setUpConfiguration(), groep)).toBe(true);
});

const changes: [string, Partial<Schemas["RestZaaktypeConfiguration"]>][] = [
  ["are not valid", { isValide: false }],
  ["route new zaken to another groep", { defaultGroepId: "test-group-b" }],
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
    expect(isSetUpFor({ ...setUpConfiguration(), ...change }, groep)).toBe(
      false,
    );
  });
}
