/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { KNOWN_ZAC_FIELDS } from "../../taken/taak-view/formio/formio-setup-service";
import { ZAC_BUILDER_PALETTE } from "./zac-builder-palette";

describe("ZAC_BUILDER_PALETTE", () => {
  const removedComponents = {
    basic: ["password"],
    advanced: ["address", "currency", "signature", "survey", "tags", "url"],
    layout: ["well"],
    data: ["datamap"],
  } as const;

  describe("components an administrator may not add to a task form", () => {
    Object.entries(removedComponents).forEach(([group, componentTypes]) => {
      componentTypes.forEach((componentType) => {
        it(`should switch '${componentType}' off in the '${group}' group`, () => {
          expect(
            ZAC_BUILDER_PALETTE[group as keyof typeof removedComponents]
              .components[componentType],
          ).toBe(false);
        });
      });
    });

    it("should drop the whole premium group, which holds file upload, nested forms and the custom component", () => {
      expect(ZAC_BUILDER_PALETTE.premium).toBe(false);
    });

    it("should name every removed component rather than omit it, so that Form.io does not fill the key back in", () => {
      const named = Object.entries(removedComponents).flatMap(
        ([group, componentTypes]) =>
          componentTypes.map((componentType) =>
            Object.hasOwn(
              ZAC_BUILDER_PALETTE[group as keyof typeof removedComponents]
                .components,
              componentType,
            ),
          ),
      );

      expect(named.every(Boolean)).toBe(true);
    });
  });

  describe("components an administrator may still add", () => {
    it("should keep the groups that hold the remaining stock components", () => {
      expect(ZAC_BUILDER_PALETTE.basic).not.toBe(false);
      expect(ZAC_BUILDER_PALETTE.advanced).not.toBe(false);
      expect(ZAC_BUILDER_PALETTE.layout).not.toBe(false);
      expect(ZAC_BUILDER_PALETTE.data).not.toBe(false);
    });

    it("should leave a component that is not switched off out of the group, so that Form.io supplies it", () => {
      expect(ZAC_BUILDER_PALETTE.basic.components).toEqual({
        password: false,
      });
    });

    it("should offer the ZAC presets, which no stock group provides", () => {
      expect(Object.keys(ZAC_BUILDER_PALETTE.zac.components)).toEqual([
        "zacGroep",
        "zacMedewerker",
        "zacReferentieTabel",
        "zacDocumenten",
        "zacDocumentenNietOndertekend",
      ]);
    });

    it("should offer the components that carry explanatory text on a task form", () => {
      expect(ZAC_BUILDER_PALETTE.layout.components).not.toHaveProperty(
        "content",
      );
      expect(ZAC_BUILDER_PALETTE.layout.components).not.toHaveProperty(
        "htmlelement",
      );
    });

    it("should keep the link column of the signing preset", () => {
      const { components } =
        ZAC_BUILDER_PALETTE.zac.components.zacDocumentenNietOndertekend.schema;

      expect(
        components.find((component) => component.type === "htmlelement")
          ?.attributes,
      ).toEqual({ ZAC_TYPE: KNOWN_ZAC_FIELDS.REGEL_LINK_VIEW_ICON });
    });
  });
});
