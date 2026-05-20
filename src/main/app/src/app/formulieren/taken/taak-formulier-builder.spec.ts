/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";
import { TranslateService } from "@ngx-translate/core";
import { of } from "rxjs";
import { fromPartial } from "../../../test-helpers";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { MedewerkerGroepFormField } from "../../shared/material-form-builder/form-components/medewerker-groep/medewerker-groep-form-field";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AbstractTaakFormulier } from "./abstract-taak-formulier";
import { TaakFormulierBuilder } from "./taak-formulier-builder";

class TestTaakFormulier extends AbstractTaakFormulier {
  taakinformatieMapping = { uitkomst: "uitkomst" };

  constructor(
    translate: TranslateService,
    informatieObjectenService: InformatieObjectenService,
  ) {
    super(translate, informatieObjectenService);
  }

  protected _initStartForm(): void {}
  protected _initBehandelForm(): void {}
}

describe(TaakFormulierBuilder.name, () => {
  let formulier: TestTaakFormulier;
  let builder: TaakFormulierBuilder;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: TranslateService, useValue: { instant: jest.fn() } },
        {
          provide: InformatieObjectenService,
          useValue: {
            listEnkelvoudigInformatieobjecten: jest
              .fn()
              .mockReturnValue(of([])),
          },
        },
      ],
    });

    formulier = new TestTaakFormulier(
      TestBed.inject(TranslateService),
      TestBed.inject(InformatieObjectenService),
    );
    builder = new TaakFormulierBuilder(formulier);
  });

  describe("startForm", () => {
    const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
      id: "plan-item-1",
      naam: "Plan Item",
      groepId: "groep-1",
      fataleDatum: "2026-12-31",
      tabellen: { col1: ["val1"] },
    });
    const zaak = fromPartial<GeneratedType<"RestZaak">>({
      zaaktype: { omschrijving: "Test zaaktype" },
    });

    it("sets zaak, taakNaam, tabellen and humanTaskData on the formulier", () => {
      builder.startForm(planItem, zaak);

      expect(formulier.zaak).toBe(zaak);
      expect(formulier.taakNaam).toBe("Plan Item");
      expect(formulier.tabellen).toEqual({ col1: ["val1"] });
      expect(formulier.humanTaskData).toMatchObject({
        planItemInstanceId: "plan-item-1",
        fataledatum: "2026-12-31",
      });
    });

    it("adds a divider row and a medewerker-groep row to the form", () => {
      builder.startForm(planItem, zaak);

      expect(formulier.form).toHaveLength(2);
    });

    it("sets zaaktypeOmschrijving on the medewerker-groep field from zaak.zaaktype.omschrijving", () => {
      builder.startForm(planItem, zaak);

      const medewerkerGroepField =
        formulier.form[1][0] as MedewerkerGroepFormField;
      expect(medewerkerGroepField.zaaktypeOmschrijving).toBe("Test zaaktype");
    });

    it("returns the builder instance for chaining", () => {
      expect(builder.startForm(planItem, zaak)).toBe(builder);
    });
  });

  describe("behandelForm", () => {
    const taak = fromPartial<GeneratedType<"RestTask">>({
      tabellen: { col1: ["a"] },
      taakdata: { veld: "waarde" },
      toelichting: null,
      taakdocumenten: [],
      status: "TOEGEKEND",
      rechten: { wijzigen: true },
    });
    const zaak = fromPartial<GeneratedType<"RestZaak">>({ uuid: "zaak-uuid-1" });

    it("sets zaak, taak, tabellen and dataElementen on the formulier", () => {
      builder.behandelForm(taak, zaak);

      expect(formulier.zaak).toBe(zaak);
      expect(formulier.taak).toBe(taak);
      expect(formulier.tabellen).toEqual({ col1: ["a"] });
      expect(formulier.dataElementen).toEqual({ veld: "waarde" });
    });

    it("returns the builder instance for chaining", () => {
      expect(builder.behandelForm(taak, zaak)).toBe(builder);
    });
  });

  describe("build", () => {
    it("returns the formulier", () => {
      expect(builder.build()).toBe(formulier);
    });
  });
});
