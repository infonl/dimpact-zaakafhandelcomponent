/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, ChangeDetectionStrategy } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { screen } from "@testing-library/angular";
import { ZacNarrowMatCheckboxDirective } from "./narrow-checkbox.directive";

@Component({
  template: "<div zacNarrowMatCheckbox>checkbox</div>",
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [ZacNarrowMatCheckboxDirective],
})
class TestHostComponent {}

describe(ZacNarrowMatCheckboxDirective.name, () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();

    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
  });

  it("should add mat-narrow-checkbox class to the host element", () => {
    expect(screen.getByText("checkbox")).toHaveClass("mat-narrow-checkbox");
  });
});
