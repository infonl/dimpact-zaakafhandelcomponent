/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { fromPartial } from "../../../../test-helpers";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { Goedkeuring } from "../goedkeuring.enum";
import { GoedkeurenFormulier } from "./goedkeuren";

describe("GoedkeurenFormulier", () => {
  let formulier: GoedkeurenFormulier;
  let informatieObjectenService: {
    listEnkelvoudigInformatieobjecten: jest.Mock;
  };
  let translateService: { instant: jest.Mock };

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
  });

  const mockDocument1 = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({ uuid: "doc-uuid-1", titel: "Document 1" });

  const mockDocument2 = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({ uuid: "doc-uuid-2", titel: "Document 2" });

  beforeEach(() => {
    informatieObjectenService = {
      listEnkelvoudigInformatieobjecten: jest.fn().mockReturnValue(of([])),
    };
    translateService = {
      instant: jest.fn().mockReturnValue("translated-value"),
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: translateService },
        {
          provide: InformatieObjectenService,
          useValue: informatieObjectenService,
        },
      ],
    });

    formulier = TestBed.inject(GoedkeurenFormulier);
  });

  describe("requestForm", () => {
    describe("field structure", () => {
      it("should return exactly 2 fields", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.length).toBe(2);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.map((f) => f.key)).toEqual([
          "vraag",
          "relevanteDocumenten",
        ]);
      });

      it("should render vraag as a textarea", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "vraag")?.type).toBe("textarea");
      });

      it("should render relevanteDocumenten as documents", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "relevanteDocumenten")?.type).toBe(
          "documents",
        );
      });
    });

    describe("vraag field", () => {
      it("should initialize vraag as empty string", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.find((f) => f.key === "vraag")?.control?.value).toBe("");
      });

      it("should require vraag", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("");
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should enforce maxLength of 1000 on vraag", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept a valid vraag within 1000 characters", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "vraag")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });
    });

    describe("relevanteDocumenten field", () => {
      it("should fetch documents as an Observable (not awaited) for the zaak", async () => {
        await formulier.requestForm(mockZaak);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({ zaakUUID: "zaak-uuid" });
      });

      it("should pass the Observable directly as options without resolving it", async () => {
        const documentsObservable = of([mockDocument1]);
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          documentsObservable,
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "relevanteDocumenten");
        // options is the Observable itself, not a resolved array
        expect("options" in field! ? field.options : null).toBe(
          documentsObservable,
        );
      });

      it("should not have a control (display-only field)", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(
          fields.find((f) => f.key === "relevanteDocumenten")?.control,
        ).toBeUndefined();
      });
    });
  });

  describe("handleForm", () => {
    const mockTaak = fromPartial<GeneratedType<"RestTask">>({
      zaakUuid: "zaak-uuid",
      zaakIdentificatie: "ZAAK-2026-001",
      taakdata: {},
    });

    describe("field structure", () => {
      it("should return exactly 4 fields", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.length).toBe(4);
      });

      it("should return fields in the expected order", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.map((f) => f.key)).toEqual([
          "intro",
          "vraag",
          "ondertekenen",
          "goedkeuren",
        ]);
      });

      it("should render intro as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "intro")?.type).toBe("plain-text");
      });

      it("should render vraag as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.type).toBe("plain-text");
      });

      it("should render ondertekenen as documents", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "ondertekenen")?.type).toBe(
          "documents",
        );
      });

      it("should render goedkeuren as radio", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "goedkeuren")?.type).toBe("radio");
      });
    });

    describe("intro field", () => {
      it("should translate msg.goedkeuring.behandelen with zaaknummer", async () => {
        await formulier.handleForm(mockTaak);

        expect(translateService.instant).toHaveBeenCalledWith(
          "msg.goedkeuring.behandelen",
          { zaaknummer: "ZAAK-2026-001" },
        );
      });

      it("should set intro control value to translated string", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "intro")?.control?.value).toBe(
          "translated-value",
        );
      });
    });

    describe("vraag field", () => {
      it("should have label vraag", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.label).toBe("vraag");
      });
    });

    describe("ondertekenen field", () => {
      it("should fetch relevanteDocumenten by UUID from taakdata", async () => {
        const taakWithDocumenten = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { relevanteDocumenten: "doc-uuid-1;doc-uuid-2" },
        });

        await formulier.handleForm(taakWithDocumenten);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({
          zaakUUID: "zaak-uuid",
          informatieobjectUUIDs: ["doc-uuid-1", "doc-uuid-2"],
        });
      });

      it("should fetch with empty UUIDs when relevanteDocumenten taakdata is absent", async () => {
        await formulier.handleForm(mockTaak);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({
          zaakUUID: "zaak-uuid",
          informatieobjectUUIDs: [],
        });
      });

      it("should fetch with empty UUIDs when taakdata is undefined", async () => {
        const taakWithoutData = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: undefined,
        });

        await formulier.handleForm(taakWithoutData);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({
          zaakUUID: "zaak-uuid",
          informatieobjectUUIDs: [],
        });
      });

      it("should set ondertekenen options to fetched documents", async () => {
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          of([mockDocument1, mockDocument2]),
        );

        const fields = await formulier.handleForm(mockTaak);

        const field = fields.find((f) => f.key === "ondertekenen");
        expect("options" in field! ? field.options : []).toEqual([
          mockDocument1,
          mockDocument2,
        ]);
      });

      it("should pre-check documents that were previously signed (ondertekenen taakdata)", async () => {
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          of([mockDocument1, mockDocument2]),
        );
        const taakWithSigned = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { ondertekenen: "doc-uuid-1" },
        });

        const fields = await formulier.handleForm(taakWithSigned);

        const control = fields.find((f) => f.key === "ondertekenen")?.control;
        expect(control?.value).toEqual([mockDocument1]);
      });

      it("should not pre-check documents that were not previously signed", async () => {
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          of([mockDocument1, mockDocument2]),
        );
        const taakWithSigned = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { ondertekenen: "doc-uuid-1" },
        });

        const fields = await formulier.handleForm(taakWithSigned);

        const control = fields.find((f) => f.key === "ondertekenen")?.control;
        expect(control?.value).not.toContainEqual(mockDocument2);
      });

      it("should initialize ondertekenen as empty when no documents were previously signed", async () => {
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          of([mockDocument1, mockDocument2]),
        );

        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "ondertekenen")?.control;
        expect(control?.value).toEqual([]);
      });
    });

    describe("goedkeuren field", () => {
      it("should have options derived from Goedkeuring enum with goedkeuren. prefix", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const field = fields.find((f) => f.key === "goedkeuren");
        expect("options" in field! ? field.options : []).toEqual(
          Object.values(Goedkeuring).map((v) => `goedkeuren.${v}`),
        );
      });

      it("should require goedkeuren", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "goedkeuren")?.control;
        control?.setValue(null);
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should pre-fill goedkeuren from taakdata", async () => {
        const taakWithGoedkeuren = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { goedkeuren: `goedkeuren.${Goedkeuring.akkoord}` },
        });

        const fields = await formulier.handleForm(taakWithGoedkeuren);

        expect(fields.find((f) => f.key === "goedkeuren")?.control?.value).toBe(
          `goedkeuren.${Goedkeuring.akkoord}`,
        );
      });

      it("should have no goedkeuren pre-filled when taakdata is empty", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "goedkeuren")?.control?.value,
        ).toBeFalsy();
      });
    });
  });
});
