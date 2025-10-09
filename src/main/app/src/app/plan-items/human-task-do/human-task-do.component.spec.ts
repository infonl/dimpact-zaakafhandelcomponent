/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder, ReactiveFormsModule } from "@angular/forms";
import { MatFormFieldHarness } from "@angular/material/form-field/testing";
import { MatInputHarness } from "@angular/material/input/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { fromPartial } from "@total-typescript/shoehorn";
import { of } from "rxjs";
import { TaakFormulierenService } from "../../formulieren/taken/taak-formulieren.service";
import { IdentityService } from "../../identity/identity.service";
import { InformatieObjectenService } from "../../informatie-objecten/informatie-objecten.service";
import { MaterialFormBuilderModule } from "../../shared/material-form-builder/material-form-builder.module";
import { MaterialModule } from "../../shared/material/material.module";
import { PipesModule } from "../../shared/pipes/pipes.module";
import { GeneratedType } from "../../shared/utils/generated-types";
import { HumanTaskDoComponent } from "./human-task-do.component";

describe("HumanTaskDoComponent", () => {
  let component: HumanTaskDoComponent;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  let identityService: IdentityService;
  let taakFormulierenService: TaakFormulierenService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [HumanTaskDoComponent],
      imports: [
        ReactiveFormsModule,
        MaterialModule,
        MaterialFormBuilderModule,
        PipesModule,
        TranslateModule.forRoot(),
        NoopAnimationsModule,
      ],
      providers: [
        FormBuilder,
        TaakFormulierenService,
        InformatieObjectenService,
        IdentityService,
      ],
    }).compileComponents();

    taakFormulierenService = TestBed.inject(TaakFormulierenService);

    identityService = TestBed.inject(IdentityService);
    jest.spyOn(identityService, "listGroups").mockReturnValue(
      of([
        fromPartial<GeneratedType<"RestGroup">>({
          id: "1",
          naam: "groep1",
        }),
      ]),
    );

    fixture = TestBed.createComponent(HumanTaskDoComponent);

    component = fixture.componentInstance;
    component.planItem = fromPartial({
      type: "HUMAN_TASK",
      formulierDefinitie: "ADVIES",
    });
    component.zaak = fromPartial({});

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("basic angular form", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockResolvedValue([]);
    });

    it("should create the default form controls", async () => {
      await component.ngOnInit();

      const fields = await loader.getAllHarnesses(MatFormFieldHarness);
      expect(fields).toHaveLength(2); // `Group` and `User` inputs
    });
  });

  describe("angular form with fields", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockResolvedValue([
          { type: "input", key: "question" },
        ]);
    });

    it("should create the form controls", async () => {
      await component.ngOnInit();

      const input = await loader.getHarness(MatInputHarness);
      expect(input).not.toBeNull(); // `question` input
    });
  });

  describe("custom form builder", () => {
    beforeEach(() => {
      jest
        .spyOn(taakFormulierenService, "getAngularRequestFormBuilder")
        .mockImplementation(() => {
          throw new Error("Not implemented");
        });
      jest.spyOn(taakFormulierenService, "getFormulierBuilder").mockReturnValue(
        fromPartial({
          startForm: () =>
            fromPartial({
              build: () =>
                fromPartial({
                  form: [],
                }),
            }),
        }),
      );
    });

    it("should fallback to the old custom form builder", async () => {
      const spy = jest.spyOn(taakFormulierenService, "getFormulierBuilder");
      await component.ngOnInit();
      expect(spy).toHaveBeenCalledWith(component.planItem?.formulierDefinitie);
    });
  });
});
