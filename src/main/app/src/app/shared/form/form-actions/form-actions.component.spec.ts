/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { FormBuilder, FormGroup } from "@angular/forms";
import { MatButtonHarness } from "@angular/material/button/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { HarnessLoader } from "@angular/cdk/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { signal } from "@angular/core";

import { ZacFormActions } from "./form-actions.component";

describe(ZacFormActions.name, () => {
  let fixture: ComponentFixture<ZacFormActions>;
  let loader: HarnessLoader;
  let form: FormGroup;
  let isPendingSignal: ReturnType<typeof signal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZacFormActions],
      imports: [TranslateModule.forRoot(), NoopAnimationsModule],
    }).compileComponents();

    const formBuilder = TestBed.inject(FormBuilder);
    form = formBuilder.group({ field: [null] });

    fixture = TestBed.createComponent(ZacFormActions);
    loader = TestbedHarnessEnvironment.loader(fixture);

    isPendingSignal = signal(false);
    fixture.componentRef.setInput("form", form);
    fixture.componentRef.setInput("mutation", {
      isPending: () => isPendingSignal(),
    });

    fixture.detectChanges();
    await Promise.resolve();
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
        form.markAsDirty();
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

    it("should disable the submit button when the form is disabled", async () => {
      form.markAsDirty();
      form.disable();

      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.verstuur" }),
      );
      const isSubmitDisabled = await submitButton.isDisabled();
      expect(isSubmitDisabled).toBe(true);
    });
  });

  describe("when mutation is pending", () => {
    beforeEach(() => {
      isPendingSignal = signal(true);
      form.markAsDirty();
    });

    it("should disable the submit button", async () => {
      const submitButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.verstuur" }),
      );
      expect(await submitButton.isDisabled()).toBe(true);
    });

    it("should disable the cancel button", async () => {
      const cancelButton = await loader.getHarness(
        MatButtonHarness.with({ text: "actie.annuleren" }),
      );
      expect(await cancelButton.isDisabled()).toBe(true);
    });

    describe("after mutation resolves", () => {
      beforeEach(async () => {
        isPendingSignal.set(false); // resolve mutation
        fixture.detectChanges();
        await fixture.whenStable();
      });

      it("should enable the submit button again", async () => {
        const submitButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.verstuur" }),
        );
        expect(await submitButton.isDisabled()).toBe(false);
      });

      it("should enable the cancel button again", async () => {
        const cancelButton = await loader.getHarness(
          MatButtonHarness.with({ text: "actie.annuleren" }),
        );
        expect(await cancelButton.isDisabled()).toBe(false);
      });
    });
  });
});
