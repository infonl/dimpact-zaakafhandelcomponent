/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { Evaluator } from "@formio/core";
import { TranslateModule } from "@ngx-translate/core";
import { of, throwError } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { FormioCustomFunctions } from "./formio-custom-functions";

const mockDocument = (titel: string | null) =>
  of({ titel } as GeneratedType<"RestEnkelvoudigInformatieobject">);

const UUID_A = "aaaaaaaa-0000-0000-0000-000000000001";
const UUID_B = "bbbbbbbb-0000-0000-0000-000000000002";
const UUID_C = "cccccccc-0000-0000-0000-000000000003";

const formWithFunction = (field: string) => ({
  components: [
    {
      html: `<p>{{ ZAC_getDocumentTitles(${field}) }}</p>`,
      type: "content",
    },
  ],
});

describe(FormioCustomFunctions.name, () => {
  let service: FormioCustomFunctions;
  let informatieObjectenService: jest.Mocked<
    Pick<InformatieObjectenService, "readEnkelvoudigInformatieobject">
  >;

  beforeEach(() => {
    informatieObjectenService = {
      readEnkelvoudigInformatieobject: jest.fn(),
    };

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [
        {
          provide: InformatieObjectenService,
          useValue: informatieObjectenService,
        },
      ],
    });

    service = TestBed.inject(FormioCustomFunctions);
  });

  describe(FormioCustomFunctions.prototype.prepareFormContext.name, () => {
    beforeEach(() => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        mockDocument("Document A"),
      );
    });

    it("should spread taakdata as top-level keys in the context", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A] },
      );

      expect(context["ZAAK_Docs"]).toEqual([UUID_A]);
    });

    it("should register ZAC_getDocumentTitles as a function in the context", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A] },
      );

      expect(typeof context["ZAC_getDocumentTitles"]).toBe("function");
    });

    it("should return the title string when ZAC_getDocumentTitles is called with UUIDs", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A])).toBe("Document A");
    });

    it("should return empty string when called with an empty array", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([])).toBe("");
    });

    it("should return empty string when the taakdata field is missing", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Missing"),
        {},
      );
      const fn = context["ZAC_getDocumentTitles"] as (uuids: unknown) => string;

      expect(fn(undefined)).toBe("");
    });

    it("should format two documents with 'en'", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) => mockDocument(uuid === UUID_A ? "Document A" : "Document B"),
      );

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A, UUID_B] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A, UUID_B])).toBe("Document A en Document B");
    });

    it("should format three documents with commas and 'en'", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) =>
          mockDocument(
            uuid === UUID_A
              ? "Document A"
              : uuid === UUID_B
                ? "Document B"
                : "Document C",
          ),
      );

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A, UUID_B, UUID_C] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A, UUID_B, UUID_C])).toBe(
        "Document A, Document B en Document C",
      );
    });

    it("should fall back to UUID when document fetch fails", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        throwError(() => new Error("Not found")),
      );

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A])).toBe(UUID_A);
    });

    it("should fall back to UUID when document has no titel", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        mockDocument(null),
      );

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [UUID_A] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A])).toBe(UUID_A);
    });

    it("should pre-fetch titles for all fields when the same function appears multiple times", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) => mockDocument(uuid === UUID_A ? "Document A" : "Document B"),
      );
      const formWithTwoCalls = {
        components: [
          {
            html: "{{ ZAC_getDocumentTitles(ZAAK_Docs_A) }}",
            type: "content",
          },
          {
            html: "{{ ZAC_getDocumentTitles(ZAAK_Docs_B) }}",
            type: "content",
          },
        ],
      };

      const context = await service.prepareFormContext(formWithTwoCalls, {
        ZAAK_Docs_A: [UUID_A],
        ZAAK_Docs_B: [UUID_B],
      });
      const fn = context["ZAC_getDocumentTitles"] as (
        uuids: string[],
      ) => string;

      expect(fn([UUID_A])).toBe("Document A");
      expect(fn([UUID_B])).toBe("Document B");
    });

    it("should render titles for datagrid rows as well as plain UUIDs", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) => mockDocument(uuid === UUID_A ? "Document A" : "Document B"),
      );
      const rows = [
        { selected: true, titel: "stale", uuid: UUID_A },
        { selected: true, titel: "stale", uuid: UUID_B },
      ];

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: rows },
      );
      const fn = context["ZAC_getDocumentTitles"] as (rows: unknown) => string;

      expect(fn(rows)).toBe("Document A en Document B");
    });

    it("should leave out datagrid rows that were unticked", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) => mockDocument(uuid === UUID_A ? "Document A" : "Document B"),
      );
      const rows = [
        { selected: true, uuid: UUID_A },
        { selected: false, uuid: UUID_B },
      ];

      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: rows },
      );
      const fn = context["ZAC_getDocumentTitles"] as (rows: unknown) => string;

      expect(fn(rows)).toBe("Document A");
      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).not.toHaveBeenCalledWith(UUID_B);
    });

    it("should accept a single UUID that is not wrapped in an array", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: UUID_A },
      );
      const fn = context["ZAC_getDocumentTitles"] as (uuid: unknown) => string;

      expect(fn(UUID_A)).toBe("Document A");
    });

    it("should ignore entries that carry no uuid", async () => {
      const context = await service.prepareFormContext(
        formWithFunction("ZAAK_Docs"),
        { ZAAK_Docs: [{ selected: true, titel: "no uuid here" }, null, 42] },
      );
      const fn = context["ZAC_getDocumentTitles"] as (rows: unknown) => string;

      expect(fn([{ selected: true, titel: "no uuid here" }, null, 42])).toBe(
        "",
      );
      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).not.toHaveBeenCalled();
    });

    it("should fetch a document only once when the same UUID occurs in several fields", async () => {
      const formWithTwoCalls = {
        components: [
          { html: "{{ ZAC_getDocumentTitles(ZAAK_Docs_A) }}", type: "content" },
          { html: "{{ ZAC_getDocumentTitles(ZAAK_Docs_B) }}", type: "content" },
        ],
      };

      await service.prepareFormContext(formWithTwoCalls, {
        ZAAK_Docs_A: [UUID_A, UUID_A],
        ZAAK_Docs_B: [{ selected: true, uuid: UUID_A }],
      });

      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).toHaveBeenCalledTimes(1);
    });

    it("should fetch each document by UUID", async () => {
      await service.prepareFormContext(formWithFunction("ZAAK_Docs"), {
        ZAAK_Docs: [UUID_A],
      });

      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).toHaveBeenCalledWith(UUID_A);
    });
  });

  describe("the zaak and the taak in the eval context", () => {
    const zaak = {
      identificatie: "ZAAK-2026-0000000835",
      zaaktype: { omschrijving: "Melding klein evenement" },
      indicaties: ["OPSCHORTING", "VERLENGD"],
      omschrijving: "<img src=x onerror=alert(1)>",
    };
    const taak = { naam: "test-taak", groep: { naam: "fakeGroupName" } };

    async function interpolate(template: string) {
      const context = await service.prepareFormContext(
        { components: [{ type: "content", html: template }] },
        {},
        zaak,
        taak,
      );
      return Evaluator.interpolate(template, context);
    }

    it.each([
      ["{{ zaak.identificatie }}", "ZAAK-2026-0000000835"],
      ["{{ zaak.zaaktype.omschrijving }}", "Melding klein evenement"],
      ["{{ taak.naam }}", "test-taak"],
      ["{{ taak.groep.naam }}", "fakeGroupName"],
    ])("should resolve %s to %s", async (template, expected) => {
      expect(await interpolate(template)).toBe(expected);
    });

    it("should compose two values into one sentence", async () => {
      expect(
        await interpolate(
          "Zaak {{ zaak.identificatie }} van {{ zaak.zaaktype.omschrijving }}",
        ),
      ).toBe("Zaak ZAAK-2026-0000000835 van Melding klein evenement");
    });

    it("should remove markup so a form cannot render what a user entered", async () => {
      const rendered = await interpolate("{{ zaak.omschrijving }}");

      expect(rendered).not.toContain("<img");
      expect(rendered).toBe("");
    });

    it("should remove markup nested in a list", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        { kenmerken: [{ bron: "<b>x</b>" }] },
      );

      expect(context.zaak).toEqual({ kenmerken: [{ bron: "x" }] });
    });

    it.each([
      ["an ampersand", "Jansen & Zn"],
      ["an apostrophe", "'s-Hertogenbosch"],
      ["a quote", 'de "grote" zaal'],
    ])(
      "should leave %s untouched, so a seeded input shows it as typed",
      async (_name, value) => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { omschrijving: value },
        );

        expect((context.zaak as { omschrijving: string }).omschrijving).toBe(
          value,
        );
      },
    );

    it("should leave the taak data it already exposed reachable", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        { NF_Uren: "8" },
        zaak,
        taak,
      );

      expect(context.NF_Uren).toBe("8");
    });

    it.each([
      ["{{ datum(zaak.startdatum) }}", "24-08-2026"],
      ["{{ jaNee(zaak.isOpen) }}", "actie.ja"],
      ["{{ jaNee(zaak.isOpgeschort) }}", "actie.nee"],
    ])("should format %s to %s", async (template, expected) => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          startdatum: "2026-08-24",
          isOpen: true,
          isOpgeschort: false,
        },
      );

      expect(Evaluator.interpolate(template, context)).toBe(expected);
    });

    it.each([
      ['{{ lijst(zaak.kenmerken, "kenmerk") }}', "fakeKenmerk1, fakeKenmerk2"],
      ["{{ lijst(zaak.indicaties) }}", "OPSCHORTING, VERLENGD"],
      ["{{ lijst(zaak.besluiten) }}", ""],
    ])("should resolve %s to %s", async (template, expected) => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          kenmerken: [{ kenmerk: "fakeKenmerk1" }, { kenmerk: "fakeKenmerk2" }],
          indicaties: ["OPSCHORTING", "VERLENGD"],
          besluiten: [],
        },
      );

      expect(Evaluator.interpolate(template, context)).toBe(expected);
    });

    it("should render every key of an object with sleutels", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: { NF_Uren: "8", rows: [1, 2, 3], nested: { a: 1, b: 2 } },
        },
      );
      const rendered = Evaluator.interpolate(
        "{{ sleutels(zaak.zaakdata) }}",
        context,
      );

      expect(rendered).toContain("<code>zaak.zaakdata.NF_Uren</code>");
      expect(rendered).toContain("<td>8</td>");
      expect(rendered).toContain("<td>[3]</td>");
      expect(rendered).toContain("<td>{2}</td>");
    });

    it("should sort the keys so the same object always reads the same way", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: { zebra: "1", alpha: "2" },
        },
      );
      const rendered = Evaluator.interpolate(
        "{{ sleutels(zaak.zaakdata) }}",
        context,
      );

      expect(rendered.indexOf("alpha")).toBeLessThan(rendered.indexOf("zebra"));
    });

    it("should say so for an object with no keys, rather than render an empty box", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: {},
        },
      );
      const rendered = Evaluator.interpolate(
        "{{ sleutels(zaak.zaakdata) }}",
        context,
      );

      expect(rendered).toContain("no keys");
      expect(rendered).not.toContain("<table");
    });

    it("should box the table with a caption naming where the keys come from", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: { NF_Uren: "8" },
        },
      );
      const rendered = Evaluator.interpolate(
        '{{ sleutels(zaak.zaakdata, "Flowable process variables") }}',
        context,
      );

      expect(rendered).toContain('<div class="card mb-2">');
      expect(rendered).toContain("Flowable process variables");
      expect(rendered).toContain("<code>zaak.zaakdata.NF_Uren</code>");
    });

    it("should spell every key out in full, so a row can be copied into an expression", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: {
            zaakIdentificatie: "ZAAK-2026-0000000248",
            zaaktypeUUID: "26076928-ce07-4d5d-8638-c2d276f6caca",
          },
        },
      );
      const rendered = Evaluator.interpolate(
        "{{ sleutels(zaak.zaakdata) }}",
        context,
      );

      expect(rendered).toContain(
        "<code>zaak.zaakdata.zaakIdentificatie</code>",
      );
      expect(rendered).toContain("<code>zaak.zaakdata.zaaktypeUUID</code>");
      expect(rendered).not.toContain("<code>zaakIdentificatie</code>");
    });

    it("should name the taak in the keys of a taak object", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        undefined,
        { taakinformatie: { toelichting: "spoed" } },
      );

      expect(
        Evaluator.interpolate("{{ sleutels(taak.taakinformatie) }}", context),
      ).toContain("<code>taak.taakinformatie.toelichting</code>");
    });

    it("should leave the keys bare when the value did not come from the zaak or the taak", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {},
      );

      expect(
        Evaluator.interpolate(
          "{{ sleutels(lijst(zaak.indicaties)) }}",
          context,
        ),
      ).toContain("no keys");
    });

    it("should leave the caption out when none is given", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        {},
        {
          zaakdata: { NF_Uren: "8" },
        },
      );
      const rendered = Evaluator.interpolate(
        "{{ sleutels(zaak.zaakdata) }}",
        context,
      );

      expect(rendered).toContain('<div class="card mb-2">');
      expect(rendered).not.toContain("card-header");
    });

    describe("a property that is absent", () => {
      async function render(template: string, source: object) {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          source,
        );
        return Evaluator.interpolate(template, context);
      }

      it.each([
        ["one level", "{{ zaak.behandelaar }}"],
        ["through an absent object", "{{ zaak.behandelaar.naam }}"],
        ["several levels deep", "{{ zaak.resultaat.resultaattype.naam }}"],
      ])(
        "should render nothing for a property absent %s",
        async (_n, template) => {
          expect(await render(template, { identificatie: "ZAAK-1" })).toBe("");
        },
      );

      it("should not throw where reading through an absent object used to crash", async () => {
        await expect(
          render("{{ zaak.behandelaar.naam }}", { behandelaar: null }),
        ).resolves.toBe("");
      });

      it.each([
        ["a null value", { behandelaar: null }],
        ["an absent key", {}],
      ])("should read on through %s without throwing", async (_n, source) => {
        expect(await render("{{ zaak.behandelaar.naam }}", source)).toBe("");
      });

      it("should not make a value look like a promise, which would hang an await", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
        );

        await expect(Promise.resolve(context.zaak)).resolves.toBeDefined();
      });

      it("should leave a real list iterable, so spreading it still works", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { indicaties: ["OPSCHORTING", "VERLENGD"] },
        );
        const zaak = context.zaak as { indicaties: string[] };

        expect([...zaak.indicaties]).toEqual(["OPSCHORTING", "VERLENGD"]);
      });

      it("should report a property the zaak does not have, naming the full path", async () => {
        const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

        await render("{{ zaak.kommunikatiekanaal }}", {
          communicatiekanaal: "Medewerkersportaal",
        });

        expect(warn).toHaveBeenCalledWith(
          expect.stringContaining(
            '"zaak.kommunikatiekanaal" is not a property',
          ),
        );
      });

      it("should name the path through a nested object", async () => {
        const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

        await render("{{ zaak.zaaktype.omschryving }}", {
          zaaktype: { omschrijving: "Melding" },
        });

        expect(warn).toHaveBeenCalledWith(
          expect.stringContaining('"zaak.zaaktype.omschryving"'),
        );
      });

      it("should stay quiet for a property that is present but empty, which is ordinary data", async () => {
        const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

        await render("{{ zaak.behandelaar.naam }}", { behandelaar: null });

        expect(warn).not.toHaveBeenCalledWith(
          expect.stringContaining("zaak.behandelaar"),
        );
      });

      it("should stay quiet for an index past the end of a list", async () => {
        const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

        await render("{{ lijst(zaak.indicaties) }}", { indicaties: ["A"] });

        expect(warn).not.toHaveBeenCalled();
      });

      it("should report the same property once, however often the form re-renders", async () => {
        const warn = jest.spyOn(console, "warn").mockImplementation(() => {});

        await render("{{ zaak.eenmaligGemeld }}", {});
        await render("{{ zaak.eenmaligGemeld }}", {});

        expect(
          warn.mock.calls.filter(([message]) =>
            String(message).includes("zaak.eenmaligGemeld"),
          ),
        ).toHaveLength(1);
      });
      it("should still read a property that is present", async () => {
        expect(
          await render("{{ zaak.behandelaar.naam }}", {
            behandelaar: { naam: "fakeUserName" },
          }),
        ).toBe("fakeUserName");
      });

      it("should keep lijst working over a list that is absent", async () => {
        expect(await render('{{ lijst(zaak.kenmerken, "kenmerk") }}', {})).toBe(
          "",
        );
      });

      it("should keep sleutels working over an object that is absent", async () => {
        expect(await render("{{ sleutels(zaak.zaakdata) }}", {})).toContain(
          "no keys",
        );
      });

      it("should keep a real list enumerable, so lijst still reads it", async () => {
        expect(
          await render('{{ lijst(zaak.kenmerken, "kenmerk") }}', {
            kenmerken: [{ kenmerk: "a" }, { kenmerk: "b" }],
          }),
        ).toBe("a, b");
      });
    });
  });
});
