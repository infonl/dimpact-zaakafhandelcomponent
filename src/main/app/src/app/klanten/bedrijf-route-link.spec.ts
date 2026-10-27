/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { fromPartial } from "src/test-helpers";
import { GeneratedType } from "../shared/utils/generated-types";
import { buildBedrijfRouteLink } from "./bedrijf-route-link";

describe("buildBedrijfRouteLink", () => {
  it("returns undefined when bedrijf is null", () => {
    expect(buildBedrijfRouteLink(null)).toBeUndefined();
  });

  it("returns undefined when bedrijf is undefined", () => {
    expect(buildBedrijfRouteLink(undefined)).toBeUndefined();
  });

  it("returns RSIN route for a rechtspersoon bedrijf", () => {
    const bedrijf = {
      kvkNummer: "12345678",
      type: "RECHTSPERSOON",
    } as GeneratedType<"RestBedrijf">;

    expect(buildBedrijfRouteLink(bedrijf)).toEqual(["/bedrijf", "12345678"]);
  });

  it("returns VN route for a vestiging bedrijf", () => {
    const bedrijf = {
      kvkNummer: "12345678",
      vestigingsnummer: "000012345678",
      identificatieType: "VN" as GeneratedType<"IdentificatieType">,
    } as GeneratedType<"RestBedrijf">;

    expect(buildBedrijfRouteLink(bedrijf)).toEqual([
      "/bedrijf",
      "12345678",
      "vestiging",
      "000012345678",
    ]);
  });

  it("throws for an unknown bedrijf type", () => {
    const bedrijf = fromPartial<
      GeneratedType<"RestBedrijf"> & { temporaryPersonId: string }
    >({ identificatieType: "BSN", temporaryPersonId: "some-uuid" });

    expect(() => buildBedrijfRouteLink(bedrijf)).toThrow(
      "Unknown bedrijf type",
    );
  });
});
