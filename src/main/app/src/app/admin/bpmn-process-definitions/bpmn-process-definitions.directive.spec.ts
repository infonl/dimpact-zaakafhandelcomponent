/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component } from "@angular/core";
import { TestBed } from "@angular/core/testing";
import { By } from "@angular/platform-browser";
import { BpmnNodeRowDirective } from "./bpmn-process-definitions.directive";

@Component({
  standalone: true,
  imports: [BpmnNodeRowDirective],
  template: `<div [bpmnNodeRow]="key"></div>`,
})
class TestHostComponent {
  key = "test-key";
}

describe(BpmnNodeRowDirective.name, () => {
  it("should expose the bound key via the key signal", () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const directive = fixture.debugElement
      .query(By.directive(BpmnNodeRowDirective))
      .injector.get(BpmnNodeRowDirective);

    expect(directive.key()).toBe("test-key");
  });

  it("should expose the host element via el", () => {
    const fixture = TestBed.createComponent(TestHostComponent);
    fixture.detectChanges();

    const directive = fixture.debugElement
      .query(By.directive(BpmnNodeRowDirective))
      .injector.get(BpmnNodeRowDirective);

    expect(directive.el.nativeElement).toBeInstanceOf(HTMLDivElement);
  });
});
