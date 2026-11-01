/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { EnhanceMatErrorDirective } from "./mat-zac-error";

@Component({
  standalone: true,
  template: `<span appEnhanceMatError>Some error message</span>`,
  imports: [EnhanceMatErrorDirective],
})
class TestHostComponent {}

describe(EnhanceMatErrorDirective.name, () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
    }).compileComponents();
  });

  it("sets title to innerHTML of the host element after view init", async () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const span = fixture.debugElement.query(
      By.directive(EnhanceMatErrorDirective),
    ).nativeElement as HTMLElement;

    expect(span.title).toBe("Some error message");
  });

  it("sets ellipsis CSS on the host element after view init", async () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();
    await fixture.whenStable();

    const span = fixture.debugElement.query(
      By.directive(EnhanceMatErrorDirective),
    ).nativeElement as HTMLElement;

    expect(span.style.textOverflow).toBe("ellipsis");
    expect(span.style.overflow).toBe("hidden");
    expect(span.style.whiteSpace).toBe("nowrap");
  });
});
