/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { TestBed } from "@angular/core/testing";

import {
  provideHttpClient,
  withInterceptorsFromDi,
} from "@angular/common/http";
import { TranslateService } from "@ngx-translate/core";
import { provideTanStackQuery } from "@tanstack/angular-query-experimental";
import { FoutAfhandelingService } from "src/app/fout-afhandeling/fout-afhandeling.service";
import { testQueryClient } from "../../../../setupJest";
import { fromPartial } from "../../../test-helpers";
import { GeneratedType } from "../../shared/utils/generated-types";
import { AanvullendeInformatieTaskForm } from "./model/aanvullende-informatie-task-form";
import { AdviesTaskForm } from "./model/advies-task-form";
import { GoedkeurenTaskForm } from "./model/goedkeuren-task-form";
import { TaakFormulierenService } from "./taak-formulieren.service";

describe("TaakFormulierenService", () => {
  let service: TaakFormulierenService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [
        { provide: FoutAfhandelingService, useValue: {} },
        { provide: TranslateService, useValue: {} },
        provideHttpClient(withInterceptorsFromDi()),
        provideTanStackQuery(testQueryClient),
      ],
    });
    service = TestBed.inject(TaakFormulierenService);
  });

  it("should be created", () => {
    expect(service).toBeTruthy();
  });

  describe("getAngularRequestFormBuilder", () => {
    const mockZaak = fromPartial<GeneratedType<"RestZaak">>({
      uuid: "zaak-uuid",
    });

    it("should delegate to goedkeurenFormulier for GOEDKEUREN", async () => {
      const spy = jest
        .spyOn(TestBed.inject(GoedkeurenTaskForm), "requestForm")
        .mockReturnValue(Promise.resolve([]));
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        formulierDefinitie: "GOEDKEUREN",
      });

      await service.getAngularRequestFormBuilder(mockZaak, planItem);

      expect(spy).toHaveBeenCalledWith(mockZaak);
    });

    it("should delegate to aanvullendeInformatieFormulier and pass planItem for AANVULLENDE_INFORMATIE", async () => {
      const spy = jest
        .spyOn(TestBed.inject(AanvullendeInformatieTaskForm), "requestForm")
        .mockReturnValue(Promise.resolve([]));
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        formulierDefinitie: "AANVULLENDE_INFORMATIE",
      });

      await service.getAngularRequestFormBuilder(mockZaak, planItem);

      expect(spy).toHaveBeenCalledWith(mockZaak, planItem);
    });

    it("should delegate to adviesFormulier for ADVIES", async () => {
      const spy = jest
        .spyOn(TestBed.inject(AdviesTaskForm), "requestForm")
        .mockReturnValue(Promise.resolve([]));
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        formulierDefinitie: "ADVIES",
      });

      await service.getAngularRequestFormBuilder(mockZaak, planItem);

      expect(spy).toHaveBeenCalledWith(mockZaak);
    });

    it("should throw for an unknown formulierDefinitie", async () => {
      const planItem = fromPartial<GeneratedType<"RESTPlanItem">>({
        formulierDefinitie: "UNKNOWN" as GeneratedType<"FormulierDefinitie">,
      });

      await expect(
        service.getAngularRequestFormBuilder(mockZaak, planItem),
      ).rejects.toThrow("Onbekende formulierDefinitie for Angular form");
    });

    it("should throw when planItem is undefined", async () => {
      await expect(
        service.getAngularRequestFormBuilder(mockZaak, undefined),
      ).rejects.toThrow("Onbekende formulierDefinitie for Angular form");
    });
  });
});
