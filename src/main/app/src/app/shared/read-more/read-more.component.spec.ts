/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { ComponentFixture, TestBed } from "@angular/core/testing";
import { MatTooltip } from "@angular/material/tooltip";
import { By } from "@angular/platform-browser";
import { NoopAnimationsModule } from "@angular/platform-browser/animations";
import { ReadMoreComponent } from "./read-more.component";

describe(ReadMoreComponent.name, () => {
  let fixture: ComponentFixture<ReadMoreComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReadMoreComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(ReadMoreComponent);
  });

  it("should render full text when within maxLength", () => {
    fixture.componentRef.setInput("text", "Short text");
    fixture.componentRef.setInput("maxLength", 100);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("Short text");
    expect(fixture.debugElement.query(By.directive(MatTooltip))).toBeNull();
  });

  it("should truncate and show tooltip when text exceeds maxLength", () => {
    fixture.componentRef.setInput("text", "This is a longer piece of text");
    fixture.componentRef.setInput("maxLength", 10);
    fixture.detectChanges();

    const tooltipEl = fixture.debugElement.query(By.directive(MatTooltip));
    expect(tooltipEl).not.toBeNull();
    expect(tooltipEl.injector.get(MatTooltip).message).toBe(
      "This is a longer piece of text",
    );
    expect(fixture.nativeElement.textContent).toContain("...");
  });

  it("should not show tooltip when text is undefined", () => {
    fixture.componentRef.setInput("text", undefined);
    fixture.detectChanges();

    expect(fixture.debugElement.query(By.directive(MatTooltip))).toBeNull();
  });
});
