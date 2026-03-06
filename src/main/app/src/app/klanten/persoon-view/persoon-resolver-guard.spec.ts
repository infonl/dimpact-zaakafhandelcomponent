/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { UrlSegment } from "@angular/router";
import { fromPartial } from "@total-typescript/shoehorn";
import { PersoonResolverGuard } from "./persoon-resolver-guard";

describe(PersoonResolverGuard.name, () => {
  let guard: PersoonResolverGuard;

  const validUuid = "1438529a-eb41-4ff9-ac98-8c1f18892b7a";

  const toSegments = (...paths: string[]): UrlSegment[] =>
    paths.map((path) => fromPartial<UrlSegment>({ path }));

  beforeEach(() => {
    guard = new PersoonResolverGuard();
  });

  describe(PersoonResolverGuard.prototype.canMatch.name, () => {
    it("should return true for a valid UUID segment", () => {
      expect(guard.canMatch(fromPartial({}), toSegments(validUuid))).toBe(true);
    });

    it("should return false when segments array is empty", () => {
      expect(guard.canMatch(fromPartial({}), [])).toBe(false);
    });

    it("should return false for a plain string that is not a UUID", () => {
      expect(guard.canMatch(fromPartial({}), toSegments("not-a-uuid"))).toBe(
        false,
      );
    });

    it("should return false for a numeric string", () => {
      expect(guard.canMatch(fromPartial({}), toSegments("123456789"))).toBe(
        false,
      );
    });

    it("should return false for a UUID with invalid characters", () => {
      expect(
        guard.canMatch(
          fromPartial({}),
          toSegments("1438529a-eb41-4ff9-ac98-8c1f18892bZZ"),
        ),
      ).toBe(false);
    });

    it("should return false for a UUID missing a section", () => {
      expect(
        guard.canMatch(fromPartial({}), toSegments("1438529a-eb41-4ff9-ac98")),
      ).toBe(false);
    });

    it("should use only the first segment for the UUID check", () => {
      expect(
        guard.canMatch(fromPartial({}), toSegments(validUuid, "extra-segment")),
      ).toBe(true);
    });

    it("should return false when the first segment path is an empty string", () => {
      expect(guard.canMatch(fromPartial({}), toSegments(""))).toBe(false);
    });
  });
});
