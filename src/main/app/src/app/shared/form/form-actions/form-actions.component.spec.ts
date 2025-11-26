/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { Injector, runInInjectionContext } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder, FormGroup } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import {
  injectMutation,
  provideQueryClient,
} from "@tanstack/angular-query-experimental";
import { sleep, testQueryClient } from "../../../../../setupJest";
import { MaterialModule } from "../../material/material.module";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacFormActions } from "./form-actions.component";

describe(ZacFormActions.name, () => {
  let fixture: ComponentFixture<ZacFormActions>;
  let loader: HarnessLoader;
  let form: FormGroup;
  let mutation: ReturnType<typeof injectMutation>;
  let injector: Injector;
  const MUTATION_TIMEOUT = 5;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ZacFormActions],
      imports: [
        TranslateModule.forRoot(),
        PipesModule,
        MaterialModule,
        NoopAnimationsModule,
      ],
      providers: [provideQueryClient(testQueryClient)],
    });

    const formBuilder = TestBed.inject(FormBuilder);
    form = formBuilder.group({
      field: [null],
    });

    injector = TestBed.inject(Injector);
    mutation = runInInjectionContext(injector, () =>
      injectMutation(() => ({
        mutationKey: ["test-mutation"],
        mutationFn: jest.fn().mockReturnValue(
          new Promise((resolve) => {
            setTimeout(() => resolve({ success: true }), MUTATION_TIMEOUT);
          }),
        ),
      })),
    );

    fixture = TestBed.createComponent(ZacFormActions);
    fixture.componentRef.setInput("mutation", mutation);
    fixture.componentRef.setInput("form", form);

    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("action buttons", () => {
    it("should have two buttons", async () => {
      const buttons = await loader.getAllHarnesses(
        MatButtonHarness.with({
          text: /actie./,
        }),
      );

      expect(buttons.length).toBe(2);
    });

    it.each(["actie.verstuur", "actie.annuleren"])(
      "should have button the %s button",
      async (label) => {
        const button = await loader.getHarness(
          MatButtonHarness.with({ text: label }),
        );

        expect(button).toBeDefined();
      },
    );
  });

  describe("disabling", () => {
    describe.each([
      [{ required: true }, true],
      [null, false],
    ])("with the form errors '%o'", (errors, expected) => {
      beforeEach(() => {
        form.setErrors(errors);
      });
      it("should set the submit button state", async () => {
        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.verstuur" }),
        );

        const isSubmitDisabled = await submitButton.isDisabled();
        expect(isSubmitDisabled).toBe(expected);
      });

      it("should not disable the cancel button", async () => {
        const cancelButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.annuleren" }),
        );

        const isCancelDisabled = await cancelButton.isDisabled();
        expect(isCancelDisabled).toBe(false);
      });
    });

    it("should disable the submit button the form is disabled", async () => {
        form.disable();
        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.verstuur" }),
        );
        const isSubmitDisabled = await submitButton.isDisabled();
        expect(isSubmitDisabled).toBe(true);
    })

    describe("when mutating", () => {
      beforeEach(async () => {
        await mutation.mutateAsync({});
      });

      it("should disable the submit button", async () => {
        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.verstuur" }),
        );
        const isDisabled = await submitButton.isDisabled();
        expect(isDisabled).toBe(true);
      });

      it("should disable the cancel button", async () => {
        const cancelButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.annuleren" }),
        );
        const isDisabled = await cancelButton.isDisabled();
        expect(isDisabled).toBe(true);
      });

      describe("on mutation settled", () => {
        beforeEach(async () => {
          await sleep(MUTATION_TIMEOUT + 100); // wait for mutation to settle
        });
        it("should enable the submit button again", async () => {
          const submitButton = await loader.getHarness(
            MatButtonHarness.with({ text: "actie.verstuur" }),
          );
          const isDisabled = await submitButton.isDisabled();
          expect(isDisabled).toBe(false);
        });

        it("should disable the cancel button", async () => {
          const cancelButton = await loader.getHarness(
            MatButtonHarness.with({ text: "actie.annuleren" }),
          );
          const isDisabled = await cancelButton.isDisabled();
          expect(isDisabled).toBe(false);
        });
      });
    });
  });
});
