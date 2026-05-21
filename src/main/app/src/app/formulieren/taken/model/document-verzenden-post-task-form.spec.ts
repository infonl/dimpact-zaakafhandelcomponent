/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { provideHttpClient } from "@angular/common/http";
import { TestBed } from "@angular/core/testing";
import { TranslateModule, TranslateService } from "@ngx-translate/core";
import moment from "moment";
import { of } from "rxjs";
import { fromPartial } from "../../../../test-helpers";
import { InformatieObjectenService } from "../../../informatie-objecten/informatie-objecten.service";
import { FormField } from "../../../shared/form/form";
import { GeneratedType } from "../../../shared/utils/generated-types";
import { DocumentVerzendenPostTaskForm } from "./document-verzenden-post-task-form";

describe(DocumentVerzendenPostTaskForm.name, () => {
  let formulier: DocumentVerzendenPostTaskForm;
  let informatieObjectenService: InformatieObjectenService;
  let listVoorVerzendenSpy: jest.SpyInstance;
  let listEnkelvoudigSpy: jest.SpyInstance;
  let translateService: TranslateService;

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
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      providers: [provideHttpClient()],
    });

    informatieObjectenService = TestBed.inject(InformatieObjectenService);
    listVoorVerzendenSpy = jest
      .spyOn(informatieObjectenService, "listInformatieobjectenVoorVerzenden")
      .mockReturnValue(of([]));
    listEnkelvoudigSpy = jest
      .spyOn(informatieObjectenService, "listEnkelvoudigInformatieobjecten")
      .mockReturnValue(of([]));

    translateService = TestBed.inject(TranslateService);
    jest
      .spyOn(translateService, "instant")
      .mockReturnValue("translated-intro");

    formulier = TestBed.inject(DocumentVerzendenPostTaskForm);
  });

  describe("requestForm (_initStartForm)", () => {
    describe("field structure", () => {
      let fields: FormField[];

      beforeEach(async () => {
        fields = await formulier.requestForm(mockZaak);
      });

      it("should expose exactly 2 fields in order", () => {
        expect(fields.map((f) => f.key)).toEqual([
          "documentenVerzendenPost",
          "toelichting",
        ]);
      });

      it("should render documentenVerzendenPost as documents", () => {
        expect(
          fields.find((f) => f.key === "documentenVerzendenPost")?.type,
        ).toBe("documents");
      });

      it("should open documents in a new tab", () => {
        const field = fields.find((f) => f.key === "documentenVerzendenPost");
        expect(
          field && "viewDocumentInNewTab" in field
            ? field.viewDocumentInNewTab
            : null,
        ).toBe(true);
      });

      it("should render toelichting as a textarea", () => {
        expect(fields.find((f) => f.key === "toelichting")?.type).toBe(
          "textarea",
        );
      });
    });

    describe("documentenVerzendenPost field", () => {
      it("should fetch documents via listInformatieobjectenVoorVerzenden with zaak uuid", async () => {
        await formulier.requestForm(mockZaak);

        expect(
          informatieObjectenService.listInformatieobjectenVoorVerzenden,
        ).toHaveBeenCalledWith("zaak-uuid");
      });

      it("should set documents options to the resolved list", async () => {
        listVoorVerzendenSpy.mockReturnValue(
          of([mockDocument1, mockDocument2]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "documentenVerzendenPost");
        expect("options" in field! ? field.options : []).toEqual([
          mockDocument1,
          mockDocument2,
        ]);
      });

      it("should initialize the control as an empty array", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(
          fields.find((f) => f.key === "documentenVerzendenPost")?.control
            ?.value,
        ).toEqual([]);
      });

      it("should require at least one selected document", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find(
          (f) => f.key === "documentenVerzendenPost",
        )?.control;
        control?.setValue([]);
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should accept a non-empty document selection", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find(
          (f) => f.key === "documentenVerzendenPost",
        )?.control;
        control?.setValue([mockDocument1]);

        expect(control?.errors).toBeNull();
      });
    });

    describe("toelichting field", () => {
      it("should initialize toelichting as null", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(
          fields.find((f) => f.key === "toelichting")?.control?.value,
        ).toBeNull();
      });

      it("should enforce maxLength of 1000 on toelichting", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "toelichting")?.control;
        control?.setValue("a".repeat(1001));

        expect(control?.errors?.["maxlength"]).toBeDefined();
      });

      it("should accept toelichting within 1000 characters", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "toelichting")?.control;
        control?.setValue("a".repeat(1000));

        expect(control?.errors).toBeNull();
      });

      it("should accept an empty toelichting (not required)", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const control = fields.find((f) => f.key === "toelichting")?.control;
        control?.setValue("");

        expect(control?.errors?.["required"]).toBeUndefined();
      });
    });
  });

  describe("handleForm (_initBehandelForm)", () => {
    const baseTaak = fromPartial<GeneratedType<"RestTask">>({
      zaakUuid: "zaak-uuid",
      taakdata: {},
      status: "OPEN" as GeneratedType<"TaakStatus">,
      rechten: fromPartial({ wijzigen: true }),
    });

    describe("field structure", () => {
      let fields: FormField[];

      beforeEach(async () => {
        fields = await formulier.handleForm(baseTaak);
      });

      it("should expose exactly 3 fields in order", () => {
        expect(fields.map((f) => f.key)).toEqual([
          "intro",
          "documentenVerzendenPost",
          "verzenddatum",
        ]);
      });

      it("should render intro as plain-text", () => {
        expect(fields.find((f) => f.key === "intro")?.type).toBe("plain-text");
      });

      it("should render documentenVerzendenPost as documents", () => {
        expect(
          fields.find((f) => f.key === "documentenVerzendenPost")?.type,
        ).toBe("documents");
      });

      it("should render verzenddatum as a date field", () => {
        expect(fields.find((f) => f.key === "verzenddatum")?.type).toBe("date");
      });
    });

    describe("intro field", () => {
      it("should translate msg.document.verzenden.post.behandelen", async () => {
        await formulier.handleForm(baseTaak);

        expect(translateService.instant).toHaveBeenCalledWith(
          "msg.document.verzenden.post.behandelen",
        );
      });

      it("should set intro control value to the translated string", async () => {
        const fields = await formulier.handleForm(baseTaak);

        expect(fields.find((f) => f.key === "intro")?.control?.value).toBe(
          "translated-intro",
        );
      });
    });

    describe("documentenVerzendenPost field", () => {
      it("should be readonly", async () => {
        const fields = await formulier.handleForm(baseTaak);

        expect(
          fields.find((f) => f.key === "documentenVerzendenPost")?.readonly,
        ).toBe(true);
      });

      it("should fetch documents by UUID from taakdata documentenVerzendenPost", async () => {
        const taakWithDocs = fromPartial<GeneratedType<"RestTask">>({
          ...baseTaak,
          taakdata: { documentenVerzendenPost: "doc-uuid-1;doc-uuid-2" },
        });
        listEnkelvoudigSpy.mockReturnValue(of([mockDocument1, mockDocument2]));

        await formulier.handleForm(taakWithDocs);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({
          zaakUUID: "zaak-uuid",
          informatieobjectUUIDs: ["doc-uuid-1", "doc-uuid-2"],
        });
      });

      it("should pre-fill options and the control with the fetched documents", async () => {
        const taakWithDocs = fromPartial<GeneratedType<"RestTask">>({
          ...baseTaak,
          taakdata: { documentenVerzendenPost: "doc-uuid-1;doc-uuid-2" },
        });
        listEnkelvoudigSpy.mockReturnValue(of([mockDocument1, mockDocument2]));

        const fields = await formulier.handleForm(taakWithDocs);

        const field = fields.find((f) => f.key === "documentenVerzendenPost");
        expect("options" in field! ? field.options : []).toEqual([
          mockDocument1,
          mockDocument2,
        ]);
        expect(field?.control?.value).toEqual([mockDocument1, mockDocument2]);
      });

      it("should not fetch when taakdata has no documentenVerzendenPost", async () => {
        await formulier.handleForm(baseTaak);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).not.toHaveBeenCalled();
      });

      it("should expose an empty list and control when taakdata has no documentenVerzendenPost", async () => {
        const fields = await formulier.handleForm(baseTaak);

        const field = fields.find((f) => f.key === "documentenVerzendenPost");
        expect("options" in field! ? field.options : null).toEqual([]);
        expect(field?.control?.value).toEqual([]);
      });

      it("should expose an empty list when taakdata itself is undefined", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            ...baseTaak,
            taakdata: undefined,
          }),
        );

        const field = fields.find((f) => f.key === "documentenVerzendenPost");
        expect("options" in field! ? field.options : null).toEqual([]);
        expect(field?.control?.value).toEqual([]);
        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).not.toHaveBeenCalled();
      });
    });

    describe("verzenddatum field", () => {
      it("should require verzenddatum", async () => {
        const fields = await formulier.handleForm(baseTaak);

        const control = fields.find((f) => f.key === "verzenddatum")?.control;
        control?.setValue(null);
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should default verzenddatum to today (moment) when taakdata is empty", async () => {
        const fields = await formulier.handleForm(baseTaak);

        const value = fields.find((f) => f.key === "verzenddatum")?.control
          ?.value;
        expect(moment.isMoment(value)).toBe(true);
        expect(
          (value as moment.Moment).isSame(moment(), "day"),
        ).toBe(true);
      });

      it("should pre-fill verzenddatum from taakdata when present", async () => {
        const taakWithDate = fromPartial<GeneratedType<"RestTask">>({
          ...baseTaak,
          taakdata: { verzenddatum: "2026-03-15T00:00:00.000Z" },
        });

        const fields = await formulier.handleForm(taakWithDate);

        const value = fields.find((f) => f.key === "verzenddatum")?.control
          ?.value;
        expect(moment.isMoment(value)).toBe(true);
        expect(
          (value as moment.Moment).isSame(moment("2026-03-15T00:00:00.000Z")),
        ).toBe(true);
      });
    });

    describe("readonly mode", () => {
      it("should mark verzenddatum readonly when task is AFGEROND", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            ...baseTaak,
            status: "AFGEROND" as GeneratedType<"TaakStatus">,
          }),
        );

        expect(fields.find((f) => f.key === "verzenddatum")?.readonly).toBe(
          true,
        );
      });

      it("should mark verzenddatum readonly when user cannot wijzigen", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            ...baseTaak,
            rechten: fromPartial({ wijzigen: false }),
          }),
        );

        expect(fields.find((f) => f.key === "verzenddatum")?.readonly).toBe(
          true,
        );
      });

      it("should mark verzenddatum readonly when rechten is missing", async () => {
        const fields = await formulier.handleForm(
          fromPartial<GeneratedType<"RestTask">>({
            ...baseTaak,
            rechten: undefined,
          }),
        );

        expect(fields.find((f) => f.key === "verzenddatum")?.readonly).toBe(
          true,
        );
      });

      it("should keep verzenddatum editable when task is OPEN and user can wijzigen", async () => {
        const fields = await formulier.handleForm(baseTaak);

        expect(fields.find((f) => f.key === "verzenddatum")?.readonly).toBe(
          false,
        );
      });
    });
  });
});
