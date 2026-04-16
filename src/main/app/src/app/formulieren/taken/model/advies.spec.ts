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
import { AdviesFormulier } from "./advies";

describe("AdviesFormulier", () => {
  let formulier: AdviesFormulier;
  let informatieObjectenService: {
    listEnkelvoudigInformatieobjecten: jest.Mock;
  };
  let translateService: { instant: jest.Mock };

  const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
    uuid: "zaak-uuid",
  });

  const mockDocument = fromPartial<
    GeneratedType<"RestEnkelvoudigInformatieobject">
  >({ uuid: "doc-uuid-1", titel: "Document 1" });

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

    formulier = TestBed.inject(AdviesFormulier);
  });

  describe("requestForm", () => {
    describe("field structure", () => {
      it("should return exactly 2 fields", async () => {
        const fields = await formulier.requestForm(mockZaak);

        expect(fields.length).toBe(2);
      });

      it("should return fields with keys: vraag, relevanteDocumenten", async () => {
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

        expect(
          fields.find((f) => f.key === "relevanteDocumenten")?.type,
        ).toBe("documents");
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
      it("should set viewDocumentInNewTab to true", async () => {
        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "relevanteDocumenten");
        expect(
          "viewDocumentInNewTab" in field! ? field.viewDocumentInNewTab : false,
        ).toBe(true);
      });

      it("should fetch documents for the zaak", async () => {
        await formulier.requestForm(mockZaak);

        expect(
          informatieObjectenService.listEnkelvoudigInformatieobjecten,
        ).toHaveBeenCalledWith({ zaakUUID: "zaak-uuid" });
      });

      it("should pass fetched documents as options", async () => {
        informatieObjectenService.listEnkelvoudigInformatieobjecten.mockReturnValue(
          of([mockDocument]),
        );

        const fields = await formulier.requestForm(mockZaak);

        const field = fields.find((f) => f.key === "relevanteDocumenten");
        expect(
          "options" in field! ? field.options : [],
        ).toEqual([mockDocument]);
      });
    });
  });

  describe("handleForm", () => {
    const mockTaak = fromPartial<GeneratedType<"RestTask">>({
      zaakUuid: "zaak-uuid",
      taakdata: {},
      tabellen: { ADVIES: ["akkoord", "niet-akkoord"] },
    });

    describe("field structure", () => {
      it("should return exactly 4 fields", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.length).toBe(4);
      });

      it("should return fields with keys: titel, vraag, relevanteDocumenten, advies", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.map((f) => f.key)).toEqual([
          "titel",
          "vraag",
          "relevanteDocumenten",
          "advies",
        ]);
      });

      it("should render titel as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "titel")?.type).toBe("plain-text");
      });

      it("should render vraag as plain-text", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "vraag")?.type).toBe("plain-text");
      });

      it("should render relevanteDocumenten as documents", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "relevanteDocumenten")?.type,
        ).toBe("documents");
      });

      it("should render advies as radio", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(fields.find((f) => f.key === "advies")?.type).toBe("radio");
      });
    });

    describe("titel field", () => {
      it("should use translated msg.advies.behandelen as value", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(translateService.instant).toHaveBeenCalledWith(
          "msg.advies.behandelen",
        );
        expect(fields.find((f) => f.key === "titel")?.control?.value).toBe(
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

    describe("relevanteDocumenten field", () => {
      it("should be readonly", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "relevanteDocumenten")?.readonly,
        ).toBe(true);
      });

      it("should fetch documents using zaakUuid and relevanteDocumenten UUIDs from taakdata", async () => {
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
    });

    describe("advies field", () => {
      it("should use options from taak.tabellen ADVIES", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const field = fields.find((f) => f.key === "advies");
        expect(
          "options" in field! ? field.options : [],
        ).toEqual(["akkoord", "niet-akkoord"]);
      });

      it("should fall back to empty options when tabellen is undefined", async () => {
        const taakWithoutTabellen = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          tabellen: undefined,
        });

        const fields = await formulier.handleForm(taakWithoutTabellen);

        const field = fields.find((f) => f.key === "advies");
        expect(
          "options" in field! ? field.options : null,
        ).toEqual([]);
      });

      it("should fall back to empty options when ADVIES key is absent from tabellen", async () => {
        const taakWithoutAdviesTabellen = fromPartial<
          GeneratedType<"RestTask">
        >({
          ...mockTaak,
          tabellen: {},
        });

        const fields = await formulier.handleForm(taakWithoutAdviesTabellen);

        const field = fields.find((f) => f.key === "advies");
        expect(
          "options" in field! ? field.options : null,
        ).toEqual([]);
      });

      it("should require advies", async () => {
        const fields = await formulier.handleForm(mockTaak);

        const control = fields.find((f) => f.key === "advies")?.control;
        control?.setValue(null);
        control?.markAsTouched();

        expect(control?.errors?.["required"]).toBeDefined();
      });

      it("should pre-fill advies from taakdata", async () => {
        const taakWithAdvies = fromPartial<GeneratedType<"RestTask">>({
          ...mockTaak,
          taakdata: { advies: "akkoord" },
        });

        const fields = await formulier.handleForm(taakWithAdvies);

        expect(
          fields.find((f) => f.key === "advies")?.control?.value,
        ).toBe("akkoord");
      });

      it("should have no advies pre-filled when taakdata is empty", async () => {
        const fields = await formulier.handleForm(mockTaak);

        expect(
          fields.find((f) => f.key === "advies")?.control?.value,
        ).toBeFalsy();
      });
    });
  });
});
