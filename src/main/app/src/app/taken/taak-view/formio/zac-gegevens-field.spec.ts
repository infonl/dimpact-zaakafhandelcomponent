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
  taakGegevensComponent,
  zaak,
  zaakGegevensComponent,
} from "./formio-setup-service.test-fixtures";
import { initializeGegevensField } from "./zac-gegevens-field";

const NO_VALUE_MARK = "—";

const translateService = {
  instant: (key: string) => key,
} as unknown as TranslateService;

const taakWithAssignees = {
  ...taak,
  groep: { id: "fakeGroupId", naam: "fakeGroupName" },
  behandelaar: { id: "fakeUserId", naam: "fakeUserName" },
} as GeneratedType<"RestTask">;

describe(initializeGegevensField.name, () => {
  describe("a gegevens field with ZAC_INVOER", () => {
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
        source: taakWithAssignees,
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
        {
          source: { ...zaak, kenmerken: [{ bron: "" }, { bron: null }] },
        },
      );

      expect(component.defaultValue).toBeUndefined();
      expect(taakdata).toEqual({});
    });

    it("should refuse a table format, which cannot go into an input", () => {
      const { component } = seed(
        gegevensInputComponent("zaakdata", { formaat: "tabel" }),
      );

      expect(component.html).toContain("cannot be put into an input");
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

    it("should report an unresolvable path in place of the field", () => {
      const { component } = seed(gegevensInputComponent("identificatie.jaar"));

      expect(component.html).toContain("holds a single value");
    });
  });

  describe("a gegevens field reading the taak", () => {
    function render(veld: string, options?: { formaat?: string }) {
      const component = taakGegevensComponent(veld, options);
      initializeGegevensField(
        component,
        taakWithAssignees,
        "Taak",
        taakWithAssignees,
        translateService,
      );
      return component;
    }

    it.each([
      ["naam", "test-taak"],
      ["status", "TOEGEKEND"],
      ["groep.naam", "fakeGroupName"],
      ["behandelaar.naam", "fakeUserName"],
      ["behandelaar.id", "fakeUserId"],
    ])("should render the taak property %s", (veld, value) => {
      expect(render(veld).html).toContain(value);
    });

    it("should name the taak as the source in an error", () => {
      expect(render("naam.eerste").html).toContain(
        "Taak property &quot;naam&quot;",
      );
    });

    it("should format a taak date with the shared format function", () => {
      expect(render("fataledatum", { formaat: "datum" }).html).toMatch(
        /\d{2}-\d{2}-\d{4}/,
      );
    });
  });

  describe("a gegevens field reading the zaak", () => {
    function render(
      veld: string,
      options?: { label?: string; formaat?: string },
      source: object = zaak,
    ) {
      const component = zaakGegevensComponent(veld, options);
      initializeGegevensField(
        component,
        source,
        "Zaak",
        taak,
        translateService,
      );
      return component;
    }

    it.each([
      ["uuid", "test-zaakUuid"],
      ["identificatie", "ZAAK-2026-0000000835"],
      ["omschrijving", "test-zaak-omschrijving"],
      ["communicatiekanaal", "Medewerkersportaal"],
      ["zaaktype.uuid", "test-zaaktype-uuid"],
      ["zaaktype.omschrijving", "test-zaaktypeOmschrijving"],
      ["groep.naam", "fakeGroupName"],
      ["behandelaar.naam", "fakeUserName"],
      ["status.naam", "In behandeling"],
    ])("should render the zaak property %s", (veld, value) => {
      expect(render(veld).html).toContain(value);
    });

    it("should separate the label from the value with a colon", () => {
      const component = render("identificatie", { label: "Zaaknummer" });

      expect(component.html).toContain("Zaaknummer: ");
      expect(component.label).toBe("");
    });

    it("should not render a stray colon when the field has no label", () => {
      expect(render("identificatie", { label: "" }).html).toContain(
        'class="zac-gegevens-label fw-medium"></span>',
      );
    });

    it("should render a value as the zaak holds it when no format is asked for", () => {
      expect(render("startdatum").html).toContain("2026-08-24");
      expect(render("isOpgeschort").html).toContain("false");
    });

    it.each([
      ["datum", "startdatum", "24-08-2026"],
      ["jaNee", "isOpgeschort", "actie.nee"],
      ["jaNee", "isOpen", "actie.ja"],
    ])(
      "should wrap the %s format function around %s",
      (formaat, veld, expected) => {
        expect(render(veld, { formaat }).html).toContain(expected);
      },
    );

    it("should report the available format functions when the requested one does not exist", () => {
      expect(render("startdatum", { formaat: "dutchDate" }).html).toContain(
        "Unknown ZAC_FORMAAT &quot;dutchDate&quot;. Available: datum, jaNee",
      );
    });

    it("should mark a property that holds no value, rather than leave the field blank", () => {
      const component = render("omschrijving", undefined, {
        ...zaak,
        omschrijving: undefined,
      });

      expect(component.html).toContain(
        `<span class="zac-gegevens-value">${NO_VALUE_MARK}</span>`,
      );
    });

    it("should not become part of the submission, so completing the task cannot write it back", () => {
      const component = render("identificatie");

      expect(component.input).toBe(false);
      expect(component.type).toBe("content");
      expect(taak.taakdata).not.toHaveProperty(component.key);
    });

    it("should render nothing for a misspelled property, because the api omits empty ones", () => {
      expect(render("kommunikatiekanaal").html).toContain(
        `<span class="zac-gegevens-value">${NO_VALUE_MARK}</span>`,
      );
    });

    it("should report a path that reads on through a single value, in place of the field", () => {
      const component = render("identificatie.jaar");

      expect(component.html).toContain(
        "Zaak property &quot;identificatie&quot; holds a single value",
      );
      expect(component.html).toContain("&quot;jaar&quot; cannot be read");
    });

    it("should say that a path stopping at an object holds no single value", () => {
      expect(render("zaaktype").html).toContain(
        "holds an object, not a single value.",
      );
    });

    it("should join a list of values", () => {
      expect(render("indicaties").html).toContain("OPSCHORTING, VERLENGD");
    });

    it("should read a property of every element of a list", () => {
      expect(render("kenmerken[].kenmerk").html).toContain(
        "fakeKenmerk1, fakeKenmerk2",
      );
    });

    it("should format every element of a list", () => {
      const component = render(
        "kenmerken[].datum",
        { formaat: "datum" },
        {
          ...zaak,
          kenmerken: [{ datum: "2026-08-24" }, { datum: "2026-01-02" }],
        },
      );

      expect(component.html).toContain("24-08-2026, 02-01-2026");
    });

    it("should mark an empty list as holding no value", () => {
      expect(render("besluiten[].identificatie").html).toContain(
        `<span class="zac-gegevens-value">${NO_VALUE_MARK}</span>`,
      );
    });

    it("should treat a list holding only empty elements as holding no value", () => {
      const component = render("kenmerken[].bron", undefined, {
        ...zaak,
        kenmerken: [{ bron: null }, { bron: "" }],
      });

      expect(component.html).toContain(
        `<span class="zac-gegevens-value">${NO_VALUE_MARK}</span>`,
      );
    });

    it("should suggest a property of the element for a list of objects", () => {
      const component = render("kenmerken");

      expect(component.html).toContain("holds a list of objects");
      expect(component.html).toContain("kenmerken[].bron");
    });

    it("should report list syntax used on something that is not a list", () => {
      expect(render("identificatie[]").html).toContain("is not a list");
    });

    it("should escape a zaak value so it cannot inject markup", () => {
      const component = render("omschrijving", undefined, {
        ...zaak,
        omschrijving: "<img src=x onerror=alert(1)>",
      });

      expect(component.html).not.toContain("<img");
      expect(component.html).toContain("&lt;img");
    });

    it.each([
      ["reading a value", undefined],
      ["rendering a table", "tabel"],
    ])(
      "should say which property is missing when no path is declared while %s",
      (_name, formaat) => {
        expect(render("", { formaat }).html).toContain(
          "Missing ZAC_VELD property",
        );
      },
    );

    describe("the tabel format", () => {
      function renderTable(veld: string, zaakdata?: Record<string, unknown>) {
        return render(
          veld,
          { formaat: "tabel" },
          zaakdata ? { ...zaak, zaakdata } : zaak,
        );
      }

      it("should render every key of an object as a row", () => {
        const component = renderTable("zaakdata", {
          NF_Uren: "8",
          TA_Toelichting: "spoed",
        });

        expect(component.html).toContain("<td>8</td>");
        expect(component.html).toContain("<td>spoed</td>");
        expect(component.html).toContain("<code>NF_Uren</code>");
      });

      it("should sort the keys so the same object always reads the same way", () => {
        const component = renderTable("zaakdata", { zebra: "1", alpha: "2" });

        expect(component.html!.indexOf("alpha")).toBeLessThan(
          component.html!.indexOf("zebra"),
        );
      });

      it("should count the entries of a nested object and the elements of a list", () => {
        const component = renderTable("zaakdata", {
          rows: [1, 2, 3],
          nested: { a: 1, b: 2 },
        });

        expect(component.html).toContain("<td>[3]</td>");
        expect(component.html).toContain("<td>{2}</td>");
      });

      it("should mark an object with no keys as holding no value", () => {
        const component = renderTable("zaakdata", {});

        expect(component.html).toContain(NO_VALUE_MARK);
        expect(component.html).not.toContain("<table");
      });

      it("should render the label as a heading above the table", () => {
        const component = render(
          "zaakdata",
          { formaat: "tabel", label: "Process data" },
          { ...zaak, zaakdata: { a: "1" } },
        );

        expect(component.html).toContain('<h4 class="mb-1">Process data</h4>');
      });

      it("should escape a key and a value so neither can inject markup", () => {
        const component = renderTable("zaakdata", {
          "<b>k</b>": "<img src=x onerror=alert(1)>",
        });

        expect(component.html).not.toContain("<img");
        expect(component.html).not.toContain("<b>");
        expect(component.html).toContain("<code>&lt;b&gt;k&lt;/b&gt;</code>");
        expect(component.html).toContain("&lt;img");
      });

      it("should refuse to tabulate a single value, and say what to drop", () => {
        const component = renderTable("identificatie");

        expect(component.html).toContain("has no keys to tabulate");
        expect(component.html).toContain("ZAC_FORMAAT");
      });
    });
  });
});
