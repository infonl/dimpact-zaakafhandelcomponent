/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ZacNarrowMatCheckboxDirective } from "./narrow-checkbox.directive";

@Component({
  template: "<div zacNarrowMatCheckbox></div>",
  imports: [ZacNarrowMatCheckboxDirective],
})
class TestHostComponent {}

describe(ZacNarrowMatCheckboxDirective.name, () => {
  let fixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it("should add mat-narrow-checkbox class to the host element", () => {
    expect(fixture.nativeElement.querySelector("div").classList).toContain(
      "mat-narrow-checkbox",
    );
  });
});
