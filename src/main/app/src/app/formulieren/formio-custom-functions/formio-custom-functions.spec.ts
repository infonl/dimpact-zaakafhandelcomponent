/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { Evaluator } from "@formio/core";
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
  });
});
