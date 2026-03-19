/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { NgIf } from "@angular/common";
import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatButtonModule } from "@angular/material/button";
import { MatIconModule } from "@angular/material/icon";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { TranslateModule } from "@ngx-translate/core";
import { UtilService } from "../../../core/service/util.service";
import { MaterialFormBuilderModule } from "../../material-form-builder/material-form-builder.module";
import { InputFormFieldBuilder } from "../../material-form-builder/form-components/input/input-form-field-builder";
import { EmptyPipe } from "../../pipes/empty.pipe";
import { OutsideClickDirective } from "../../directives/outside-click.directive";
import { EditInputComponent } from "./edit-input.component";

@Component({
  templateUrl: "./edit-input.component.html",
  standalone: true,
  imports: [
    NgIf,
    MatIconModule,
    MatButtonModule,
    TranslateModule,
    EmptyPipe,
    OutsideClickDirective,
    MaterialFormBuilderModule,
  ],
})
class TestEditInputComponent extends EditInputComponent {}

describe(EditInputComponent.name, () => {
  let fixture: ComponentFixture<TestEditInputComponent>;
  let component: TestEditInputComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TestEditInputComponent,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        {
          provide: UtilService,
          useValue: {
            setTitle: jest.fn(),
            openSnackbar: jest.fn(),
            hasEditOverlay: jest.fn().mockReturnValue(false),
          } satisfies Pick<UtilService, "setTitle" | "openSnackbar" | "hasEditOverlay">,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TestEditInputComponent);
    component = fixture.componentInstance;
    component.formField = new InputFormFieldBuilder("test waarde")
      .id("test")
      .label("test.label")
      .build();
    fixture.detectChanges();
  });

  it("should render the field label and value in static view", () => {
    expect(fixture.nativeElement.textContent).toContain("test.label");
    expect(fixture.nativeElement.textContent).toContain("test waarde");
  });

  it("should start editing when clicked and not readonly", () => {
    expect(component.editing).toBe(false);
    fixture.nativeElement.querySelector(".static-text").click();
    expect(component.editing).toBe(true);
  });

  it("should not start editing when readonly", () => {
    component.readonly = true;
    fixture.nativeElement.querySelector(".static-text").click();
    expect(component.editing).toBe(false);
  });

  it("should exit editing and emit saveField on save", () => {
    component.formField.formControl.setValue("nieuwe waarde");
    component.edit();

    const emitted: Record<string, unknown>[] = [];
    component.saveField.subscribe((v) => emitted.push(v));

    component.formField.formControl.markAsDirty();
    component.save();

    expect(component.editing).toBe(false);
    expect(emitted[0]).toMatchObject({ test: "nieuwe waarde" });
  });

  it("should exit editing without emitting on cancel", () => {
    component.edit();
    const emitted: unknown[] = [];
    component.saveField.subscribe((v) => emitted.push(v));

    component.cancel();

    expect(component.editing).toBe(false);
    expect(emitted).toHaveLength(0);
  });
});
