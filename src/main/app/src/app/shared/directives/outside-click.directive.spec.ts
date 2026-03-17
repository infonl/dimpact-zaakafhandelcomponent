/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
} from "@angular/core/testing";
import { Component } from "@angular/core";
import { UtilService } from "../../core/service/util.service";
import { OutsideClickDirective } from "./outside-click.directive";

@Component({
  template: `<div zacOutsideClick (zacOutsideClick)="onOutsideClick($event)">
    <span id="inside">inside</span>
  </div>`,
  imports: [OutsideClickDirective],
})
class TestHostComponent {
  onOutsideClick = jest.fn();
}

describe(OutsideClickDirective.name, () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let component: TestHostComponent;
  let utilServiceMock: Pick<UtilService, "hasEditOverlay">;

  beforeEach(async () => {
    utilServiceMock = { hasEditOverlay: jest.fn().mockReturnValue(false) };

    await TestBed.configureTestingModule({
      imports: [TestHostComponent],
      providers: [{ provide: UtilService, useValue: utilServiceMock }],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    component = fixture.componentInstance;
  });

  it("should emit outsideClick when clicking outside the host element", fakeAsync(() => {
    fixture.detectChanges();
    tick(0);

    document.body.dispatchEvent(new MouseEvent("click", { bubbles: true }));

    expect(component.onOutsideClick).toHaveBeenCalled();
  }));

  it("should not emit when clicking inside the host element", fakeAsync(() => {
    fixture.detectChanges();
    tick(0);

    fixture.nativeElement.querySelector("#inside").click();

    expect(component.onOutsideClick).not.toHaveBeenCalled();
  }));

  it("should not emit when hasEditOverlay returns true", fakeAsync(() => {
    (utilServiceMock.hasEditOverlay as jest.Mock).mockReturnValue(true);
    fixture.detectChanges();
    tick(0);

    document.body.dispatchEvent(new MouseEvent("click", { bubbles: true }));

    expect(component.onOutsideClick).not.toHaveBeenCalled();
  }));
});
