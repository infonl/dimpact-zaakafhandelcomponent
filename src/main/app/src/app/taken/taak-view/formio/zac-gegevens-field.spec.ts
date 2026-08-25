/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ExtendedComponentSchema } from "@formio/angular";
import { TranslateService } from "@ngx-translate/core";
import { GeneratedType } from "../../../shared/utils/generated-types";
import {
  gegevensInputComponent,
  taak,
  zaak,
  zaakGegevensComponent,
} from "./formio-setup-service.test-fixtures";
import { initializeGegevensField } from "./zac-gegevens-field";

const NO_VALUE_MARK = "—";

const translateService = {
  instant: (key: string) => key,
} as unknown as TranslateService;

describe(initializeGegevensField.name, () => {
  function seed(
    component: ExtendedComponentSchema,
    {
      taakdata = {},
      source = zaak as object,
      sourceLabel = "Zaak",
    }: {
      taakdata?: Record<string, unknown>;
      source?: object;
      sourceLabel?: string;
    } = {},
  ) {
    const seededTaak = { ...taak, taakdata } as GeneratedType<"RestTask">;
    initializeGegevensField(
      component,
      source,
      sourceLabel,
      seededTaak,
      translateService,
    );
    return { component, taakdata: seededTaak.taakdata };
  }

  describe("a field that holds no value", () => {
    it("should point the designer at the template syntax instead of rendering nothing", () => {
      const { component } = seed({
        ...zaakGegevensComponent("identificatie"),
        type: "content",
        input: false,
      });

      expect(component.html).toContain("{{ zaak.identificatie }}");
      expect(component.html).toContain("holds no value");
    });

    it("should name the taak as the source when the field reads the taak", () => {
      const { component } = seed(
        { ...zaakGegevensComponent("naam"), type: "content", input: false },
        { source: taak, sourceLabel: "Taak" },
      );

      expect(component.html).toContain("{{ taak.naam }}");
    });
  });

  describe("a field that holds a value", () => {
    it("should leave the field editable instead of rendering it as text", () => {
      const { component } = seed(gegevensInputComponent("identificatie"));

      expect(component.type).toBe("textfield");
      expect(component.input).toBe(true);
      expect(component.html).toBeUndefined();
      expect(component.defaultValue).toBe("ZAAK-2026-0000000835");
    });

    it("should put the value in the task data, because Form.io prefers that over a default", () => {
      const { component, taakdata } = seed(
        gegevensInputComponent("identificatie"),
      );

      expect(taakdata![component.key]).toBe("ZAAK-2026-0000000835");
    });

    it("should not overwrite an answer the user already stored", () => {
      const { component, taakdata } = seed(
        gegevensInputComponent("identificatie"),
        { taakdata: { IN_Seeded: "edited by the user" } },
      );

      expect(taakdata![component.key]).toBe("edited by the user");
      expect(component.defaultValue).toBe("ZAAK-2026-0000000835");
    });

    it("should apply the format function to the seeded value", () => {
      const { component } = seed(
        gegevensInputComponent("startdatum", { formaat: "datum" }),
      );

      expect(component.defaultValue).toBe("24-08-2026");
    });

    it("should seed from the taak as well as from the zaak", () => {
      const { component } = seed(gegevensInputComponent("groep.naam"), {
        source: {
          ...taak,
          groep: { id: "fakeGroupId", naam: "fakeGroupName" },
        },
        sourceLabel: "Taak",
      });

      expect(component.defaultValue).toBe("fakeGroupName");
    });

    it.each([
      ["an absent property", "einddatum"],
      ["a misspelled property", "eindatum"],
      ["an empty list", "besluiten[].identificatie"],
    ])("should leave the input untouched for %s", (_name, veld) => {
      const { component, taakdata } = seed(gegevensInputComponent(veld));

      expect(component.defaultValue).toBeUndefined();
      expect(taakdata).toEqual({});
    });

    it("should never seed the no-value marker, which a date picker cannot parse", () => {
      const { component, taakdata } = seed(
        gegevensInputComponent("einddatum", { formaat: "datum" }),
      );

      expect(component.defaultValue).not.toBe(NO_VALUE_MARK);
      expect(JSON.stringify(taakdata)).not.toContain(NO_VALUE_MARK);
    });

    it("should not seed a list whose elements all hold no value", () => {
      const { component, taakdata } = seed(
        gegevensInputComponent("kenmerken[].bron"),
        { source: { ...zaak, kenmerken: [{ bron: "" }, { bron: null }] } },
      );

      expect(component.defaultValue).toBeUndefined();
      expect(taakdata).toEqual({});
    });

    it.each([
      ["a datetime field", { type: "datetime" }],
      ["a field carrying a calendar widget", { widget: { type: "calendar" } }],
    ])(
      "should refuse a format on %s, which parses its own value",
      (_name, shape) => {
        const { component } = seed({
          ...gegevensInputComponent("startdatum", { formaat: "datum" }),
          ...shape,
        });

        expect(component.html).toContain("parses its own value");
        expect(component.defaultValue).toBeUndefined();
      },
    );

    it("should seed a date picker with the raw value", () => {
      const { component } = seed({
        ...gegevensInputComponent("startdatum"),
        type: "datetime",
      });

      expect(component.defaultValue).toBe("2026-08-24");
    });

    it("should join a list of values", () => {
      const { component } = seed(gegevensInputComponent("indicaties"));

      expect(component.defaultValue).toBe("OPSCHORTING, VERLENGD");
    });

    it("should read a property of every element of a list", () => {
      const { component } = seed(gegevensInputComponent("kenmerken[].kenmerk"));

      expect(component.defaultValue).toBe("fakeKenmerk1, fakeKenmerk2");
    });

    it("should say which property is missing when no path is declared", () => {
      const { component } = seed(gegevensInputComponent(""));

      expect(component.html).toContain("Missing ZAC_VELD property");
    });

    it.each([
      [
        "a path that reads on through a single value",
        "identificatie.jaar",
        "holds a single value",
      ],
      [
        "a path that stops on an object",
        "zaaktype",
        "holds an object, not a single value",
      ],
      [
        "a path that stops on a list of objects",
        "kenmerken",
        "holds a list of objects",
      ],
      [
        "list syntax on something that is not a list",
        "identificatie[]",
        "is not a list",
      ],
    ])("should report %s in place of the field", (_name, veld, message) => {
      const { component } = seed(gegevensInputComponent(veld));

      expect(component.html).toContain(message);
    });

    it.each([
      ["datum", "omschrijving", "is not a date"],
      ["jaNee", "identificatie", "is not a yes or a no"],
    ])(
      "should report %s on %s instead of seeding the value unformatted",
      (formaat, veld, reason) => {
        const { component } = seed(gegevensInputComponent(veld, { formaat }));

        expect(component.html).toContain(
          `ZAC_FORMAAT &quot;${formaat}&quot; cannot be applied`,
        );
        expect(component.html).toContain(reason);
        expect(component.defaultValue).toBeUndefined();
      },
    );

    it("should report the available format functions when the requested one does not exist", () => {
      const { component } = seed(
        gegevensInputComponent("startdatum", { formaat: "dutchDate" }),
      );

      expect(component.html).toContain(
        "Unknown ZAC_FORMAAT &quot;dutchDate&quot;. Available: datum, jaNee",
      );
    });

    it("should escape a message so a form value in it cannot inject markup", () => {
      const { component } = seed(
        gegevensInputComponent("omschrijving", { formaat: "datum" }),
        { source: { ...zaak, omschrijving: "<img src=x onerror=alert(1)>" } },
      );

      expect(component.html).not.toContain("<img");
      expect(component.html).toContain("&lt;img");
    });
  });
});
