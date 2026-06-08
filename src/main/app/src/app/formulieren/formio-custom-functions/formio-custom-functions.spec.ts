/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
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

    it("should fetch each document by UUID", async () => {
      await service.prepareFormContext(formWithFunction("ZAAK_Docs"), {
        ZAAK_Docs: [UUID_A],
      });

      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).toHaveBeenCalledWith(UUID_A);
    });
  });
});
