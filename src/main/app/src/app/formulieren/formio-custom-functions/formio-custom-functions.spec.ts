/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { LOCALE_ID } from "@angular/core";
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
        { provide: LOCALE_ID, useValue: "nl-NL" },
        {
          provide: InformatieObjectenService,
          useValue: informatieObjectenService,
        },
      ],
    });

    service = TestBed.inject(FormioCustomFunctions);
  });

  afterEach(() => jest.restoreAllMocks());

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
    };
    const taak = { naam: "test-taak", groep: { naam: "fakeGroupName" } };

    async function interpolate(template: string, source: object = zaak) {
      const context = await service.prepareFormContext(
        { components: [{ type: "content", html: template }] },
        {},
        source,
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

    it("should leave the taak data it already exposed reachable", async () => {
      const context = await service.prepareFormContext(
        { components: [] },
        { NF_Uren: "8" },
        zaak,
        taak,
      );

      expect(context.NF_Uren).toBe("8");
    });

    it("should keep a value that is not a plain object intact, rather than empty it", async () => {
      const registratiedatum = new Date("2026-08-24T00:00:00.000Z");

      const context = await service.prepareFormContext(
        { components: [] },
        {},
        { registratiedatum },
      );

      expect(
        (context.zaak as { registratiedatum: Date }).registratiedatum,
      ).toEqual(registratiedatum);
    });

    describe("a property that is absent", () => {
      it.each([
        ["one level", "{{ zaak.behandelaar }}"],
        ["through an absent object", "{{ zaak.behandelaar.naam }}"],
        ["several levels deep", "{{ zaak.resultaat.resultaattype.naam }}"],
      ])(
        "should render nothing for a property absent %s",
        async (_description, template) => {
          expect(await interpolate(template, { identificatie: "ZAAK-1" })).toBe(
            "",
          );
        },
      );

      it.each([
        ["a null value", { behandelaar: null }],
        ["an absent key", {}],
      ])(
        "should read on through %s without throwing",
        async (_description, source) => {
          expect(await interpolate("{{ zaak.behandelaar.naam }}", source)).toBe(
            "",
          );
        },
      );

      it("should still read a property that is present", async () => {
        expect(
          await interpolate("{{ zaak.behandelaar.naam }}", {
            behandelaar: { naam: "fakeUserName" },
          }),
        ).toBe("fakeUserName");
      });

      it("should let a taak without a groep be read as plain JavaScript, the way customDefaultValue is run", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
          { naam: "test-taak", groep: null },
        );
        const readTaak = context.taak as { groep: { naam: string } };

        expect(String(readTaak.groep.naam)).toBe("");
      });

      it("should report a property the zaak does not have, naming the full path", async () => {
        const consoleWarn = jest
          .spyOn(console, "warn")
          .mockImplementation(() => {});

        await interpolate("{{ zaak.kommunikatiekanaal }}", {
          communicatiekanaal: "Medewerkersportaal",
        });

        expect(consoleWarn).toHaveBeenCalledWith(
          expect.stringContaining(
            '"zaak.kommunikatiekanaal" is not a property',
          ),
        );
      });

      it("should name the path through a nested object", async () => {
        const consoleWarn = jest
          .spyOn(console, "warn")
          .mockImplementation(() => {});

        await interpolate("{{ zaak.zaaktype.omschryving }}", {
          zaaktype: { omschrijving: "Melding" },
        });

        expect(consoleWarn).toHaveBeenCalledWith(
          expect.stringContaining('"zaak.zaaktype.omschryving"'),
        );
      });

      it("should stay quiet for a property that is present but empty, which is ordinary data", async () => {
        const consoleWarn = jest
          .spyOn(console, "warn")
          .mockImplementation(() => {});

        await interpolate("{{ zaak.behandelaar.naam }}", {
          behandelaar: null,
        });

        expect(consoleWarn).not.toHaveBeenCalledWith(
          expect.stringContaining("zaak.behandelaar"),
        );
      });

      it("should stay quiet for an index past the end of a list", async () => {
        const consoleWarn = jest
          .spyOn(console, "warn")
          .mockImplementation(() => {});

        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { indicaties: ["OPSCHORTING"] },
        );
        const readableZaak = context.zaak as { indicaties: string[] };

        expect(String(readableZaak.indicaties[999])).toBe("");
        expect(consoleWarn).not.toHaveBeenCalled();
      });

      it("should report the same property once, however often the form re-renders", async () => {
        const consoleWarn = jest
          .spyOn(console, "warn")
          .mockImplementation(() => {});

        await interpolate("{{ zaak.eenmaligGemeld }}", {});
        await interpolate("{{ zaak.eenmaligGemeld }}", {});

        expect(
          consoleWarn.mock.calls.filter(([message]) =>
            String(message).includes("zaak.eenmaligGemeld"),
          ),
        ).toHaveLength(1);
      });

      it("should not make a value look like a promise, which would hang an await", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
        );

        expect((context.zaak as { then?: unknown }).then).toBeUndefined();
      });

      it("should leave a real list iterable, so spreading it still works", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { indicaties: ["OPSCHORTING", "VERLENGD"] },
        );
        const readableZaak = context.zaak as { indicaties: string[] };

        expect([...readableZaak.indicaties]).toEqual([
          "OPSCHORTING",
          "VERLENGD",
        ]);
      });
    });

    describe("markup in a value", () => {
      it.each([
        ["a plain tag", "<b>x</b>", "x"],
        ["a script tag", "<script>alert(1)</script>", "alert(1)"],
        ["an image with a handler", "<img src=x onerror=alert(1)>", ""],
        [
          "a tag spliced together by removing another",
          "<scr<x>ipt>alert(1)</script>",
          "iptalert(1)",
        ],
        [
          "a doubled opening bracket",
          "<<script>script>alert(1)",
          "scriptalert(1)",
        ],
        ["a stray bracket", "5 < 6", "5  6"],
      ])(
        "should leave no markup for %s",
        async (_description, value, expected) => {
          const context = await service.prepareFormContext(
            { components: [] },
            {},
            { omschrijving: value },
          );
          const stripped = (context.zaak as { omschrijving: string })
            .omschrijving;

          expect(stripped).toBe(expected);
          expect(stripped).not.toContain("<");
          expect(stripped).not.toContain(">");
        },
      );

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
        async (_description, value) => {
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
    });

    describe("the formatters", () => {
      it.each([
        ["{{ ZAC_opmaakDatum(zaak.startdatum) }}", "24\u201108\u20112026"],
        ["{{ ZAC_opmaakBoolean(zaak.isOpen) }}", "actie.ja"],
        ["{{ ZAC_opmaakBoolean(zaak.isOpgeschort) }}", "actie.nee"],
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
        ["a null value", null],
        ["an empty string", ""],
        ["an absent property", undefined],
      ])(
        "should render nothing for %s, so it does not read as a deliberate Nee",
        async (_description, value) => {
          const context = await service.prepareFormContext(
            { components: [] },
            {},
            { isVerlengd: value },
          );

          expect(
            Evaluator.interpolate(
              "{{ ZAC_opmaakBoolean(zaak.isVerlengd) }}",
              context,
            ),
          ).toBe("");
        },
      );

      it("should render nothing for a boolean the zaak does not have at all", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
        );

        expect(
          Evaluator.interpolate(
            "{{ ZAC_opmaakBoolean(zaak.isVerlengd) }}",
            context,
          ),
        ).toBe("");
      });

      it.each([
        ["a string", "misschien", "misschien"],
        ["a number", 1, "1"],
      ])(
        "should render %s unchanged, because it is not a boolean to answer",
        async (_description, value, expected) => {
          const context = await service.prepareFormContext(
            { components: [] },
            {},
            { isVerlengd: value },
          );

          expect(
            Evaluator.interpolate(
              "{{ ZAC_opmaakBoolean(zaak.isVerlengd) }}",
              context,
            ),
          ).toBe(expected);
        },
      );

      it.each([
        ['{{ ZAC_opmaakBoolean(zaak.isOpen, "Open", "Gesloten") }}', "Open"],
        [
          '{{ ZAC_opmaakBoolean(zaak.isOpgeschort, "Opgeschort", "Loopt") }}',
          "Loopt",
        ],
        ['{{ ZAC_opmaakBoolean(zaak.isOpen, "Open") }}', "Open"],
        [
          '{{ ZAC_opmaakBoolean(zaak.isOpgeschort, "Opgeschort") }}',
          "actie.nee",
        ],
        ['{{ ZAC_opmaakBoolean(zaak.isOpen, "actie.ja") }}', "actie.ja"],
      ])("should resolve %s to %s", async (template, expected) => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { isOpen: true, isOpgeschort: false },
        );

        expect(Evaluator.interpolate(template, context)).toBe(expected);
      });

      it("should render nothing for an absent property even when labels are given", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
        );

        expect(
          Evaluator.interpolate(
            '{{ ZAC_opmaakBoolean(zaak.isVerlengd, "Verlengd", "Niet verlengd") }}',
            context,
          ),
        ).toBe("");
      });

      it.each([
        [
          '{{ ZAC_opmaakLijst(zaak.kenmerken, "kenmerk") }}',
          "fakeKenmerk1, fakeKenmerk2",
        ],
        ["{{ ZAC_opmaakLijst(zaak.indicaties) }}", "OPSCHORTING, VERLENGD"],
        ["{{ ZAC_opmaakLijst(zaak.besluiten) }}", ""],
      ])("should resolve %s to %s", async (template, expected) => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {
            kenmerken: [
              { kenmerk: "fakeKenmerk1" },
              { kenmerk: "fakeKenmerk2" },
            ],
            indicaties: ["OPSCHORTING", "VERLENGD"],
            besluiten: [],
          },
        );

        expect(Evaluator.interpolate(template, context)).toBe(expected);
      });

      it("should keep lijst working over a list that is absent", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {},
        );

        expect(
          Evaluator.interpolate(
            '{{ ZAC_opmaakLijst(zaak.kenmerken, "kenmerk") }}',
            context,
          ),
        ).toBe("");
      });

      it.each([
        [
          "a value",
          "{{ ZAC_opmaakLegeWaarde(zaak.omschrijving) }}",
          "test-omschrijving",
        ],
        [
          "an empty string",
          "{{ ZAC_opmaakLegeWaarde(zaak.toelichting) }}",
          "-",
        ],
        [
          "an absent property",
          "{{ ZAC_opmaakLegeWaarde(zaak.einddatum) }}",
          "-",
        ],
        ["an empty list", "{{ ZAC_opmaakLegeWaarde(zaak.besluiten) }}", "-"],
        [
          "a value, which leaves a given placeholder unused",
          '{{ ZAC_opmaakLegeWaarde(zaak.omschrijving, "Onbekend") }}',
          "test-omschrijving",
        ],
        [
          "an empty string with a placeholder of its own",
          '{{ ZAC_opmaakLegeWaarde(zaak.toelichting, "Onbekend") }}',
          "Onbekend",
        ],
        [
          "an absent property with a placeholder of its own",
          '{{ ZAC_opmaakLegeWaarde(zaak.einddatum, "Nog niet bekend") }}',
          "Nog niet bekend",
        ],
        [
          "an empty list with a placeholder given as a translation key",
          '{{ ZAC_opmaakLegeWaarde(zaak.besluiten, "actie.nee") }}',
          "actie.nee",
        ],
      ])("should render %s", async (_description, template, expected) => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          {
            omschrijving: "test-omschrijving",
            toelichting: "",
            besluiten: [],
          },
        );

        expect(Evaluator.interpolate(template, context)).toBe(expected);
      });

      it("should leave a value the date pipe cannot read alone", async () => {
        const context = await service.prepareFormContext(
          { components: [] },
          {},
          { omschrijving: "geen datum" },
        );

        expect(
          Evaluator.interpolate(
            "{{ ZAC_opmaakDatum(zaak.omschrijving) }}",
            context,
          ),
        ).toBe("geen datum");
      });
    });
  });
});
