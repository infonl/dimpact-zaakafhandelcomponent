/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { of, throwError } from "rxjs";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../shared/utils/generated-types";
import { ZakenService } from "../../zaken/zaken.service";
import {
  FormioCustomFunctions,
  KNOWN_FORMIO_FUNCTIONS,
} from "./formio-custom-functions";

const mockZaak = (zaakdata: Record<string, unknown>) =>
  of({ zaakdata } as GeneratedType<"RestZaak">);

const mockDocument = (titel: string | null) =>
  of({ titel } as GeneratedType<"RestEnkelvoudigInformatieobject">);

const ZAAK_UUID = "zaak-uuid-123";
const UUID_A = "aaaaaaaa-0000-0000-0000-000000000001";
const UUID_B = "bbbbbbbb-0000-0000-0000-000000000002";
const UUID_C = "cccccccc-0000-0000-0000-000000000003";

const formWithFunction = (field: string) => ({
  components: [
    { html: `<p>{{ getDocumentTitles(${field}) }}</p>`, type: "content" },
  ],
});

describe(FormioCustomFunctions.name, () => {
  let service: FormioCustomFunctions;
  let zakenService: jest.Mocked<Pick<ZakenService, "readZaak">>;
  let informatieObjectenService: jest.Mocked<
    Pick<InformatieObjectenService, "readEnkelvoudigInformatieobject">
  >;

  beforeEach(() => {
    zakenService = { readZaak: jest.fn() };
    informatieObjectenService = {
      readEnkelvoudigInformatieobject: jest.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        FormioCustomFunctions,
        { provide: ZakenService, useValue: zakenService },
        {
          provide: InformatieObjectenService,
          useValue: informatieObjectenService,
        },
      ],
    });

    service = TestBed.inject(FormioCustomFunctions);
  });

  describe(FormioCustomFunctions.prototype.hasFunctionCalls.name, () => {
    it("should return true when form contains a {{ }} function call", () => {
      expect(service.hasFunctionCalls(formWithFunction("ZAAK_Docs"))).toBe(
        true,
      );
    });

    it("should return false for a form with no function calls", () => {
      const form = { components: [{ label: "Name", type: "textfield" }] };
      expect(service.hasFunctionCalls(form)).toBe(false);
    });

    it("should return false for null", () => {
      expect(service.hasFunctionCalls(null)).toBe(false);
    });

    it("should return false for an empty object", () => {
      expect(service.hasFunctionCalls({})).toBe(false);
    });

    it("should detect function calls in deeply nested components", () => {
      const form = {
        components: [
          {
            components: [
              { html: "{{ getDocumentTitles(ZAAK_Docs) }}", type: "content" },
            ],
          },
        ],
      };
      expect(service.hasFunctionCalls(form)).toBe(true);
    });

    it("should return false for plain HTML without function syntax", () => {
      const form = {
        components: [{ html: "<p>Vaste tekst zonder functies</p>" }],
      };
      expect(service.hasFunctionCalls(form)).toBe(false);
    });
  });

  describe(FormioCustomFunctions.prototype.buildEvalContext.name, () => {
    beforeEach(() => {
      zakenService.readZaak.mockReturnValue(mockZaak({ ZAAK_Docs: [UUID_A] }));
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        mockDocument("Document A"),
      );
    });

    it("should spread zaakdata as top-level keys in the context", async () => {
      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );

      expect(context["ZAAK_Docs"]).toEqual([UUID_A]);
    });

    it("should register getDocumentTitles as a function in the context", async () => {
      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );

      expect(
        typeof context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE],
      ).toBe("function");
    });

    it("should return the pre-built title string when getDocumentTitles is called", async () => {
      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe("Document A");
    });

    it("should return empty string when the zaakdata field is missing", async () => {
      zakenService.readZaak.mockReturnValue(mockZaak({}));
      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Missing"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe("");
    });

    it("should format two documents with 'en'", async () => {
      zakenService.readZaak.mockReturnValue(mockZaak({ ZAAK_Docs: [UUID_A, UUID_B] }));
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) => mockDocument(uuid === UUID_A ? "Document A" : "Document B"),
      );

      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe("Document A en Document B");
    });

    it("should format three documents with commas and 'en'", async () => {
      zakenService.readZaak.mockReturnValue(mockZaak({ ZAAK_Docs: [UUID_A, UUID_B, UUID_C] }));
      informatieObjectenService.readEnkelvoudigInformatieobject.mockImplementation(
        (uuid) =>
          mockDocument(uuid === UUID_A ? "Document A" : uuid === UUID_B ? "Document B" : "Document C"),
      );

      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe("Document A, Document B en Document C");
    });

    it("should fall back to UUID when document fetch fails", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        throwError(() => new Error("Not found")),
      );

      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe(UUID_A);
    });

    it("should fall back to UUID when document has no titel", async () => {
      informatieObjectenService.readEnkelvoudigInformatieobject.mockReturnValue(
        mockDocument(null),
      );

      const context = await service.buildEvalContext(
        formWithFunction("ZAAK_Docs"),
        ZAAK_UUID,
      );
      const fn = context[KNOWN_FORMIO_FUNCTIONS.GET_DOCUMENT_TITLE] as () => string;

      expect(fn()).toBe(UUID_A);
    });

    it("should fetch zaakdata using the provided zaakUuid", async () => {
      await service.buildEvalContext(formWithFunction("ZAAK_Docs"), ZAAK_UUID);

      expect(zakenService.readZaak).toHaveBeenCalledWith(ZAAK_UUID);
    });

    it("should fetch each document with the zaakUuid for context", async () => {
      await service.buildEvalContext(formWithFunction("ZAAK_Docs"), ZAAK_UUID);

      expect(
        informatieObjectenService.readEnkelvoudigInformatieobject,
      ).toHaveBeenCalledWith(UUID_A, ZAAK_UUID);
    });
  });
});
