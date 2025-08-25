/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 *
 */

import { HarnessLoader } from "@angular/cdk/testing";
import { TestbedHarnessEnvironment } from "@angular/cdk/testing/testbed";
import { CommonModule } from "@angular/common";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { MatButtonModule } from "@angular/material/button";
import { MatFormFieldModule } from "@angular/material/form-field";
import { MatErrorHarness } from "@angular/material/form-field/testing";
import { MatIconModule } from "@angular/material/icon";
import { MatInputModule } from "@angular/material/input";
import { MatMenuModule } from "@angular/material/menu";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { NgxEditorModule, Toolbar } from "ngx-editor";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";
import { PipesModule } from "../../pipes/pipes.module";
import { ZacHtmlEditor } from "./html-editor";

interface TestForm extends Record<string, AbstractControl> {
  content: FormControl<string | null>;
  description: FormControl<string | null>;
  body: FormControl<string | null>;
}

describe(ZacHtmlEditor.name, () => {
  let component: ZacHtmlEditor<TestForm, keyof TestForm>;
  let fixture: ComponentFixture<typeof component>;
  let loader: HarnessLoader;

  const createTestForm = () => {
    return new FormGroup<TestForm>({
      content: new FormControl<string | null>(null, { nonNullable: true }),
      description: new FormControl<string | null>(null, { nonNullable: true }),
      body: new FormControl<string | null>(null, { nonNullable: true }),
    });
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ZacHtmlEditor],
      imports: [
        CommonModule,
        ReactiveFormsModule,
        NoopAnimationsModule,
        NgxEditorModule,
        MatFormFieldModule,
        MatInputModule,
        MatIconModule,
        MatButtonModule,
        MatMenuModule,
        PipesModule,
        MaterialFormBuilderModule,
        TranslateModule.forRoot(),
      ],
      providers: [],
    }).compileComponents();

    fixture = TestBed.createComponent(ZacHtmlEditor<TestForm, keyof TestForm>);
    component = fixture.componentInstance;
    loader = TestbedHarnessEnvironment.loader(fixture);
  });

  describe("Basic functionality", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "content";
      fixture.detectChanges();
    });

    it("should create", () => {
      expect(component).toBeTruthy();
    });

    it("should display the editor", () => {
      const editorElement = fixture.nativeElement.querySelector("ngx-editor");
      expect(editorElement).toBeTruthy();
    });

    it("should display the editor menu", () => {
      const menuElement =
        fixture.nativeElement.querySelector("ngx-editor-menu");
      expect(menuElement).toBeTruthy();
    });

    it("should bind to the form control", () => {
      component.form.controls.content.setValue("Test content");
      fixture.detectChanges();

      expect(component.form.controls.content.value).toBe("<p>Test content</p>");
    });
  });

  describe("Toolbar configuration", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "content";
      fixture.detectChanges();
    });

    it("should use custom toolbar when provided", () => {
      const customToolbar: Toolbar = [["bold", "italic"]];
      component.toolbar = customToolbar;
      fixture.detectChanges();

      expect(component.toolbar).toEqual(customToolbar);
    });
  });

  describe("Validation", () => {
    it("should apply maxlength constraint", () => {
      const form = createTestForm();
      form.controls.content.addValidators(Validators.maxLength(100));
      component.form = form;
      component.key = "content";
      fixture.detectChanges();

      const editorElement = fixture.nativeElement.querySelector("ngx-editor");
      expect(editorElement.getAttribute("maxlength")).toBe("100");
    });

    it("should display required validation error when field is empty and has required validator", async () => {
      const form = createTestForm();
      form.controls.content.addValidators(Validators.required);
      component.form = form;
      component.key = "content";
      fixture.detectChanges();

      const error = await loader.getHarness(
        MatErrorHarness.with({
          text: /required/,
        }),
      );
      expect(error).toBeTruthy();
    });

    it("should not display validation error when field is valid", async () => {
      const form = createTestForm();
      form.controls.content.addValidators(Validators.required);
      component.form = form;
      component.key = "content";
      fixture.detectChanges();

      form.controls.content.setValue("Valid content");
      fixture.detectChanges();

      const errors = await loader.getAllHarnesses(
        MatErrorHarness.with({
          text: /required/,
        }),
      );
      expect(errors.length).toBe(0);
    });

    it("should display maxlength validation error when content exceeds limit", async () => {
      const form = createTestForm();
      form.controls.content.addValidators(Validators.maxLength(10));
      component.form = form;
      component.key = "content";
      fixture.detectChanges();

      form.controls.content.setValue(
        "This content is too long and exceeds the maximum length",
      );
      fixture.detectChanges();

      const error = await loader.getHarness(
        MatErrorHarness.with({
          text: /maxlength/,
        }),
      );
      expect(error).toBeTruthy();
    });
  });

  describe("Plain text schema", () => {
    beforeEach(() => {
      component.form = createTestForm();
      component.key = "content";
      component.isPlainText = true;
      fixture.detectChanges();
    });

    it("should use plain text schema when isPlainText is true", () => {
      expect(component.toolbar).toEqual([]);
    });

    it("should strip html tags", () => {
      component.form.controls.content.setValue("Test content");
      fixture.detectChanges();

      expect(component.form.controls.content.value).toBe("Test content");
    });
  });
});
