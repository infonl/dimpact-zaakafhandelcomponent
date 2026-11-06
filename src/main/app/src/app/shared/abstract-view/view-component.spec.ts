/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import { Component, signal } from "@angular/core";
import {
  ComponentFixture,
  fakeAsync,
  TestBed,
  tick,
} from "@angular/core/testing";
import { MatDrawerMode, MatSidenavContainer } from "@angular/material/sidenav";
import { Subscription } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ViewComponent } from "./view-component";

@Component({
  template: "",
  standalone: true,
})
class TestViewComponent extends ViewComponent {
  readonly updateContentMarginsMock = jest.fn();

  readonly sideNavContainer = signal(
    fromPartial<MatSidenavContainer>({
      updateContentMargins: this.updateContentMarginsMock,
    }),
  );

  public constructor() {
    super();
  }

  get mode(): MatDrawerMode {
    return this.sideNavMode();
  }

  triggerModeChange(mode: string): void {
    this.menuModeChanged(mode);
  }

  addSubscription(sub: Subscription): void {
    this.subscriptions$.push(sub);
  }
}

describe(ViewComponent.name, () => {
  let fixture: ComponentFixture<TestViewComponent>;
  let component: TestViewComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestViewComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(TestViewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it("should update sideNavMode when mode changes", () => {
    component.triggerModeChange("over");
    expect(component.mode).toBe("over");
  });

  it("should call updateContentMargins after mode change", fakeAsync(() => {
    component.triggerModeChange("over");
    tick(300);
    expect(component.updateContentMarginsMock).toHaveBeenCalled();
  }));

  it("should unsubscribe all subscriptions on destroy", () => {
    const sub = new Subscription();
    jest.spyOn(sub, "unsubscribe");
    component.addSubscription(sub);
    component.ngOnDestroy();
    expect(sub.unsubscribe).toHaveBeenCalled();
  });
});
