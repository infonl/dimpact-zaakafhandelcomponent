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

    isPendingSignal = signal(true);
    fixture.componentRef.setInput("form", form);
    fixture.componentRef.setInput("mutation", {
      isPending: () => isPendingSignal(),
    });

    fixture.detectChanges();
    await Promise.resolve();
  });

  describe("when mutation is pending", () => {
    beforeEach(() => form.markAsDirty());

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
