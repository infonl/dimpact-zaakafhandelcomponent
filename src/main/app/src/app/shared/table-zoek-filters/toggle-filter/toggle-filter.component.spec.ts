/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ToggleFilterComponent } from "./toggle-filter.component";
import { ToggleSwitchOptions } from "./toggle-switch-options";

describe(ToggleFilterComponent.name, () => {
  let fixture: ComponentFixture<ToggleFilterComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToggleFilterComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ToggleFilterComponent);
    fixture.detectChanges();
  });

  it("should render indeterminate icon by default", () => {
    expect(
      fixture.nativeElement.querySelector("mat-icon")?.textContent?.trim(),
    ).toBe("radio_button_unchecked");
  });

  it("should cycle INDETERMINATE → CHECKED on click and emit", () => {
    const emitted: ToggleSwitchOptions[] = [];
    fixture.componentInstance.changed.subscribe((v: ToggleSwitchOptions) =>
      emitted.push(v),
    );
    fixture.nativeElement.querySelector("button").click();

    expect(emitted).toEqual([ToggleSwitchOptions.CHECKED]);
  });

  it("should cycle CHECKED → UNCHECKED on click and emit", () => {
    fixture.componentRef.setInput("selected", ToggleSwitchOptions.CHECKED);
    fixture.detectChanges();

    const emitted: ToggleSwitchOptions[] = [];
    fixture.componentInstance.changed.subscribe((v: ToggleSwitchOptions) =>
      emitted.push(v),
    );
    fixture.nativeElement.querySelector("button").click();

    expect(emitted).toEqual([ToggleSwitchOptions.UNCHECKED]);
  });

  it("should cycle UNCHECKED → INDETERMINATE on click and emit", () => {
    fixture.componentRef.setInput("selected", ToggleSwitchOptions.UNCHECKED);
    fixture.detectChanges();

    const emitted: ToggleSwitchOptions[] = [];
    fixture.componentInstance.changed.subscribe((v: ToggleSwitchOptions) =>
      emitted.push(v),
    );
    fixture.nativeElement.querySelector("button").click();

    expect(emitted).toEqual([ToggleSwitchOptions.INDETERMINATE]);
  });
});
