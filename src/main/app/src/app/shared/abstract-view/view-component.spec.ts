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
import {
  MatDrawerMode,
  MatSidenav,
  MatSidenavContainer,
} from "@angular/material/sidenav";
import { Subject, Subscription } from "rxjs";
import { fromPartial } from "src/test-helpers";
import { ViewComponent } from "./view-component";

@Component({ template: "", standalone: true })
class TestViewComponent extends ViewComponent {
  private readonly openedStart = new Subject<void>();
  readonly updateContentMarginsMock = jest.fn();

  readonly sideNavContainer = signal(
    fromPartial<MatSidenavContainer>({
      hasBackdrop: true,
      updateContentMargins: this.updateContentMarginsMock,
    }),
  );

  readonly menuSidenav = signal(
    fromPartial<MatSidenav>({
      openedStart: this.openedStart.asObservable(),
    }),
  );

  public constructor() {
    super();
  }

  get mode(): MatDrawerMode {
    return this.sideNaveMode;
  }

  get containerHasBackdrop(): boolean {
    return this.sideNavContainer().hasBackdrop;
  }

  triggerModeChange(mode: string): void {
    this.menuModeChanged(mode);
  }

  triggerMenuOpen(): void {
    this.openedStart.next();
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

  it("should update sideNaveMode when mode changes", () => {
    component.triggerModeChange("over");
    expect(component.mode).toBe("over");
  });

  it("should set hasBackdrop to false when menu sidenav opens", () => {
    component.triggerMenuOpen();
    expect(component.containerHasBackdrop).toBe(false);
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
